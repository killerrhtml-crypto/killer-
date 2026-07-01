package com.killer.automation.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.killer.automation.data.UserPreferences
import com.killer.automation.data.ZonePreference
import com.killer.automation.engine.DecisionEngine
import com.killer.automation.manager.SubscriptionManager
import com.killer.automation.utils.EncryptedDataManager
import kotlin.math.roundToInt

/**
 * SettingsActivity - Interfaz de configuración para que el chófer ajuste sus filtros
 */
class SettingsActivity : ComponentActivity() {
    
    private lateinit var encryptedDataManager: EncryptedDataManager
    private lateinit var decisionEngine: DecisionEngine
    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var locationManager: LocationManager
    
    private var currentLatitude by mutableStateOf(0.0)
    private var currentLongitude by mutableStateOf(0.0)
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            getCurrentLocation()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        encryptedDataManager = EncryptedDataManager(this)
        decisionEngine = DecisionEngine.getInstance(encryptedDataManager)
        subscriptionManager = SubscriptionManager.getInstance(this)
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        
        setContent {
            SettingsScreen()
        }
    }
    
    @Composable
    private fun SettingsScreen() {
        val currentPreferences = remember {
            mutableStateOf(decisionEngine.getUserPreferences() ?: UserPreferences.getDefaults())
        }
        
        var minPrice by remember { mutableStateOf(currentPreferences.value.minPrice.toFloat()) }
        var pricePerKm by remember { mutableStateOf(currentPreferences.value.pricePerKm.toFloat()) }
        var maxDistance by remember { mutableStateOf(currentPreferences.value.maxDistance.toFloat()) }
        var autoAccept by remember { mutableStateOf(currentPreferences.value.autoAccept) }
        var showSaveZoneDialog by remember { mutableStateOf(false) }
        var zoneName by remember { mutableStateOf("") }
        var zoneRadius by remember { mutableStateOf("2.0") }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF0d0d12))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Text(
                "KILLER CONFIG",
                color = Color(0xFFfbbf24),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                "Personaliza tus filtros de viajes",
                color = Color(0xFFA1A1A7),
                fontSize = 12.sp,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Estado de suscripción
            SubscriptionStatusCard(subscriptionManager)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Precio Mínimo
            SectionTitle("💰 Filtros de Precio")
            
            PriceSliderCard(
                title = "Precio Mínimo",
                value = minPrice,
                onValueChange = { minPrice = it },
                range = 0f..500f,
                unit = "RD$"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PriceSliderCard(
                title = "Precio por km",
                value = pricePerKm,
                onValueChange = { pricePerKm = it },
                range = 10f..100f,
                unit = "RD$/km"
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            PriceSliderCard(
                title = "Distancia Máxima",
                value = maxDistance,
                onValueChange = { maxDistance = it },
                range = 5f..100f,
                unit = "km"
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Geofencing
            SectionTitle("📍 Geofencing de Zonas")
            
            SaveZoneButton(
                onClickRequestLocation = {
                    requestLocationPermissionAndGetCurrent()
                },
                onClickSaveZone = {
                    showSaveZoneDialog = true
                },
                currentLat = currentLatitude,
                currentLon = currentLongitude
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ZonesDisplay(
                zones = currentPreferences.value.blockedZones,
                title = "🚫 Zonas Bloqueadas",
                onDelete = { zone ->
                    val updated = currentPreferences.value.blockedZones.filter { it != zone }
                    currentPreferences.value = currentPreferences.value.copy(blockedZones = updated)
                    decisionEngine.updateUserPreferences(currentPreferences.value)
                }
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            ZonesDisplay(
                zones = currentPreferences.value.preferredZones,
                title = "✅ Zonas Preferidas",
                onDelete = { zone ->
                    val updated = currentPreferences.value.preferredZones.filter { it != zone }
                    currentPreferences.value = currentPreferences.value.copy(preferredZones = updated)
                    decisionEngine.updateUserPreferences(currentPreferences.value)
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Opciones adicionales
            SectionTitle("⚙️ Opciones")
            
            AutoAcceptToggle(
                autoAccept = autoAccept,
                onToggle = { autoAccept = it }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Botón Guardar
            SaveButton(
                minPrice = minPrice.toDouble(),
                pricePerKm = pricePerKm.toDouble(),
                maxDistance = maxDistance.toDouble(),
                autoAccept = autoAccept,
                currentPreferences = currentPreferences.value,
                decisionEngine = decisionEngine,
                onSave = {
                    currentPreferences.value = it
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Dialog para guardar zona
        if (showSaveZoneDialog) {
            SaveZoneDialog(
                zoneName = zoneName,
                zoneRadius = zoneRadius,
                latitude = currentLatitude,
                longitude = currentLongitude,
                onSave = { name, radius, isPreferred ->
                    val newZone = ZonePreference(
                        name = name,
                        latitude = currentLatitude,
                        longitude = currentLongitude,
                        radiusKm = radius,
                        zoneType = if (isPreferred) {
                            ZonePreference.ZoneType.PREFERRED
                        } else {
                            ZonePreference.ZoneType.BLOCKED
                        }
                    )
                    
                    if (isPreferred) {
                        currentPreferences.value = currentPreferences.value.copy(
                            preferredZones = currentPreferences.value.preferredZones + newZone
                        )
                    } else {
                        currentPreferences.value = currentPreferences.value.copy(
                            blockedZones = currentPreferences.value.blockedZones + newZone
                        )
                    }
                    
                    decisionEngine.updateUserPreferences(currentPreferences.value)
                    showSaveZoneDialog = false
                    zoneName = ""
                    zoneRadius = "2.0"
                },
                onDismiss = { showSaveZoneDialog = false }
            )
        }
    }
    
    @Composable
    private fun SubscriptionStatusCard(subscriptionManager: SubscriptionManager) {
        val status = subscriptionManager.getSubscriptionStatus()
        val statusColor = when (status) {
            SubscriptionManager.SubscriptionStatus.ACTIVE -> Color(0xFF22C55E)
            SubscriptionManager.SubscriptionStatus.BLOCKED -> Color(0xFFEF4444)
            SubscriptionManager.SubscriptionStatus.EXPIRED -> Color(0xFFF59E0B)
            else -> Color(0xFF6B7280)
        }
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F2937))
                .padding(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Estado de Suscripción", color = Color(0xFFA1A1A7), fontSize = 12.sp)
                    Text(
                        status.name,
                        color = statusColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Indicador visual
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(statusColor, shape = androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
    }
    
    @Composable
    private fun SectionTitle(title: String) {
        Text(
            title,
            color = Color(0xFFfbbf24),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
    
    @Composable
    private fun PriceSliderCard(
        title: String,
        value: Float,
        onValueChange: (Float) -> Unit,
        range: ClosedFloatingPointRange<Float>,
        unit: String
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F2937)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(title, color = Color(0xFFE5E7EB), fontWeight = FontWeight.Bold)
                    Text(
                        "${value.roundToInt()} $unit",
                        color = Color(0xFFfbbf24),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = range,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFfbbf24),
                        activeTrackColor = Color(0xFFfbbf24),
                        inactiveTrackColor = Color(0xFF374151)
                    )
                )
            }
        }
    }
    
    @Composable
    private fun SaveZoneButton(
        onClickRequestLocation: () -> Unit,
        onClickSaveZone: () -> Unit,
        currentLat: Double,
        currentLon: Double
    ) {
        Button(
            onClick = {
                if (currentLat == 0.0) {
                    onClickRequestLocation()
                } else {
                    onClickSaveZone()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E))
        ) {
            Text(
                if (currentLat == 0.0) "📍 Obtener Ubicación Actual" else "💾 Guardar Zona Actual",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
    
    @Composable
    private fun ZonesDisplay(
        zones: List<ZonePreference>,
        title: String,
        onDelete: (ZonePreference) -> Unit
    ) {
        if (zones.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1F2937)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
            ) {
                Text(
                    "$title - Sin zonas configuradas",
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp
                )
            }
        } else {
            Text(title, color = Color(0xFFE5E7EB), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            zones.forEach { zone ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111827))
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(zone.name, color = Color(0xFFE5E7EB), fontWeight = FontWeight.Bold)
                            Text(
                                "${zone.radiusKm} km radio",
                                color = Color(0xFF9CA3AF),
                                fontSize = 11.sp
                            )
                        }
                        
                        Button(
                            onClick = { onDelete(zone) },
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                        ) {
                            Text("Eliminar", color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
    
    @Composable
    private fun AutoAcceptToggle(autoAccept: Boolean, onToggle: (Boolean) -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1F2937)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F2937))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Auto-aceptar ofertas", color = Color(0xFFE5E7EB), fontWeight = FontWeight.Bold)
                Switch(
                    checked = autoAccept,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFFfbbf24),
                        checkedTrackColor = Color(0xFFfbbf24).copy(alpha = 0.3f)
                    )
                )
            }
        }
    }
    
    @Composable
    private fun SaveButton(
        minPrice: Double,
        pricePerKm: Double,
        maxDistance: Double,
        autoAccept: Boolean,
        currentPreferences: UserPreferences,
        decisionEngine: DecisionEngine,
        onSave: (UserPreferences) -> Unit
    ) {
        Button(
            onClick = {
                val updated = currentPreferences.copy(
                    minPrice = minPrice,
                    pricePerKm = pricePerKm,
                    maxDistance = maxDistance,
                    autoAccept = autoAccept
                )
                decisionEngine.updateUserPreferences(updated)
                onSave(updated)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFfbbf24))
        ) {
            Text("💾 Guardar Configuración", color = Color(0xFF0d0d12), fontWeight = FontWeight.Bold)
        }
    }
    
    @Composable
    private fun SaveZoneDialog(
        zoneName: String,
        zoneRadius: String,
        latitude: Double,
        longitude: Double,
        onSave: (String, Double, Boolean) -> Unit,
        onDismiss: () -> Unit
    ) {
        var name by remember { mutableStateOf(zoneName) }
        var radius by remember { mutableStateOf(zoneRadius) }
        var isPreferred by remember { mutableStateOf(true) }
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Guardar Nueva Zona", color = Color(0xFFE5E7EB)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre de zona", color = Color(0xFFA1A1A7)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1F2937),
                            unfocusedContainerColor = Color(0xFF1F2937)
                        )
                    )
                    
                    TextField(
                        value = radius,
                        onValueChange = { radius = it },
                        label = { Text("Radio (km)", color = Color(0xFFA1A1A7)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1F2937),
                            unfocusedContainerColor = Color(0xFF1F2937)
                        )
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isPreferred,
                            onCheckedChange = { isPreferred = it }
                        )
                        Text(
                            "Zona Preferida (desmarcar para bloqueada)",
                            color = Color(0xFFE5E7EB),
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSave(name, radius.toDoubleOrNull() ?: 2.0, isPreferred)
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancelar")
                }
            },
            containerColor = Color(0xFF1F2937)
        )
    }
    
    private fun requestLocationPermissionAndGetCurrent() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            )
        }
    }
    
    private fun getCurrentLocation() {
        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                location?.let {
                    currentLatitude = it.latitude
                    currentLongitude = it.longitude
                }
            }
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error getting location: ${e.message}")
        }
    }
    
    companion object {
        private const val TAG = "SettingsActivity"
    }
}
