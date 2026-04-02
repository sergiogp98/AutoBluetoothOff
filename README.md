# 📵 Auto Bluetooth Off — App Android

Aplicación Android en Kotlin que **apaga el Bluetooth automáticamente** si transcurren 60 segundos sin que ningún dispositivo se conecte.

---

## 🧠 Cómo funciona internamente

```
Usuario activa el toggle
        │
        ▼
BluetoothMonitorService arranca (Foreground Service)
        │
        ├── BT ya estaba encendido? ──► Inicia cuenta atrás (60s)
        │
BluetoothStateReceiver escucha eventos:
        │
        ├── BT se enciende ──────────► Inicia cuenta atrás (60s)
        │                                       │
        │                              ¿Conecta dispositivo? ──► SÍ → Cancela timer ✓
        │                                       │
        │                                       NO → Apaga BT al llegar a 0 ✗
        │
        ├── Dispositivo conectado ──► Cancela timer (BT se queda encendido)
        │
        └── Dispositivo desconectado ► Reinicia timer (60s de nuevo)
```

---

## 📁 Estructura del proyecto

```
AutoBluetoothOff/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/autobluetooth/off/
│       │   ├── MainActivity.kt            ← UI + toggle
│       │   ├── BluetoothMonitorService.kt ← Lógica + timer + notificación
│       │   ├── BluetoothStateReceiver.kt  ← Escucha cambios BT del sistema
│       │   └── BootReceiver.kt            ← Restaura el servicio tras reinicio
│       └── res/
│           ├── layout/activity_main.xml
│           ├── values/{strings, colors, themes}.xml
│           └── drawable/{ic_bluetooth, ic_bluetooth_large, indicators}.xml
├── build.gradle
└── settings.gradle
```

---

## ⚙️ Requisitos

| Elemento        | Versión             |
|-----------------|---------------------|
| Android mínimo  | Android 10 (API 29) |
| Target SDK      | Android 14 (API 34) |
| Lenguaje        | Kotlin 1.9.x        |
| Android Studio o SDK | Hedgehog o superior |

Además, hay que añadir un archivo llamdo _local.properties_ con el siguiente contenido:

```properties
sdk.dir=ANDROID_SDK_DIR_PATH
```
O crear una variable de entorno llamada _ANDROID\_HOME_ con la ruta al directorio de instalación de Android. 

---

## 📦 Permisos utilizados

| Permiso | Para qué |
|---|---|
| `BLUETOOTH_CONNECT` | Leer estado y desactivar Bluetooth |
| `BLUETOOTH_SCAN` | Detectar dispositivos cercanos |
| `FOREGROUND_SERVICE` | Mantener el servicio activo |
| `POST_NOTIFICATIONS` | Mostrar notificación persistente |
| `RECEIVE_BOOT_COMPLETED` | Restaurar el servicio al reiniciar |

---

## 🔨 Compilar e instalar

### Opción A — Android Studio (recomendado)

1. Abre Android Studio → `File > Open` → selecciona la carpeta `AutoBluetoothOff/`
2. Deja que Gradle sincronice las dependencias
3. Conecta un dispositivo Android o lanza un emulador (API 29+)
4. Pulsa ▶ **Run 'app'**

### Opción B — Línea de comandos

```bash
# Desde la raíz del proyecto
./gradlew assembleDebug

# Instalar en dispositivo conectado
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚠️ Nota sobre Android 13+

A partir de Android 13 (API 33), Google eliminó la API para desactivar el Bluetooth directamente desde una app de terceros sin ser app de sistema.  
En estos dispositivos, la app **muestra una notificación** pidiendo al usuario que apague el Bluetooth manualmente cuando el tiempo expira.  
En Android 10–12, el apagado es completamente automático.

---

## 🧩 Dependencias

```gradle
implementation 'androidx.core:core-ktx:1.12.0'
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.cardview:cardview:1.0.0'
```

---

*Desarrollado con ❤️ usando Kotlin y Material Design 3*
