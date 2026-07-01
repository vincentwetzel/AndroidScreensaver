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
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
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

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
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

        val responseStr = httpClient.newCall(request).execute().use { it.body?.string() }
            ?: throw IllegalStateException("Empty response from device code endpoint")

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

            val (isSuccessful, bodyStr) = httpClient.newCall(request).execute().use { response ->
                response.isSuccessful to response.body?.string()
            }
            
            if (bodyStr == null) continue
            val json = JSONObject(bodyStr)

            if (isSuccessful) {
                val accessToken = json.getString("access_token")
                val refreshToken = json.optString("refresh_token", null)
                
                // Fetch the Account ID using the Graph API to act as the primary routing key
                val accountId = fetchAccountId(accessToken)
                saveTokens(accountId, accessToken, refreshToken)
                break
            } else {
                val error = json.optString("error")
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

    private suspend fun fetchAccountId(accessToken: String): String {
        val request = Request.Builder()
            .url("https://graph.microsoft.com/v1.0/me")
            .header("Authorization", "Bearer $accessToken")
            .build()
            
        return httpClient.newCall(request).execute().use { res ->
            if (!res.isSuccessful) throw IllegalStateException("Failed to fetch user ID")
            val json = JSONObject(res.body?.string() ?: "")
            json.getString("id")
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
                        cont.resume(application)
                    }
                    override fun onError(exception: MsalException) {
                        cont.resumeWithException(exception)
                    }
                }
            )
        }

        // MSAL interactive sign-in must be executed on the Main thread as it launches UI components
        val authResult = withContext(Dispatchers.Main) {
            suspendCancellableCoroutine<IAuthenticationResult> { cont ->
                pca.signIn(activity, null, SCOPES.toTypedArray(), object : AuthenticationCallback {
                    override fun onSuccess(authenticationResult: IAuthenticationResult) {
                        cont.resume(authenticationResult)
                    }
                    override fun onError(exception: MsalException) {
                        cont.resumeWithException(exception)
                    }
                    override fun onCancel() {
                        cont.resumeWithException(java.util.concurrent.CancellationException("User cancelled MSAL sign-in"))
                    }
                })
            }
        }

        val accountId = authResult.account.id
        val accessToken = authResult.accessToken
        // MSAL handles refreshing implicitly, but caching the access token aligns with our explicit repository behavior
        saveTokens(accountId, accessToken, null)
    }

    fun saveTokens(accountId: String, accessToken: String, refreshToken: String?) {
        val encodedId = URLEncoder.encode(accountId, "UTF-8")
        // Tokens are never logged per the security guidelines
        securePrefs.edit().apply {
            putString(KEY_ACCESS_TOKEN + encodedId, accessToken)
            refreshToken?.let { putString(KEY_REFRESH_TOKEN + encodedId, it) }
        }.apply()
    }

    fun getAccessToken(accountId: String): String? {
        val encodedId = URLEncoder.encode(accountId, "UTF-8")
        return securePrefs.getString(KEY_ACCESS_TOKEN + encodedId, null)
    }

    fun clearTokens(accountId: String) {
        val encodedId = URLEncoder.encode(accountId, "UTF-8")
        securePrefs.edit().apply {
            remove(KEY_ACCESS_TOKEN + encodedId)
            remove(KEY_REFRESH_TOKEN + encodedId)
        }.apply()
    }

    fun getAllAccountIds(): List<String> {
        return securePrefs.all.keys
            .filter { it.startsWith(KEY_ACCESS_TOKEN) }
            .map { it.removePrefix(KEY_ACCESS_TOKEN) }
            .map { URLDecoder.decode(it, "UTF-8") }
    }
}