package com.autobluetooth.off

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Receiver que se ejecuta al arrancar el dispositivo.
 * Si el usuario tenía activado el monitoreo, lo reinicia automáticamente.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean(MainActivity.KEY_MONITORING_ENABLED, false)

            if (isEnabled) {
                Log.d("BootReceiver", "Dispositivo reiniciado: restaurando servicio de monitoreo BT")
                val serviceIntent = Intent(context, BluetoothMonitorService::class.java).apply {
                    action = BluetoothMonitorService.ACTION_START
                }
                ContextCompat.startForegroundService(context, serviceIntent)
            }
        }
    }
}
