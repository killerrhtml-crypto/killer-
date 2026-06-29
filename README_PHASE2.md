# Phase 2: Servicios de Interacción - AccessibilityService, GestureInterceptor & Overlay

## Overview

Phase 2 implementa la capa de automatización core con tres componentes principales:

1. **AutomationAccessibilityService** - Servicio de accesibilidad para interacción global de UI
2. **GestureInterceptor** - Interceptor de eventos que bloquea acciones nativas
3. **FloatingWidgetService** - Widget flotante persistente sobre otras aplicaciones

---

## 1. AutomationAccessibilityService

### Características

- **@AndroidEntryPoint** - Inyección de dependencias Hilt
- **AccessibilityServiceInfo** - Configuración de eventos a escuchar
- **GestureDispatcher** - Despacho de gestos con jitter
- **Event Handling** - Manejo de TYPE_VIEW_CLICKED y TYPE_TOUCH_INTERACTION_START

### Eventos Escuchados

```kotlin
AccessibilityEvent.TYPE_VIEW_CLICKED              // Clic en vista
AccessibilityEvent.TYPE_TOUCH_INTERACTION_START   // Inicio de toque
AccessibilityEvent.TYPE_TOUCH_INTERACTION_END     // Fin de toque
AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED      // Cambio de ventana
```

### Métodos Principales

#### `dispatchGestureWithJitter(path, duration, jitterRadius)`
```kotlin
/**
 * Ejecuta un gesto con jitter aleatorio (±3px) para simular entrada humana
 * 
 * @param path Ruta del gesto (Path con coordenadas)
 * @param duration Duración en milisegundos (default 300ms)
 * @param jitterRadius Radio del jitter en píxeles (default ±3px)
 */
fun dispatchGestureWithJitter(
    path: Path,
    duration: Long = GESTURE_DURATION_SWIPE,
    jitterRadius: Float = JITTER_RADIUS
)
```

**Jitter Implementation:**
```kotlin
private fun createJitteredGesture(
    originalPath: Path,
    duration: Long,
    jitterRadius: Float
): GestureDescription {
    val jitteredPath = Path(originalPath)
    
    // Jitter aleatorio en rango [-jitterRadius, +jitterRadius]
    val random = Random.Default
    val jitterX = random.nextFloat() * jitterRadius * 2 - jitterRadius  // ±3px
    val jitterY = random.nextFloat() * jitterRadius * 2 - jitterRadius  // ±3px
    
    jitteredPath.offset(jitterX, jitterY)
    
    return GestureDescription.Builder()
        .addStroke(GestureDescription.StrokeDescription(jitteredPath, 0, duration))
        .build()
}
```

#### `performClickWithJitter(x, y, jitterRadius)`
```kotlin
/**
 * Ejecuta un clic con jitter en coordenadas específicas
 * Simula un toque humano impreciso
 */
fun performClickWithJitter(
    x: Float,
    y: Float,
    jitterRadius: Float = JITTER_RADIUS
)
```

#### `performSwipeWithJitter(startX, startY, endX, endY, duration, jitterRadius)`
```kotlin
/**
 * Ejecuta un swipe (deslizamiento) con jitter
 * El swipe es un gesto continuo desde un punto de inicio a uno final
 */
fun performSwipeWithJitter(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    duration: Long = GESTURE_DURATION_SWIPE,
    jitterRadius: Float = JITTER_RADIUS
)
```

#### `performScrollWithJitter(startX, startY, endX, endY, steps, duration)`
```kotlin
/**
 * Ejecuta un scroll con múltiples puntos interpolados
 * Crea movimiento suave simulando scroll natural
 */
fun performScrollWithJitter(
    startX: Float,
    startY: Float,
    endX: Float,
    endY: Float,
    steps: Int = 10,
    duration: Long = GESTURE_DURATION_SWIPE
)
```

#### `performMultiTouchGesture(paths, duration)`
```kotlin
/**
 * Ejecuta gestos multitoque (pinch, doble tap, etc.)
 * Permite múltiples toques simultáneos
 */
fun performMultiTouchGesture(
    paths: List<Path>,
    duration: Long = GESTURE_DURATION_SWIPE
)
```

### Utilidades de Nodos

#### `getRootNode()`
```kotlin
/**
 * Obtiene el AccessibilityNodeInfo raíz de la ventana actual
 * Útil para navegar la jerarquía de vistas
 */
fun getRootNode(): AccessibilityNodeInfo?
```

