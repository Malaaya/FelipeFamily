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
- Recursos nativos: **notificaciones locales** y **selector de archivos**.
- Solo datos ficticios; la ubicación es una zona referencial ingresada manualmente.

## Arquitectura (MVVM)
- `data/` – Room: entidades, DAOs y base de datos.
- `util/` – validaciones, matching y recursos nativos.
- `viewmodel/` – estado y lógica de cada flujo.
- `ui/` – componentes, navegación y pantallas en Compose.

## Cómo ejecutar
1. Abrir el proyecto en Android Studio y esperar el Gradle Sync.
2. Ejecutar en un emulador o dispositivo Android (minSdk 24).
3. Cuentas demo (clave `123456`): `productor_demo01`, `prestador_demo01`, `admin_demo`.
4. Para probar la importación, copiar `datos_prueba/catalogo_ficticio.json` al dispositivo.
