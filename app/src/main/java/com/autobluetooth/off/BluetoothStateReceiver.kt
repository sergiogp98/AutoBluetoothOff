package com.autobluetooth.off

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Receiver que escucha cambios de estado del Bluetooth cuando la app está en segundo plano.
 * Declarado en el Manifest para que funcione incluso con la app cerrada.
 */
class BluetoothStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Solo reenviar al servicio si el monitoreo está activo
        val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean(MainActivity.KEY_MONITORING_ENABLED, false)

        if (isEnabled) {
            Log.d("BTStateReceiver", "Evento BT recibido: ${intent.action}, reenviando al servicio")
            val serviceIntent = Intent(context, BluetoothMonitorService::class.java).apply {
                action = BluetoothMonitorService.ACTION_START
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
