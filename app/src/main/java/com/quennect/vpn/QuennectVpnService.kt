package com.quennect.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import libv2ray.Libv2ray
import libv2ray.CoreController
import libv2ray.CoreCallbackHandler
import java.util.concurrent.TimeUnit

class QuennectVpnService : VpnService(), CoreCallbackHandler {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var coreController: CoreController? = null
    private val CHANNEL_ID = "QuennectVPN"
    
    companion object {
        const val LOG_ACTION = "com.quennect.vpn.LOG"
        const val LOG_EXTRA = "log_msg"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        try {
            coreController = Libv2ray.newCoreController(this)
        } catch (e: Exception) {
            Log.e("Quennect", "Failed to init core: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "ACTION_CONNECT") {
            val notification = createNotification("Authenticating...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(1, notification)
            }
            
            Thread {
                val accessGranted = checkAccess()
                if (accessGranted) {
                    connect()
                } else {
                    stopSelf()
                }
            }.start()
            
        } else if (action == "ACTION_DISCONNECT") {
            disconnect()
        }
        return START_STICKY
    }

    private fun checkAccess(): Boolean {
        val user = Firebase.auth.currentUser
        
        // 1. GUEST ACCESS (Not Logged In)
        if (user == null) {
            sendBroadcastLog("[AUTH] GUEST DETECTED")
            // Normally you would store daily guest usage in Local Storage or Device ID in Firestore
            // For now, we simulate the 2-minute rule
            sendBroadcastLog("[TRIAL] 2 MINUTE GUEST LIMIT ACTIVE")
            sendBroadcastLog("[TRIAL] LOGIN FOR 10 MINUTE EXTENSION")
            
            // Start a timer to kill the connection in 2 minutes
            Thread {
                Thread.sleep(120000) // 2 mins
                if (Firebase.auth.currentUser == null) {
                    sendBroadcastLog("[TRIAL] GUEST LIMIT EXCEEDED. DISCONNECTING.")
                    disconnect()
                }
            }.start()
            return true
        }

        // 2. REGISTERED ACCESS (Logged In)
        val db = Firebase.firestore
        return try {
            val task = db.collection("users").document(user.uid).get()
            val doc = Tasks.await(task, 10, TimeUnit.SECONDS)
            
            val isPremium = doc.getBoolean("is_premium") ?: false
            val expiry = doc.getLong("expiry") ?: 0L
            val now = System.currentTimeMillis()

            if (isPremium && expiry > now) {
                sendBroadcastLog("[AUTH] ACCESS GRANTED: PREMIUM PILOT")
                return true
            } else {
                sendBroadcastLog("[ERROR] TRIAL EXPIRED")
                sendBroadcastLog("[AUTH] 3 DAYS: ₱5 | 1 WEEK: ₱99")
                sendBroadcastLog("[AUTH] VISIT MARSCONNECT.ICEIY.COM TO UPGRADE")
                return false
            }
        } catch (e: Exception) {
            sendBroadcastLog("[ERROR] CLOUD SYNC FAILED")
            false
        }
    }

    private fun connect() {
        sendBroadcastLog("[SYSTEM] Preparing VPN routes...")
        try {
            val builder = Builder()
            vpnInterface = builder
                .setSession("Quennect")
                .setMtu(1500)
                .addAddress("10.0.0.1", 24)
                .addDnsServer("1.1.1.1")
                .addDnsServer("1.0.0.1")
                .addRoute("0.0.0.0", 0)
                .addDisallowedApplication(packageName) 
                .establish()

            if (vpnInterface != null) {
                sendBroadcastLog("[STATUS] QUENNECT IS LIVE")
                updateNotification("Quennect Protected")
                startProxyEngine()
            }
        } catch (e: Exception) {
            sendBroadcastLog("[ERROR] Setup failed: ${e.message}")
            stopSelf()
        }
    }

    private fun startProxyEngine() {
        coreController?.let { controller ->
            val config = QuennectConfig.vlessJsonConfig
            val fd = vpnInterface?.fd ?: -1
            Thread {
                try {
                    sendBroadcastLog("[SERVER] Initializing handshake...")
                    controller.startLoop(config, fd)
                } catch (e: Exception) {
                    sendBroadcastLog("[ERROR] Core crashed: ${e.message}")
                }
            }.start()
        }
    }

    private fun disconnect() {
        sendBroadcastLog("[SYSTEM] Disconnecting...")
        try { coreController?.stopLoop() } catch (e: Exception) {}
        try { vpnInterface?.close() } catch (e: Exception) {}
        vpnInterface = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }

    private fun sendBroadcastLog(msg: String) {
        val intent = Intent(LOG_ACTION)
        intent.putExtra(LOG_EXTRA, msg)
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID, "Quennect VPN Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(content: String): Notification {
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Quennect VPN")
            .setContentText(content)
            .setSmallIcon(com.quennect.vpn.R.drawable.ic_launcher)
            .setOngoing(true)
            .setContentIntent(mainPendingIntent)
            .build()
    }

    private fun updateNotification(content: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(1, createNotification(content))
    }

    override fun startup(): Long = 0
    override fun shutdown(): Long = 0
    override fun onEmitStatus(code: Long, msg: String?): Long {
        msg?.let { sendBroadcastLog(it) }
        return 0
    }

    fun protect(socket: Long): Long {
        return if (protect(socket.toInt())) 0 else 1
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }
}
