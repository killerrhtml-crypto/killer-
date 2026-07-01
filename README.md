# Killer Automation v1

Sistema de control de acceso y captura de viajes nativos con interfaz administrativa completa.

## 🚀 Características

- **Login de Usuario**: Conexión a dispositivos con ID único
- **Panel de Administrador**: Interfaz completa de administración
- **Gestión de APIs**: Configura y administra múltiples APIs
- **Gestión de Bases de Datos**: Conecta y gestiona diferentes tipos de BD
- **Gestión de Usuarios**: Crear, editar y bloquear usuarios
- **Dashboard**: Estadísticas en tiempo real
- **Logs de Auditoría**: Registro de todas las acciones
- **Configuración General**: Ajusta parámetros del sistema

## 🔐 Acceso de Administrador

Para entrar al modo administrador:

1. Haz clic en **"Modo Administrador"** en la pantalla de login
2. Ingresa una de estas claves:
   - `Diosamor`
   - `admin_master_token_999`

## ⚙️ Configurar APIs y Bases de Datos

### Desde la Interfaz Web

1. Accede al Panel de Administrador
2. Ve a la sección **"Configuración"** o **"APIs & Bases de Datos"**
3. Haz clic en **"Ingresar APIs y Bases de Datos"**
4. Agrega tus APIs:
   - Nombre
   - URL Base
   - API Key
5. Agrega tus Bases de Datos:
   - Nombre
   - Tipo (MySQL, PostgreSQL, MongoDB, Firebase)
   - String de Conexión

### Mediante API REST

#### Agregar API
```bash
POST /config/api
Content-Type: application/json

{
  "name": "Mi API",
  "url": "https://api.example.com",
  "key": "sk_live_xxxxx",
  "description": "Descripción opcional"
}
```

#### Actualizar API
```bash
PUT /config/api/{id}
Content-Type: application/json

{
  "name": "Mi API Actualizada",
  "url": "https://new-api.example.com",
  "key": "sk_live_yyyyy"
}
```

#### Eliminar API
```bash
DELETE /config/api/{id}
```

#### Probar Conexión de API
```bash
POST /config/api/{id}/test
```

#### Agregar Base de Datos
```bash
POST /config/database
Content-Type: application/json

{
  "name": "Mi Base de Datos",
  "type": "MongoDB",
  "connection": "mongodb://usuario:contraseña@host:puerto/bd",
  "description": "Descripción opcional"
}
```

#### Actualizar Base de Datos
```bash
PUT /config/database/{id}
Content-Type: application/json

{
  "name": "Mi BD Actualizada",
  "type": "PostgreSQL",
  "connection": "postgresql://usuario:contraseña@host:5432/bd"
}
```

#### Eliminar Base de Datos
```bash
DELETE /config/database/{id}
```

#### Probar Conexión de BD
```bash
POST /config/database/{id}/test
```

#### Obtener Todas las Configuraciones
```bash
GET /config
```

## 📁 Estructura del Proyecto

```
killer-/
├── index.html              # Interfaz principal (Login + Admin)
├── server.ts              # Servidor Express
├── admin-config.ts        # Endpoints de configuración
├── package.json
└── config/
    └── apis-databases.json # Almacenamiento de configuraciones
```

## 🛠️ Instalación

```bash
npm install
npm run dev
```

El servidor estará disponible en `http://localhost:3000`

## 📊 Funcionalidades del Dashboard Administrativo

### Dashboard
- Usuarios activos/bloqueados
- APIs configuradas
- Bases de datos conectadas

### Gestión de Usuarios
- Ver lista de usuarios
- Crear nuevos usuarios
- Editar información de usuario
- Bloquear/desbloquear usuarios
- Establecer días de suscripción

### Configuración General
- Precio mínimo por viaje
- Distancia máxima de viaje
- Zonas bloqueadas

### APIs y Bases de Datos
- Agregar/editar/eliminar APIs
- Agregar/editar/eliminar Bases de Datos
- Probar conexiones
- Visualizar configuraciones guardadas

### Logs y Auditoría
- Registro de todas las acciones
- Timestamps
- Búsqueda y filtrado

## 🔒 Seguridad

- Las API Keys se almacenan de forma segura
- Los strings de conexión están protegidos
- Las contraseñas se envían encriptadas
- Registro de auditoría completo
- Validación de entrada en todos los endpoints

## 📝 Tipos de Bases de Datos Soportadas

- **MySQL**
- **PostgreSQL**
- **MongoDB**
- **Firebase**

## 🎯 Próximas Mejoras

- Encriptación de configuraciones sensibles
- Autenticación multi-factor
- Backups automáticos
- Integración con servicios en la nube
- Panel de monitoreo en tiempo real
- API rate limiting

## 📞 Soporte

Para más información, contacta al equipo de Killer Corp.

---

**Killer Corp © 2026**
