package com.autobluetooth.off

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.autobluetooth.off.databinding.ActivityMainBinding

/**
 * Actividad principal de Auto Bluetooth Off.
 * Muestra una interfaz minimalista con un toggle para activar/desactivar
 * la función de apagado automático del Bluetooth.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        (getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    }

    // Launcher para solicitar múltiples permisos
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Permisos concedidos: activar el toggle si el usuario lo quería
            enableMonitoring(true)
        } else {
            Toast.makeText(this, getString(R.string.permissions_required), Toast.LENGTH_LONG).show()
            binding.toggleMonitoring.isChecked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Restaurar estado del toggle desde preferencias
        val isEnabled = prefs.getBoolean(KEY_MONITORING_ENABLED, false)
        binding.toggleMonitoring.isChecked = isEnabled
        updateStatusUI(isEnabled)

        // Listener del toggle principal
        binding.toggleMonitoring.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkPermissionsAndEnable()
            } else {
                enableMonitoring(false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Actualizar UI según el estado actual del servicio
        val isEnabled = prefs.getBoolean(KEY_MONITORING_ENABLED, false)
        binding.toggleMonitoring.isChecked = isEnabled
        updateStatusUI(isEnabled)
        updateBluetoothStatusUI()
    }

    /**
     * Verifica si se tienen los permisos necesarios y los solicita si no.
     */
    private fun checkPermissionsAndEnable() {
        val requiredPermissions = mutableListOf<String>()

        // Android 12+ necesita permisos específicos de BT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }

        // Android 13+ necesita permiso para notificaciones
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (requiredPermissions.isEmpty()) {
            enableMonitoring(true)
        } else {
            permissionLauncher.launch(requiredPermissions.toTypedArray())
        }
    }

    /**
     * Activa o desactiva el monitoreo del Bluetooth.
     */
    private fun enableMonitoring(enable: Boolean) {
        // Guardar preferencia
        prefs.edit().putBoolean(KEY_MONITORING_ENABLED, enable).apply()

        val serviceIntent = Intent(this, BluetoothMonitorService::class.java)

        if (enable) {
            serviceIntent.action = BluetoothMonitorService.ACTION_START
            ContextCompat.startForegroundService(this, serviceIntent)
            Toast.makeText(this, getString(R.string.monitoring_enabled), Toast.LENGTH_SHORT).show()
        } else {
            serviceIntent.action = BluetoothMonitorService.ACTION_STOP
            startService(serviceIntent)
            Toast.makeText(this, getString(R.string.monitoring_disabled), Toast.LENGTH_SHORT).show()
        }

        updateStatusUI(enable)
    }

    /**
     * Actualiza los elementos visuales según el estado del monitoreo.
     */
    private fun updateStatusUI(isEnabled: Boolean) {
        binding.tvStatus.text = if (isEnabled) {
            getString(R.string.status_active)
        } else {
            getString(R.string.status_inactive)
        }

        binding.statusIndicator.setBackgroundResource(
            if (isEnabled) R.drawable.indicator_active else R.drawable.indicator_inactive
        )

        updateBluetoothStatusUI()
    }

    /**
     * Muestra el estado actual del Bluetooth en la UI.
     */
    private fun updateBluetoothStatusUI() {
        val btState = when {
            bluetoothAdapter == null -> getString(R.string.bt_not_supported)
            bluetoothAdapter!!.isEnabled -> getString(R.string.bt_enabled)
            else -> getString(R.string.bt_disabled)
        }
        binding.tvBluetoothState.text = getString(R.string.bluetooth_state, btState)
    }

    companion object {
        const val PREFS_NAME = "AutoBluetoothPrefs"
        const val KEY_MONITORING_ENABLED = "monitoring_enabled"
    }
}
