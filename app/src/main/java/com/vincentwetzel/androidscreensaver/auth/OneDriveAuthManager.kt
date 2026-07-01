package com.vincentwetzel.androidscreensaver.auth

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.IPublicClientApplication
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.exception.MsalException
import com.vincentwetzel.androidscreensaver.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    ) = withContext(Dispatchers.IO) {
        if (isAndroidTv()) {
            if (onDeviceCode == null) throw IllegalArgumentException("onDeviceCode callback required for Android TV")
            startDeviceAuthorizationGrant(onDeviceCode)
        } else {
            startMsalInteractiveAuth(activity)
        }
    }

    private suspend fun startDeviceAuthorizationGrant(onDeviceCode: (String, String) -> Unit) {
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

        pollForDeviceToken(clientId, deviceCode, interval)
    }

    private suspend fun pollForDeviceToken(clientId: String, deviceCode: String, intervalSeconds: Int) {
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
                
                // Fetch the Account ID using the Graph API to act as the primary routing key
                val accountId = fetchAccountId(accessToken)
                saveTokens(accountId, accessToken, refreshToken)
                break
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

    private suspend fun fetchAccountId(accessToken: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://graph.microsoft.com/v1.0/me")
            .header("Authorization", "Bearer $accessToken")
            .build()
            
        httpClient.newCall(request).await().use { res ->
            if (!res.isSuccessful) throw IllegalStateException("Failed to fetch user ID")
            val bodyString = res.body?.string() ?: throw IllegalStateException("Empty response body from Microsoft Graph API")
            val json = JSONObject(bodyString)
            when {
                json.has("mail") && !json.isNull("mail") -> json.getString("mail")
                json.has("userPrincipalName") && !json.isNull("userPrincipalName") -> json.getString("userPrincipalName")
                else -> json.getString("id")
            }
        }
    }

    private suspend fun startMsalInteractiveAuth(activity: Activity) {
        val configFileResourceId = context.resources.getIdentifier("msal_auth_config", "raw", context.packageName)
        if (configFileResourceId == 0) {
            throw IllegalStateException("MSAL config file not found. Ensure msal_auth_config.json is in res/raw/")
        }

        val pca = suspendCancellableCoroutine<ISingleAccountPublicClientApplication> { cont ->
            PublicClientApplication.createSingleAccountPublicClientApplication(
                context,
                configFileResourceId,
                object : IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
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
                pca.signIn(activity, null, SCOPES.toTypedArray(), object : AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        if (cont.isActive) cont.resume(authenticationResult)
                    }
                    override fun onError(exception: MsalException) {
                        if (cont.isActive) cont.resumeWithException(exception)
                    }
                    override fun onCancel() {
                        if (cont.isActive) cont.resumeWithException(java.util.concurrent.CancellationException("User cancelled MSAL sign-in"))
                    }
                })
            }
        }

        val accountId = authResult.account.username ?: authResult.account.id
        val accessToken = authResult.accessToken
        // MSAL handles refreshing implicitly, but caching the access token aligns with our explicit repository behavior
        saveTokens(accountId, accessToken, null)
    }

    suspend fun saveTokens(accountId: String, accessToken: String, refreshToken: String?): Boolean = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(accountId, StandardCharsets.UTF_8.name())
        // Tokens are never logged per the security guidelines
        try {
            securePrefs.edit().apply {
                putString(KEY_ACCESS_TOKEN + encodedId, accessToken)
                refreshToken?.let { putString(KEY_REFRESH_TOKEN + encodedId, it) }
            }.commit()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAccessToken(accountId: String): String? = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(accountId, StandardCharsets.UTF_8.name())
        try {
            securePrefs.getString(KEY_ACCESS_TOKEN + encodedId, null)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun clearTokens(accountId: String): Boolean = withContext(Dispatchers.IO) {
        val encodedId = URLEncoder.encode(accountId, StandardCharsets.UTF_8.name())
        try {
            securePrefs.edit().apply {
                remove(KEY_ACCESS_TOKEN + encodedId)
                remove(KEY_REFRESH_TOKEN + encodedId)
            }.commit()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAllAccountIds(): List<String> = withContext(Dispatchers.IO) {
        try {
            securePrefs.all.keys
                .filter { it.startsWith(KEY_ACCESS_TOKEN) }
                .map { it.removePrefix(KEY_ACCESS_TOKEN) }
                .map { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) }
        } catch (e: Exception) {
            emptyList()
        }
    }
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