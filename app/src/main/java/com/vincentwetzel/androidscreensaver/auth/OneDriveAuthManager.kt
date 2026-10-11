package com.vincentwetzel.androidscreensaver.auth

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IMultipleAccountPublicClientApplication
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import com.vincentwetzel.androidscreensaver.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OneDriveAuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val httpClient: OkHttpClient
) {

    companion object {
        private const val PREFS_NAME = "onedrive_auth_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token_"
        private const val KEY_REFRESH_TOKEN = "refresh_token_"
        private const val KEY_TOKEN_EXPIRY = "token_expiry_"
        private const val KEY_MSAL_ACCOUNT = "msal_account_"
        // Principle of Least Privilege: only request read-only scopes.
        private val SCOPES = listOf("Files.Read", "User.Read", "offline_access")
    }

    // Lazy initialize to prevent blocking DI initialization on the main thread with Keystore I/O
    private val securePrefs by lazy {
        try {
            createEncryptedPrefs()
        } catch (e: Exception) {
            // Under extreme keystore corruption scenarios, clear existing prefs to recover
            try {
                context.deleteSharedPreferences(PREFS_NAME)
            } catch (_: Exception) {}
            try {
                createEncryptedPrefs()
            } catch (retryException: Exception) {
                throw IllegalStateException("Failed to initialize EncryptedSharedPreferences securely", retryException)
            }
        }
    }

    // Repository authentication checks are synchronous, so keep the account IDs
    // in memory after loading them from encrypted preferences off the caller thread.
    @Volatile
    private var cachedAccountIds: Set<String> = emptySet()

    private val accountCacheLock = Any()
    private val tokenRefreshLocks = ConcurrentHashMap<String, Mutex>()

    private val authScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        authScope.launch {
            getAllAccountIds()
        }
    }

    private fun createEncryptedPrefs() = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun isAndroidTv(): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        return uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
    }

    suspend fun authenticate(
        activity: Activity,
        onDeviceCode: ((userCode: String, verificationUri: String) -> Unit)? = null
    ): OneDriveAuthenticatedAccount = withContext(Dispatchers.IO) {
        if (isAndroidTv()) {
            if (onDeviceCode == null) throw IllegalArgumentException("onDeviceCode callback required for Android TV")
            startDeviceAuthorizationGrant(onDeviceCode)
        } else {
            startMsalInteractiveAuth(activity)
        }
    }

    private suspend fun startDeviceAuthorizationGrant(onDeviceCode: (String, String) -> Unit): OneDriveAuthenticatedAccount {
        val clientId = BuildConfig.ONEDRIVE_CLIENT_ID
        val reqBody = FormBody.Builder()
            .add("client_id", clientId)
            .add("scope", SCOPES.joinToString(" "))
            .build()

        val request = Request.Builder()
            .url("https://login.microsoftonline.com/common/oauth2/v2.0/devicecode")
            .post(reqBody)
            .build()

        val responseStr = httpClient.newCall(request).await().use { response ->
            if (!response.isSuccessful) throw IllegalStateException("Device code endpoint failed: ${response.code}")
            response.body?.string()
        } ?: throw IllegalStateException("Empty response from device code endpoint")

        val json = JSONObject(responseStr)
        val deviceCode = json.getString("device_code")
        val userCode = json.getString("user_code")
        val verificationUri = json.getString("verification_uri")
        val interval = json.optInt("interval", 5)

        withContext(Dispatchers.Main) {
            onDeviceCode(userCode, verificationUri)
        }

        return pollForDeviceToken(clientId, deviceCode, interval)
    }

    private suspend fun pollForDeviceToken(
        clientId: String,
        deviceCode: String,
        intervalSeconds: Int
    ): OneDriveAuthenticatedAccount {
        var currentIntervalSeconds = intervalSeconds
        
        val tokenBody = FormBody.Builder()
            .add("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
            .add("client_id", clientId)
            .add("device_code", deviceCode)
            .build()

        val request = Request.Builder()
            .url("https://login.microsoftonline.com/common/oauth2/v2.0/token")
            .post(tokenBody)
            .build()

        while (true) {
            delay(currentIntervalSeconds * 1000L)

            val (isSuccessful, bodyStr) = try {
                httpClient.newCall(request).await().use { response ->
                    response.isSuccessful to response.body?.string()
                }
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // Fail gracefully on transient network errors and let the next poll cycle try again
                false to null
            }
            
            if (bodyStr == null) continue
            val json = try {
                JSONObject(bodyStr)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                continue
            }

            if (isSuccessful) {
                val accessToken = json.getString("access_token")
                val refreshToken = if (json.has("refresh_token") && !json.isNull("refresh_token")) {
                    json.getString("refresh_token")
                } else {
                    null
                }
                val expiresAt = System.currentTimeMillis() + json.optLong("expires_in", 3600L) * 1000L
                
                val account = fetchAccountProfile(accessToken)
                if (!saveTokens(account.accountId, accessToken, refreshToken, expiresAtMillis = expiresAt)) {
                    throw IllegalStateException("Failed to save OneDrive credentials")
                }
                return account
            } else {
                val error = if (json.has("error") && !json.isNull("error")) {
                    json.getString("error")
                } else {
                    "unknown_error"
                }
                if (error == "authorization_pending") {
                    continue
                } else if (error == "slow_down") {
                    currentIntervalSeconds += 5 // Respect the server's backoff request
                    continue
                } else if (error == "authorization_declined" || error == "expired_token") {
                    throw IllegalStateException("Device authorization failed: $error")
                } else {
                    throw IllegalStateException("Token poll error: $error")
                }
            }
        }
    }

    private suspend fun fetchAccountProfile(
        accessToken: String,
        fallbackEmail: String? = null
    ): OneDriveAuthenticatedAccount = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://graph.microsoft.com/v1.0/me")
            .header("Authorization", "Bearer $accessToken")
            .build()
            
        httpClient.newCall(request).await().use { res ->
            if (!res.isSuccessful) throw IllegalStateException("Failed to fetch user ID")
            val bodyString = res.body?.string() ?: throw IllegalStateException("Empty response body from Microsoft Graph API")
            val json = JSONObject(bodyString)
            OneDriveAccountProfilePolicy.fromFields(
                graphId = json.optString("id").takeIf { it.isNotBlank() },
                mail = json.optString("mail").takeIf { it.isNotBlank() },
                userPrincipalName = json.optString("userPrincipalName").takeIf { it.isNotBlank() },
                fallbackEmail = fallbackEmail
            ) ?: throw IllegalStateException("Microsoft Graph did not return an account email")
        }
    }

    private suspend fun startMsalInteractiveAuth(activity: Activity): OneDriveAuthenticatedAccount {
        val configFileResourceId = context.resources.getIdentifier("msal_auth_config", "raw", context.packageName)
        if (configFileResourceId == 0) {
            throw IllegalStateException("MSAL config file not found. Ensure msal_auth_config.json is in res/raw/")
        }

        val pca = suspendCancellableCoroutine<IMultipleAccountPublicClientApplication> { cont ->
            PublicClientApplication.createMultipleAccountPublicClientApplication(
                context,
                configFileResourceId,
                object : IPublicClientApplication.IMultipleAccountApplicationCreatedListener {
                    override fun onCreated(application: IMultipleAccountPublicClientApplication) {
                        if (cont.isActive) {
                            cont.resume(application)
                        }
                    }
                    override fun onError(exception: MsalException) {
                        if (cont.isActive) {
                            cont.resumeWithException(exception)
                        }
                    }
                }
            )
        }

        // MSAL interactive sign-in must be executed on the Main thread as it launches UI components
        val authResult = withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<IAuthenticationResult> { cont ->
                val parameters = AcquireTokenParameters.Builder()
                    .startAuthorizationFromActivity(activity)
                    .withScopes(SCOPES)
                    .withCallback(object : AuthenticationCallback {
                        override fun onSuccess(authenticationResult: IAuthenticationResult) {
                            if (cont.isActive) cont.resume(authenticationResult)
                        }
                        override fun onError(exception: MsalException) {
                            if (cont.isActive) cont.resumeWithException(exception)
                        }
                        override fun onCancel() {
                            if (cont.isActive) cont.resumeWithException(
                                java.util.concurrent.CancellationException("User cancelled MSAL sign-in")
                            )
                        }
                    })
                    .build()
                pca.acquireToken(parameters)
            }
        }

        val accessToken = authResult.accessToken
        val account = fetchAccountProfile(accessToken, fallbackEmail = authResult.account.username)
        if (!saveTokens(
                account.accountId,
                accessToken,
                refreshToken = null,
                expiresAtMillis = authResult.expiresOn.time,
                msalAccountId = authResult.account.id
            )
        ) {
            throw IllegalStateException("Failed to save OneDrive credentials")
        }
        return account
    }

    suspend fun saveTokens(
        accountId: String,
        accessToken: String,
        refreshToken: String?,
        expiresAtMillis: Long? = null,
        msalAccountId: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(accountId, StandardCharsets.UTF_8.name())
        // Tokens are never logged per the security guidelines
        try {
            val saved = securePrefs.edit().apply {
                putString(KEY_ACCESS_TOKEN + encodedId, accessToken)
                refreshToken?.let { putString(KEY_REFRESH_TOKEN + encodedId, it) }
                expiresAtMillis?.let { putLong(KEY_TOKEN_EXPIRY + encodedId, it) }
                msalAccountId?.let { putString(KEY_MSAL_ACCOUNT + encodedId, it) }
            }.commit()
            if (saved) {
                synchronized(accountCacheLock) {
                    cachedAccountIds = cachedAccountIds + accountId
                }
            }
            saved
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAccessToken(accountId: String): String? = withContext(Dispatchers.IO) {
        tokenRefreshLocks.computeIfAbsent(accountId) { Mutex() }.withLock {
            val record = try {
                val encodedId = URLEncoder.encode(accountId, StandardCharsets.UTF_8.name())
                TokenRecord(
                    accessToken = securePrefs.getString(KEY_ACCESS_TOKEN + encodedId, null),
                    refreshToken = securePrefs.getString(KEY_REFRESH_TOKEN + encodedId, null),
                    expiresAtMillis = securePrefs.getLong(KEY_TOKEN_EXPIRY + encodedId, 0L),
                    msalAccountId = securePrefs.getString(KEY_MSAL_ACCOUNT + encodedId, null)
                )
            } catch (e: Exception) {
                return@withLock null
            }

            val currentToken = record.accessToken ?: return@withLock null
            val nowMillis = System.currentTimeMillis()
            if (!OneDriveTokenPolicy.shouldRefresh(record.expiresAtMillis, nowMillis)) {
                return@withLock currentToken
            }

            val refreshedToken = when {
                record.refreshToken != null -> refreshDeviceFlowToken(accountId, record.refreshToken)
                record.msalAccountId != null -> refreshMsalToken(accountId, record.msalAccountId)
                else -> null
            }
            refreshedToken ?: currentToken.takeIf {
                OneDriveTokenPolicy.canUseAfterRefreshFailure(record.expiresAtMillis, System.currentTimeMillis())
            }
        }
    }

    private data class TokenRecord(
        val accessToken: String?,
        val refreshToken: String?,
        val expiresAtMillis: Long,
        val msalAccountId: String?
    )

    private suspend fun refreshDeviceFlowToken(accountId: String, refreshToken: String): String? {
        val request = Request.Builder()
            .url("https://login.microsoftonline.com/common/oauth2/v2.0/token")
            .post(
                FormBody.Builder()
                    .add("client_id", BuildConfig.ONEDRIVE_CLIENT_ID)
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refreshToken)
                    .add("scope", SCOPES.joinToString(" "))
                    .build()
            )
            .build()

        return try {
            httpClient.newCall(request).await().use { response ->
                if (!response.isSuccessful) return null
                val json = JSONObject(response.body?.string() ?: return null)
                val accessToken = json.optString("access_token").takeIf { it.isNotBlank() } ?: return null
                val rotatedRefreshToken = json.optString("refresh_token")
                    .takeIf { it.isNotBlank() } ?: refreshToken
                val expiresAt = System.currentTimeMillis() + json.optLong("expires_in", 3600L) * 1000L
                if (saveTokens(accountId, accessToken, rotatedRefreshToken, expiresAtMillis = expiresAt)) {
                    accessToken
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    private suspend fun refreshMsalToken(accountId: String, msalAccountId: String): String? {
        return try {
            val configFileResourceId = context.resources.getIdentifier("msal_auth_config", "raw", context.packageName)
            if (configFileResourceId == 0) return null
            val pca = suspendCancellableCoroutine<IMultipleAccountPublicClientApplication> { cont ->
                PublicClientApplication.createMultipleAccountPublicClientApplication(
                    context,
                    configFileResourceId,
                    object : IPublicClientApplication.IMultipleAccountApplicationCreatedListener {
                        override fun onCreated(application: IMultipleAccountPublicClientApplication) {
                            if (cont.isActive) cont.resume(application)
                        }

                        override fun onError(exception: MsalException) {
                            if (cont.isActive) cont.resumeWithException(exception)
                        }
                    }
                )
            }
            val account: IAccount = pca.getAccount(msalAccountId) ?: return null
            val authority = pca.configuration.defaultAuthority.authorityURL.toString()
            val result = pca.acquireTokenSilent(SCOPES.toTypedArray(), account, authority)
            val accessToken = result.accessToken
            if (saveTokens(
                    accountId,
                    accessToken,
                    refreshToken = null,
                    expiresAtMillis = result.expiresOn.time,
                    msalAccountId = result.account.id
                )
            ) {
                accessToken
            } else {
                null
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    suspend fun clearTokens(accountId: String): Boolean = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(accountId, StandardCharsets.UTF_8.name())
        try {
            val cleared = securePrefs.edit().apply {
                remove(KEY_ACCESS_TOKEN + encodedId)
                remove(KEY_REFRESH_TOKEN + encodedId)
                remove(KEY_TOKEN_EXPIRY + encodedId)
                remove(KEY_MSAL_ACCOUNT + encodedId)
            }.commit()
            if (cleared) {
                synchronized(accountCacheLock) {
                    cachedAccountIds = cachedAccountIds - accountId
                }
            }
            cleared
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAllAccountIds(): List<String> = withContext(Dispatchers.IO) {
        try {
            val accountIds = securePrefs.all.keys
                .filter { it.startsWith(KEY_ACCESS_TOKEN) }
                .map { it.removePrefix(KEY_ACCESS_TOKEN) }
                .map { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
            synchronized(accountCacheLock) {
                cachedAccountIds = accountIds.toSet()
            }
            accountIds
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Returns the last account-ID snapshot loaded from encrypted preferences.
     * This is intentionally non-suspending for repository authentication checks.
     */
    fun getCachedAccountIds(): List<String> = cachedAccountIds.toList()
}

/**
 * Suspends the coroutine cooperatively until the OkHttp Call completes,
 * supporting structured cancellation by cancelling the underlying socket request immediately.
 */
private suspend fun Call.await(): Response = suspendCancellableCoroutine { cont ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            if (cont.isActive) {
                cont.resume(response)
            } else {
                response.close() // Prevent resource and socket leaks if the calling coroutine has been cancelled
            }
        }
        override fun onFailure(call: Call, e: IOException) {
            if (cont.isActive) {
                cont.resumeWithException(e)
            }
        }
    })
    cont.invokeOnCancellation {
        cancel()
    }
}
