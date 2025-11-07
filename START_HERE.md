# ✅ MovieRoulette - Proyecto Configurado

## 🎉 ¡Tu proyecto está listo!

Tus credenciales ya están configuradas en `gradle.properties`:
- ✅ Supabase URL
- ✅ Supabase Anon Key  
- ✅ TMDB API Key

## 📋 Checklist Final

### 1. ✅ Ejecutar SQL en Supabase (IMPORTANTE)

**DEBES hacer esto primero antes de ejecutar la app:**

1. Ve a: https://supabase.com/dashboard/project/qaxnypaxddemznhpvppi
2. Click en **SQL Editor** (panel izquierdo)
3. Click en **New Query**
4. Copia y pega **TODO** el contenido del archivo `supabase_schema.sql`
5. Click en **RUN** (botón verde) o presiona `Ctrl + Enter`
6. Verifica que diga "Success. No rows returned"

⚠️ **Sin este paso, la app no funcionará** porque las tablas no existirán.

### 2. Abrir en Android Studio

```bash
# Abre Android Studio
File → Open → Selecciona la carpeta: C:\Github\MovieRoulette
```

### 3. Sincronizar Gradle

```bash
# Android Studio hará esto automáticamente
# O manualmente: File → Sync Project with Gradle Files
# Espera 5-10 minutos la primera vez
```

### 4. Ejecutar la App

```bash
# Click en el botón Run ▶️ (Shift + F10)
# Selecciona un emulador o dispositivo físico
# La app se instalará y abrirá
```

## 🎯 Primer Uso

1. **Pantalla de Login** aparecerá
2. Click en "Crear Cuenta"
3. Ingresa:
   - Nombre de usuario
   - Email
   - Contraseña
4. ¡Listo! Ya puedes crear grupos y añadir películas

## 🔍 URLs Útiles

- **Supabase Dashboard**: https://supabase.com/dashboard/project/qaxnypaxddemznhpvppi
- **TMDB**: https://www.themoviedb.org
- **Supabase Auth**: https://supabase.com/dashboard/project/qaxnypaxddemznhpvppi/auth/users
- **Supabase Database**: https://supabase.com/dashboard/project/qaxnypaxddemznhpvppi/database/tables
- **Supabase SQL Editor**: https://supabase.com/dashboard/project/qaxnypaxddemznhpvppi/sql

## 🐛 Si tienes problemas...

### La app no compila
```bash
File → Invalidate Caches → Invalidate and Restart
```

### Error: "Table 'profiles' does not exist"
**Solución**: No ejecutaste el SQL. Ve al paso 1.

### Error de autenticación
**Solución**: 
1. Ve a Supabase Dashboard → Authentication → Configuration
2. Verifica que Email Provider esté habilitado
3. Desactiva "Confirm email" si solo estás probando

### No aparecen películas en la búsqueda
**Solución**: Verifica tu conexión a internet en el dispositivo/emulador

## 📱 Funcionalidades

### ✅ Implementadas
- Registro y Login
- Crear grupos
- Unirse con código de invitación
- Buscar películas (TMDB)
- Añadir películas al grupo
- Ver listas (Pendientes, Viendo, Vistas)
- Girar ruleta
- Valorar películas (0-10 + comentario)
- Ver valoraciones de todos los miembros

### 🎨 Diseño
- Tema oscuro estilo Apple
- Animaciones suaves
- Material Design 3

## 🔐 Seguridad

⚠️ **IMPORTANTE**: El archivo `gradle.properties` contiene tus credenciales.
- Ya está en `.gitignore`
- NO lo compartas públicamente
- NO lo subas a GitHub

Si necesitas compartir el proyecto:
1. Crea un `gradle.properties.example` sin las credenciales reales
2. Documenta qué variables se necesitan

## 📊 Estructura de la Base de Datos

Tu base de datos tendrá estas tablas (después de ejecutar el SQL):

- **profiles** - Usuarios de la app
- **groups** - Grupos de amigos
- **group_members** - Relación usuarios-grupos
- **movies** - Películas añadidas
- **movie_status** - Estado de cada película (pending, watching, watched...)
- **movie_ratings** - Valoraciones individuales

## 🎬 Flujo Típico de Uso

```
1. Usuario se registra
   ↓
2. Crea un grupo "Familia" (obtiene código: ABC12345)
   ↓
3. Comparte código con amigos
   ↓
4. Amigos se unen con el código
   ↓
5. Todos añaden películas buscando en TMDB
   ↓
6. Giran la ruleta para elegir una
   ↓
7. Deciden: Ver ahora / Más tarde / Eliminar
   ↓
8. Ven la película
   ↓
9. Cada uno la valora (nota + comentario)
   ↓
10. Pueden ver el promedio grupal
```

## 🚀 Próximos Pasos (Opcional)

Si quieres mejorar la app:

1. **Notificaciones**: Cuando alguien añada una película
2. **Chat grupal**: Comentar sobre películas
3. **Filtros**: Por género, año, etc.
4. **Estadísticas**: Gráficos de valoraciones
5. **Exportar**: Lista de películas a PDF
6. **Streaming**: Links a Netflix, Prime, etc.

## 📞 Soporte

Si tienes dudas:
1. Revisa los logs en Android Studio (Logcat)
2. Revisa los logs en Supabase Dashboard
3. Lee `README.md` para más detalles

---

## ✨ ¡Todo listo para empezar!

**Recuerda**: Ejecuta el SQL en Supabase primero, luego abre el proyecto en Android Studio y ejecuta.

**¡Disfruta de Movie Roulette! 🍿🎬**
