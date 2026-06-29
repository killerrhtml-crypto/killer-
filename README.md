# Killer Automation - Phase 1: Hilt DI + Security-Crypto

## Overview

Esta es la implementación de **Phase 1** del sistema de automatización avanzado de Android. Esta fase establece la base de infraestructura para inyección de dependencias (DI) y gestión segura de datos.

## Phase 1 Components

### 1. **KillerApplication** (`@HiltAndroidApp`)
- Punto de entrada principal de la aplicación
- Inicializa Hilt DI framework
- Configura logging con Timber
- Árbol de logging personalizado para Release mode

### 2. **AppModule** (Hilt Dependency Injection)
- Proporciona `WindowManager` como Singleton
- Proporciona `MasterKey` para cifrado seguro
- Proporciona `EncryptedDataManager` como Singleton
- Proporciona `KillerDatabase` (será integrada en fases posteriores)

### 3. **EncryptedDataManager** (Security-Crypto)
Gestor de datos cifrados con las siguientes características:

- **MasterKey**: Generado y almacenado de forma segura en el Keystore del sistema Android
- **Encriptación AES-256-GCM**: Para claves de preferencias
- **Encriptación AES-256-SIV**: Para valores de preferencias
- **Métodos disponibles**:
  - `saveString(key, value)` / `getString(key, default)`
  - `saveInt(key, value)` / `getInt(key, default)`
  - `saveFloat(key, value)` / `getFloat(key, default)`
  - `saveBoolean(key, value)` / `getBoolean(key, default)`
  - `remove(key)` / `clear()`
  - `contains(key)`
  - `getOrGenerateDatabasePassphrase()` - Para SQLCipher
  - `saveAuthToken(token)` / `getAuthToken()`
  - `saveRefreshToken(token)` / `getRefreshToken()`

### 4. **Build Configuration**
- Kotlin 1.9.10
- Android SDK 34 (target)
- Android minSdk 24
- Gradle 8.1.2

### 5. **ProGuard/R8 Rules**
- Configuración completa para ofuscación de código
- Mantiene clases críticas de Hilt, Room, y Retrofit
- Elimina logs en modo Release
- Preserva información de línea para stack traces

## Dependencies

### Core Android
```
androidx.core:core-ktx:1.12.0
androidx.appcompat:appcompat:1.6.1
androidx.activity:activity-compose:1.8.0
androidx.lifecycle:lifecycle-runtime-ktx:2.6.2
```

### Jetpack Compose
```
androidx.compose.ui:ui:1.6.0
androidx.compose.material3:material3:1.1.1
androidx.compose.foundation:foundation:1.6.0
```

### Dependency Injection (Hilt)
```
com.google.dagger:hilt-android:2.48
com.google.dagger:hilt-compiler:2.48
androidx.hilt:hilt-navigation-compose:1.1.0
```

### Security & Encryption
```
androidx.security:security-crypto:1.1.0-alpha06
com.google.crypto.tink:tink-android:1.10.0
net.zetetic:android-database-sqlcipher:4.5.4
```

### Networking
```
com.squareup.retrofit2:retrofit:2.9.0
com.squareup.okhttp3:okhttp:4.11.0
```

### Logging
```
com.jakewharton.timber:timber:5.0.1
```

## Project Structure

```
app/src/main/
├── java/com/killer/automation/
│   ├── KillerApplication.kt          # @HiltAndroidApp entry point
│   ├── di/
│   │   └── AppModule.kt              # Hilt dependency provides
│   ├── data/
│   │   ├── security/
│   │   │   └── EncryptedDataManager.kt
│   │   ├── database/
│   │   │   ├── KillerDatabase.kt
│   │   │   └── DatabaseConverters.kt
│   │   └── models/
│   │       └── (Models - próximas fases)
│   ├── ui/
│   │   ├── MainActivity.kt
│   │   └── theme/
│   │       ├── Theme.kt
│   │       └── Type.kt
│   └── core/
│       └── (Components - próximas fases)
├── res/
│   ├── values/
│   │   ├── strings.xml
│   │   ├── colors.xml
│   │   └── themes.xml
│   └── xml/
│       └── data_extraction_rules.xml
└── AndroidManifest.xml
```

## Usage Example

### Inyecting EncryptedDataManager in a Component

```kotlin
@AndroidEntryPoint
class MyActivity : ComponentActivity() {
    
    @Inject
    lateinit var encryptedDataManager: EncryptedDataManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Usar el EncryptedDataManager
        encryptedDataManager.saveString("user_token", "abc123xyz")
        val token = encryptedDataManager.getString("user_token")
        
        // Guardar datos numéricos
        encryptedDataManager.saveInt("user_id", 12345)
        val userId = encryptedDataManager.getInt("user_id")
    }
}
```

### Database Passphrase Management

```kotlin
@Inject
lateinit var encryptedDataManager: EncryptedDataManager

// Obtener o generar la contraseña para SQLCipher
val passphrase = encryptedDataManager.getOrGenerateDatabasePassphrase()
// La contraseña se almacena de forma cifrada automáticamente
```

## Security Features

### 1. **MasterKey en Android Keystore**
- El MasterKey se genera y almacena en el Keystore seguro del dispositivo
- No se puede acceder directamente desde el código
- Protegido por los mecanismos de seguridad del sistema (biometría, PIN, etc.)

### 2. **Encriptación en Reposo**
- Todos los datos en EncryptedSharedPreferences están cifrados
- AES-256-GCM para valores
- AES-256-SIV para claves

### 3. **Base de Datos Cifrada**
- SQLCipher para encriptación de base de datos
- Contraseña generada dinámicamente y almacenada de forma segura

### 4. **Logging Seguro**
- En Release mode, solo se registran errores críticos
- Información sensitiva nunca se registra
- Compatible con Firebase Crashlytics

## Building and Running

### Development Build
```bash
./gradlew assembleDebug
```

### Release Build (con ofuscación)
```bash
./gradlew assembleRelease
```

### Running Tests
```bash
./gradlew test
./gradlew connectedAndroidTest
```

## Next Phases

- **Phase 2**: AccessibilityService con GestureDispatcher
- **Phase 3**: Overlay Widget System (WindowManager)
- **Phase 4**: Authentication & JWT Validation
- **Phase 5**: Filtering Engine & Geofencing
- **Phase 6**: Touch Event Interceptor

## Important Notes

1. **Android Manifest**: Configurado con permisos necesarios para la automatización
2. **ProGuard/R8**: Reglas completas para ofuscación manteniendo funcionalidad
3. **Error Handling**: Todos los métodos de EncryptedDataManager manejan excepciones gracefully
4. **Timber Logging**: Logs detallados en desarrollo, mínimos en producción

## License

Proprietary - Killer Automation

## Author

Senior Android Developer
