# FelipeFamily – MVP de vinculación agrícola (TECMUNDO)

App Android (Kotlin + Jetpack Compose) que conecta **productores** con **prestadores de servicios agrícolas**
(fumigación y aplicación con drones). Proyecto académico DSY1105 – Desarrollo de Aplicaciones Móviles, Duoc UC.

**Integrantes:** 
Nicolás Robles
Ángel Martín

## Funcionalidades
- Login, registro y recuperación de contraseña con validaciones por campo (íconos y mensajes). Sesión guardada en Room.
- **Productor:** registra una necesidad, ve prestadores compatibles (matching con % de compatibilidad), los compara y manifiesta interés (vinculación simulada).
- **Prestador:** mantiene su perfil de servicios y acepta o rechaza a los productores interesados.
- **Administrador:** ve usuarios ficticios y gestiona el catálogo de servicios; puede importar un catálogo desde un archivo JSON.
- Recursos nativos: **notificaciones locales** y **GPS** para detectar una zona referencial.
- Solo datos ficticios; la ubicación puede ingresarse manualmente o detectarse con el GPS.

## Integración de API

El proyecto consume 10 endpoints: nueve de los microservicios propios de TECMUNDO y uno de la API externa Open-Meteo.

| # | Método | Endpoint | Uso |
|---|---|---|---|
| 1 | `GET` | `/api/services` | Obtener el catálogo de servicios agrícolas. |
| 2 | `POST` | `/api/services` | Crear un servicio en el catálogo como administrador. |
| 3 | `GET` | `/api/needs/producer/{producerId}` | Listar las necesidades registradas por un productor. |
| 4 | `POST` | `/api/needs` | Registrar una nueva necesidad agrícola. |
| 5 | `GET` | `/api/providers` | Obtener las prestaciones activas para el motor de matching. |
| 6 | `POST` | `/api/providers` | Registrar o actualizar el perfil de servicio de un prestador. |
| 7 | `POST` | `/api/interests` | Enviar una manifestación de interés de un productor a un prestador. |
| 8 | `PUT` | `/api/interests/{id}/status` | Aceptar o rechazar una vinculación simulada. |
| 9 | `DELETE` | `/api/users/{id}` | Eliminar una cuenta de usuario desde el flujo de administrador. |
| 10 | `GET` | `https://api.open-meteo.com/v1/forecast?latitude={lat}&longitude={lon}&current_weather=true` | Consultar el clima actual y el viento para evaluar condiciones de vuelo de drones. |

Los microservicios propios usan como base `http://10.0.2.2:8080/` desde el emulador de Android Studio. La API externa usa Open-Meteo.

## Recursos nativos

- **Notificaciones locales:** solicitan permiso en Android 13 o superior y muestran avisos cuando se genera interés, se acepta una vinculación o se requiere informar al productor.
- **GPS:** solicita permisos de ubicación fina o aproximada y usa la última ubicación disponible por GPS o red para clasificar al usuario en una zona referencial (`Norte-1`, `Centro-1` o `Sur-1`).

## Arquitectura (MVVM)
- `data/` – Room: entidades, DAOs y base de datos.
- `repository/` – acceso a datos de autenticación, productor, prestador y administración; media entre Room y los ViewModels.
- `util/` – validaciones, matching y recursos nativos.
- `viewmodel/` – estado y lógica de cada flujo.
- `ui/` – componentes, navegación y pantallas en Compose.

## Cómo ejecutar
1. Abrir el proyecto en Android Studio y esperar el Gradle Sync.
2. Ejecutar en un emulador o dispositivo Android (minSdk 24).
3. Cuentas demo (clave `123456`): `productor_demo01`, `prestador_demo01`, `admin_demo`.