#### `findNodeByText(text)`
```kotlin
/**
 * Busca un nodo específico por su contenido de texto
 * Útil para ubicar vistas específicas en la jerarquía
 */
fun findNodeByText(text: String): AccessibilityNodeInfo?
```

### Configuración en AndroidManifest

```xml
<service
    android:name=".core.accessibility.AutomationAccessibilityService"
    android:exported="true"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:label="@string/accessibility_service_description">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

---

## 2. GestureInterceptor

### Características

- **@ServiceScoped** - Ciclo de vida vinculado al servicio
- **Event Detection** - Identifica eventos en botones objetivo
- **Native Action Blocking** - Bloquea acciones nativas
- **Programmatic Gesture Delegation** - Delega a gestos programáticos

### Patrones de Botones Objetivo

```kotlin
defaultTargetPatterns = listOf(
    "accept",      // Aceptar
    "reject",      // Rechazar
    "decline",     // Declinar
    "confirm",     // Confirmar
    "approve",     // Aprobar
    "deny",        // Denegar
    "yes",         // Sí
    "no",          // No
    "ok",          // OK
    "cancel",      // Cancelar
    "submit",      // Enviar
    "ok_btn",      // Botón OK
    "reject_btn"   // Botón Rechazar
)
```

### Métodos Principales

#### `handleAccessibilityEvent(event)`
```kotlin
/**
 * Maneja eventos de accesibilidad
 * Identifica y bloquea acciones en botones objetivo
 */
suspend fun handleAccessibilityEvent(event: AccessibilityEvent)
```

**Flujo de Procesamiento:**
```
1. Evento de Accesibilidad Recibido
   ↓
2. Verificar tipo de evento (CLICK, TOUCH_START, etc.)
   ↓
3. Obtener AccessibilityNodeInfo del evento
   ↓
4. Verificar si el nodo es un botón objetivo
   ↓
5. SI es objetivo:
   - Bloquear nodo en lista negra
   - Obtener coordenadas del nodo
   - Ejecutar gesto programático
   ↓
6. SI no es objetivo:
   - Permitir acción nativa
```

#### `shouldBlockNode(node)`
```kotlin
/**
 * Verifica si un nodo debe ser bloqueado
 * Comprueba:
 * - Clase del nodo (debe ser Button)
 * - Descripción de contenido
 * - Texto visible
 * - ID del recurso
 */
private suspend fun shouldBlockNode(node: AccessibilityNodeInfo): Boolean
```

#### `performProgrammaticGesture(sourceNode)`
```kotlin
/**
 * Ejecuta un gesto programático en respuesta a un botón bloqueado
 * Obtiene coordenadas del nodo y ejecuta un swipe desde esa posición
 */
private suspend fun performProgrammaticGesture(sourceNode: AccessibilityNodeInfo)
```

#### Gestión de Patrones Personalizados

```kotlin
// Agregar patrón personalizado
gestureInterceptor.addTargetButtonPattern("custom_button")

// Eliminar patrón
gestureInterceptor.removeTargetButtonPattern("custom_button")

// Resetear a patrones predeterminados
gestureInterceptor.resetTargetButtonPatterns()

// Obtener patrones actuales
val patterns = gestureInterceptor.getTargetButtonPatterns()
```

---

## 3. FloatingWidgetService

### Características

- **WindowManager Integration** - Manejo de ventanas flotantes
- **Compose UI** - Renderizado con Jetpack Compose
- **Drag-and-Drop** - Widget arrastrable
- **Persistent Overlay** - Visible sobre otras aplicaciones

### Estructura

```kotlin
@AndroidEntryPoint
class FloatingWidgetService : Service()
    ↓
    ├─ WindowManager (inyectado)
    ├─ GestureInterceptor (inyectado)
    ├─ ComposeView (widget UI)
    └─ LayoutParams (configuración de ventana)
```

### Métodos Principales

#### `onStartCommand(intent, flags, startId)`
```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    createFloatingWidget()
    return START_STICKY  // Reiniciar si es matado
}
```

#### `createFloatingWidget()`
```kotlin
/**
 * Crea el widget flotante y lo agrega al WindowManager
 * - Renderiza ComposeView con FloatingWidgetContent
 * - Configura LayoutParams
 * - Agrega vista al WindowManager
 */
private fun createFloatingWidget()
```

#### `updateWidgetPosition(x, y)`
```kotlin
/**
 * Actualiza la posición del widget en la pantalla
 * Llamado durante el arrastre
 */
