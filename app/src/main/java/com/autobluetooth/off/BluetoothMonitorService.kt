package com.autobluetooth.off

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Servicio en primer plano (Foreground Service) que monitorea el Bluetooth.
 *
 * Lógica principal:
 * 1. Cuando el Bluetooth se activa → inicia temporizador de 60 segundos.
 * 2. Si en esos 60s NO hay conexión → apaga el Bluetooth automáticamente.
 * 3. Si hay conexión → cancela el temporizador (no apaga nada).
 * 4. Si el dispositivo se desconecta → reinicia el temporizador.
 */
class BluetoothMonitorService : Service() {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    private var countdownTimer: CountDownTimer? = null
    private var isDeviceConnected = false
    private var remainingSeconds = TIMEOUT_SECONDS

    // Receiver interno para escuchar eventos de Bluetooth
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> handleBluetoothStateChange(intent)
                BluetoothDevice.ACTION_ACL_CONNECTED   -> handleDeviceConnected()
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> handleDeviceDisconnected()
            }
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Ciclo de vida del servicio
    // ──────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerBluetoothReceiver()
        Log.d(TAG, "Servicio creado")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                Log.d(TAG, "Iniciando monitoreo")
                startForeground(NOTIFICATION_ID, buildNotification(getString(R.string.notif_monitoring)))
                // Si el BT ya está encendido al arrancar el servicio, iniciar temporizador
                if (bluetoothAdapter?.isEnabled == true && !isDeviceConnected) {
                    startCountdown()
                }
            }
            ACTION_STOP -> {
                Log.d(TAG, "Deteniendo monitoreo")
                stopSelf()
            }
            ACTION_UPDATE_NOTIF -> {
                // Actualiza la notificación desde el timer tick
                val msg = intent.getStringExtra(EXTRA_NOTIF_MSG) ?: getString(R.string.notif_monitoring)
                updateNotification(msg)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelCountdown()
        unregisterReceiver(bluetoothReceiver)
        Log.d(TAG, "Servicio destruido")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ──────────────────────────────────────────────────────────────
    // Handlers de eventos Bluetooth
    // ──────────────────────────────────────────────────────────────

    private fun handleBluetoothStateChange(intent: Intent) {
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
        Log.d(TAG, "Estado BT cambiado: $state")
        when (state) {
            BluetoothAdapter.STATE_ON -> {
                // Bluetooth se acaba de encender → iniciar cuenta atrás
                if (!isDeviceConnected) {
                    startCountdown()
                }
            }
            BluetoothAdapter.STATE_OFF -> {
                // Bluetooth apagado (ya sea por nosotros o por el usuario) → cancelar
                cancelCountdown()
                isDeviceConnected = false
                updateNotification(getString(R.string.notif_bt_off))
            }
        }
    }

    private fun handleDeviceConnected() {
        Log.d(TAG, "Dispositivo Bluetooth conectado")
        isDeviceConnected = true
        cancelCountdown()
        updateNotification(getString(R.string.notif_connected))
    }

    private fun handleDeviceDisconnected() {
        Log.d(TAG, "Dispositivo Bluetooth desconectado")
        isDeviceConnected = false
        // Reiniciar temporizador si el BT sigue encendido
        if (bluetoothAdapter?.isEnabled == true) {
            startCountdown()
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Lógica del temporizador
    // ──────────────────────────────────────────────────────────────

    /**
     * Inicia la cuenta atrás de 60 segundos.
     * Si expira sin conexión → apaga el Bluetooth.
     */
    private fun startCountdown() {
        cancelCountdown() // Evitar timers duplicados
        remainingSeconds = TIMEOUT_SECONDS
        Log.d(TAG, "Iniciando countdown de $TIMEOUT_SECONDS segundos")

        countdownTimer = object : CountDownTimer(TIMEOUT_SECONDS * 1000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                remainingSeconds = (millisUntilFinished / 1000).toInt()
                val msg = getString(R.string.notif_countdown, remainingSeconds)
                updateNotification(msg)
                Log.d(TAG, "Countdown: $remainingSeconds s restantes")
            }

            override fun onFinish() {
                Log.d(TAG, "Countdown terminado. Sin conexión → apagando Bluetooth")
                disableBluetooth()
            }
        }.start()
    }

    private fun cancelCountdown() {
        countdownTimer?.cancel()
        countdownTimer = null
        Log.d(TAG, "Countdown cancelado")
    }

    /**
     * Desactiva el Bluetooth del dispositivo.
     * En Android 13+, se abre el panel de configuración ya que la API directa fue eliminada.
     */
    @Suppress("DEPRECATION")
    private fun disableBluetooth() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                // Android 10–12: desactivación directa (deprecated pero funcional)
                bluetoothAdapter?.disable()
                Log.d(TAG, "Bluetooth desactivado directamente")
            } else {
                // Android 13+: notificar al usuario (Android eliminó la API directa)
                updateNotification(getString(R.string.notif_manual_off))
                Log.d(TAG, "Android 13+: notificando al usuario para apagar BT manualmente")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Sin permiso para desactivar Bluetooth: ${e.message}")
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Notificaciones
    // ──────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notif_channel_desc)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(message: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_bluetooth)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification(message: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(message))
    }

    // ──────────────────────────────────────────────────────────────
    // BroadcastReceiver
    // ──────────────────────────────────────────────────────────────

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothReceiver, filter)
    }

    companion object {
        private const val TAG = "BTMonitorService"
        const val ACTION_START = "com.autobluetooth.off.START"
        const val ACTION_STOP  = "com.autobluetooth.off.STOP"
        const val ACTION_UPDATE_NOTIF = "com.autobluetooth.off.UPDATE_NOTIF"
        const val EXTRA_NOTIF_MSG = "notif_message"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "bt_monitor_channel"
        const val TIMEOUT_SECONDS = 60
    }
}
