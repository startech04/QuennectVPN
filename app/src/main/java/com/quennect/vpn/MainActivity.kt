package com.quennect.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private val logs = mutableStateListOf<String>()
    private var userName = mutableStateOf<String?>(null)
    private val signInLoading = mutableStateOf(false)
    private val signInError = mutableStateOf<String?>(null)
    private val updateInfo = mutableStateOf<UpdateInfo?>(null)
    private val updateChecking = mutableStateOf(true)
    private val updateProgress = mutableStateOf(-1)
    private val updateError = mutableStateOf<String?>(null)

    private val logReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.getStringExtra(QuennectVpnService.LOG_EXTRA)?.let {
                logs.add(it)
                if (logs.size > 100) logs.removeAt(0)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)

        val currentVersion = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            "1.0.0.2"
        }
        logs.add("[SYSTEM] Quennect v$currentVersion initialized")

        auth = Firebase.auth
        userName.value = auth.currentUser?.displayName

        val filter = IntentFilter(QuennectVpnService.LOG_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(logReceiver, filter)
        }

        checkForUpdates()

        setContent {
            val info = updateInfo.value
            val checking = updateChecking.value
            val progress = updateProgress.value
            val error = updateError.value
            
            val currentVersionName = try {
                packageManager.getPackageInfo(packageName, 0).versionName
            } catch (e: Exception) {
                "1.0.0.2"
            }

            if (!checking && info != null && info.updateAvailable) {
                UpdateRequiredScreen(
                    currentVersionName = currentVersionName,
                    remoteVersionName = info.remoteVersionName,
                    progress = progress,
                    error = error,
                    onUpdateRequested = { downloadUpdate() }
                )
            } else {
                QuennectApp(
                    logs = logs,
                    versionName = currentVersionName,
                    userName = userName.value,
                    signInLoading = signInLoading.value,
                    signInError = signInError.value,
                    onConnectRequested = { connectVpn() },
                    onDisconnectRequested = { disconnectVpn() },
                    onSignInRequested = { handleSignIn() }
                )
            }
        }
    }

    private fun checkForUpdates() {
        CoroutineScope(Dispatchers.IO).launch {
            val info = UpdateManager.checkForUpdate(this@MainActivity)
            updateInfo.value = info
            updateChecking.value = false
        }
    }

    private fun downloadUpdate() {
        updateProgress.value = 0
        updateError.value = null
        UpdateManager.downloadAndInstall(
            context = this,
            onProgress = { runOnUiThread { updateProgress.value = it } },
            onComplete = { runOnUiThread { updateProgress.value = 100 } },
            onError = { runOnUiThread { updateError.value = it } }
        )
    }

    private fun handleSignIn() {
        val credentialManager = CredentialManager.create(this)

        val serverClientId = "917081148123-kdk1hbn6ii0lpei8n5bmkokla9g1j3q1.apps.googleusercontent.com"

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        signInLoading.value = true
        signInError.value = null

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@MainActivity,
                    request = request
                )
                val credential = result.credential
                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    if (idToken != null) {
                        firebaseAuthWithGoogle(idToken)
                    } else {
                        signInLoading.value = false
                        signInError.value = "Failed to retrieve ID token"
                        logs.add("[AUTH] Error: ID token was null")
                    }
                } else {
                    signInLoading.value = false
                    signInError.value = "Unexpected credential type"
                    logs.add("[AUTH] Error: unexpected credential type received")
                }
            } catch (e: GetCredentialException) {
                signInLoading.value = false
                val message = e.message ?: "Sign-in failed"
                signInError.value = message
                Log.e("QuennectAuth", "Sign-in failed", e)
                logs.add("[AUTH] Sign-in failed: $message")
            } catch (e: Exception) {
                signInLoading.value = false
                val message = e.localizedMessage ?: "An unexpected error occurred"
                signInError.value = message
                Log.e("QuennectAuth", "Sign-in error", e)
                logs.add("[AUTH] Sign-in error: $message")
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                signInLoading.value = false
                if (task.isSuccessful) {
                    userName.value = auth.currentUser?.displayName
                    signInError.value = null
                    logs.add("[AUTH] Welcome to Starship, ${userName.value}!")
                } else {
                    val error = task.exception?.localizedMessage ?: "Unknown error"
                    signInError.value = "Firebase auth failed: $error"
                    logs.add("[AUTH] Firebase authentication failed: $error")
                }
            }
    }

    private fun connectVpn() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, 0)
        } else {
            onActivityResult(0, RESULT_OK, null)
        }
    }

    private fun disconnectVpn() {
        val intent = Intent(this, QuennectVpnService::class.java)
        intent.action = "ACTION_DISCONNECT"
        startService(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val intent = Intent(this, QuennectVpnService::class.java)
            intent.action = "ACTION_CONNECT"
            startService(intent)
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(logReceiver)
        } catch (e: Exception) {}
        super.onDestroy()
    }
}