private fun updateWidgetPosition(x: Int, y: Int)
```

### Configuración de WindowManager.LayoutParams

```kotlin
params = WindowManager.LayoutParams().apply {
    type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY  // Android O+
    
    format = PixelFormat.TRANSLUCENT  // Pixeles transparentes
    
    flags = FLAG_NOT_FOCUSABLE or        // No recibe foco
            FLAG_NOT_TOUCH_MODAL or      // No modalidad
            FLAG_LAYOUT_NO_LIMITS        // Sin límites de pantalla
    
    gravity = Gravity.TOP or Gravity.START  // Esquina superior izquierda
    
    width = 100    // 100dp
    height = 100   // 100dp
    x = 0          // Posición X inicial
    y = 100        // Posición Y inicial
    
    alpha = 0.95f  // 95% opacidad
}
```

### FloatingWidgetContent (Composable)

```kotlin
@Composable
private fun FloatingWidgetContent(
    onWidgetTapped: (Float, Float) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .size(100.dp)
            .background(
                color = Color(0xFF1976D2),
                shape = CircleShape
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Play Automation",
            tint = Color.White,
            modifier = Modifier.size(50.dp)
        )
    }
}
```

### Configuración en AndroidManifest

```xml
<service
    android:name=".core.overlay.FloatingWidgetService"
    android:exported="false"
    android:label="@string/floating_widget_description" />
```

### Permisos Requeridos

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.SYSTEM_OVERLAY_WINDOW" />
```

---

## Estructura de Carpetas

```
app/src/main/
├── java/com/killer/automation/
│   ├── core/
│   │   ├── accessibility/
│   │   │   └── AutomationAccessibilityService.kt
│   │   ├── gesture/
│   │   │   └── GestureInterceptor.kt
│   │   └── overlay/
│   │       ├── FloatingWidgetService.kt
│   │       └── FloatingWidgetView.kt
│   └── di/
│       └── AccessibilityModule.kt
├── res/
│   ├── values/
│   │   └── strings.xml (actualizado)
│   └── xml/
│       └── accessibility_service_config.xml
└── AndroidManifest.xml (actualizado)
```

---

## Uso en la Aplicación

### Iniciar FloatingWidgetService

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Iniciar widget flotante
        val intent = Intent(this, FloatingWidgetService::class.java)
        startService(intent)
    }
}
```

### Habilitar AccessibilityService

El usuario debe habilitar manualmente en:
**Configuración → Accesibilidad → Servicios → Killer Automation**

### Personalizar Patrones de Botones

```kotlin
@Inject
lateinit var gestureInterceptor: GestureInterceptor

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Agregar patrones personalizados
    gestureInterceptor.addTargetButtonPattern("accept_ride")
    gestureInterceptor.addTargetButtonPattern("confirm_price")
    
    // Obtener patrones actuales
    val patterns = gestureInterceptor.getTargetButtonPatterns()
}
```

---

## Flow de Ejecución Completo

### Scenario: Usuario toca un botón "Accept"

```
1. Usuario toca botón en app objetivo
   ↓
2. AccessibilityService recibe AccessibilityEvent.TYPE_VIEW_CLICKED
   ↓
3. GestureInterceptor.handleAccessibilityEvent() es llamado
   ↓
4. shouldBlockNode() verifica si es botón objetivo
   - Clase: Button ✓
   - Texto contiene "accept" ✓
   ↓
5. Botón es bloqueado (nodeId agregado a blockedNodeIds)
   ↓
6. performProgrammaticGesture() obtiene coordenadas del botón
   ↓
7. performSwipeWithJitter() es ejecutado
   - Ruta: desde botón hacia arriba
   - Jitter: ±3px aleatorio aplicado
   - Duración: 300ms
   ↓
8. dispatchGesture() ejecuta el gesto
   ↓
9. Callback confirma ejecución exitosa
   ↓
10. Logging registra la acción
```

---

## Consideraciones de Seguridad

1. **Permissions Runtime** - Solicitar permisos en runtime para Android 6.0+
2. **User Consent** - Usuario debe habilitar servicio de accesibilidad
3. **Logging Seguro** - No loguear datos sensitivos en production
4. **ProGuard Rules** - Mantener clases de accesibilidad en release builds

---

## Próximos Pasos (Phase 3)

- [ ] Autenticación JWT y validación remota
- [ ] Motor de filtrado de datos
- [ ] Geofencing y zonificación
- [ ] Persistencia de configuración cifrada
- [ ] Tests unitarios y de integración

---

## Dependencies

```gradle
// Ya incluidas en Phase 1
androidx.activity:activity-compose:1.8.0
androidx.compose.material3:material3:1.1.1
kotlinx.coroutines.android:1.7.3
```

---

## License

Proprietary - Killer Automation
