"# 🎬 MovieRoulette - Guía de Configuración

Una aplicación Android moderna para elegir películas en grupo con un diseño limpio estilo Apple y modo oscuro.

## 📋 Características

- ✅ Autenticación con Supabase (registro/login)
- ✅ Sistema de grupos con códigos de invitación
- ✅ Búsqueda de películas con TMDB API
- ✅ Ruleta para elegir películas aleatoriamente
- ✅ Estados de películas: Pendiente, Viendo, Vista, Dropeada
- ✅ Sistema de valoraciones grupales
- ✅ Diseño oscuro minimalista estilo Apple
- ✅ Jetpack Compose + Material Design 3

## 🛠️ Tecnologías

- **Kotlin** - Lenguaje de programación
- **Jetpack Compose** - UI moderna declarativa
- **Supabase** - Backend (Auth, Database, Realtime)
- **TMDB API** - Base de datos de películas
- **Material Design 3** - Sistema de diseño
- **Coroutines & Flow** - Programación asíncrona
- **ViewModel** - Arquitectura MVVM
- **Navigation Compose** - Navegación

## 📦 Requisitos Previos

1. **Android Studio** - Flamingo o superior
2. **JDK 17** o superior
3. **Cuenta de Supabase** (gratuita)
4. **API Key de TMDB** (gratuita)

## 🚀 Configuración Paso a Paso

### 1. Configurar Supabase

#### 1.1 Crear Proyecto en Supabase

1. Ve a [https://supabase.com](https://supabase.com)
2. Crea una cuenta o inicia sesión
3. Clic en "New Project"
4. Completa:
   - **Name**: MovieRoulette
   - **Database Password**: [elige una contraseña segura]
   - **Region**: Elige la más cercana
5. Espera a que el proyecto se cree (2-3 minutos)

#### 1.2 Ejecutar el Schema SQL

1. En tu proyecto de Supabase, ve a **SQL Editor** (panel izquierdo)
2. Clic en "New Query"
3. Copia y pega TODO el contenido de `supabase_schema.sql`
4. Clic en **RUN** (o Ctrl+Enter)
5. Verifica que aparezca "Success. No rows returned"

#### 1.3 Obtener Credenciales

1. Ve a **Settings** > **API**
2. Copia:
   - **Project URL**: `https://xxxxx.supabase.co`
   - **anon/public key**: Una clave larga que empieza con `eyJ...`

### 2. Configurar TMDB API

#### 2.1 Crear Cuenta en TMDB

1. Ve a [https://www.themoviedb.org/signup](https://www.themoviedb.org/signup)
2. Crea una cuenta
3. Verifica tu email

#### 2.2 Obtener API Key

1. Ve a tu perfil > **Settings** > **API**
2. Clic en "Create" bajo API Key (v3 auth)
3. Completa el formulario:
   - **Type of Use**: Website
   - **Application Name**: MovieRoulette
   - **Application URL**: http://localhost (o tu dominio)
   - Acepta los términos
4. Copia tu **API Key** (es una cadena de 32 caracteres hexadecimales)

### 3. Configurar el Proyecto Android

#### 3.1 Clonar/Abrir el Proyecto

```bash
# Si tienes el proyecto en GitHub
git clone https://github.com/tuusuario/MovieRoulette.git
cd MovieRoulette

# Abre el proyecto en Android Studio
```

#### 3.2 Configurar gradle.properties

1. Abre el archivo `gradle.properties`
2. Reemplaza los valores de ejemplo con tus credenciales:

```properties
# Supabase Configuration
SUPABASE_URL=https://tu-proyecto-real.supabase.co
SUPABASE_ANON_KEY=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.tu-clave-real-aqui...

# TMDB API Configuration
TMDB_API_KEY=tu_api_key_de_32_caracteres_aqui
```

⚠️ **IMPORTANTE**: Nunca subas `gradle.properties` a GitHub con tus credenciales reales.

### 4. Sincronizar y Compilar

1. En Android Studio, clic en **File** > **Sync Project with Gradle Files**
2. Espera a que se descarguen las dependencias (puede tardar 5-10 minutos la primera vez)
3. Si hay errores, verifica que:
   - Tienes JDK 17 configurado
   - Tienes conexión a internet
   - Las credenciales en `gradle.properties` son correctas

### 5. Ejecutar la App

1. Conecta un dispositivo Android (API 26+) o inicia un emulador
2. Clic en el botón **Run** (▶️) o presiona Shift+F10
3. La app se instalará y abrirá automáticamente

## 📱 Estructura de la App

```
app/
├── data/
│   ├── model/          # Modelos de datos (User, Group, Movie)
│   ├── remote/         # APIs (Supabase, TMDB)
│   └── repository/     # Repositorios (Auth, Group, Movie)
├── ui/
│   ├── components/     # Componentes reutilizables
│   ├── navigation/     # Navegación
│   ├── screens/        # Pantallas
│   │   ├── auth/       # Login, Registro
│   │   ├── groups/     # Grupos
│   │   ├── movies/     # Películas
│   │   ├── roulette/   # Ruleta
│   │   └── rating/     # Valoraciones
│   └── theme/          # Tema oscuro Apple-style
└── viewmodel/          # ViewModels (MVVM)
```

## 🎯 Flujo de Uso

1. **Registro/Login** → Crea cuenta o inicia sesión
2. **Crear/Unirse a Grupo** → Crea un grupo o únete con código
3. **Añadir Películas** → Busca y agrega películas al grupo
4. **Girar la Ruleta** → Selecciona una película aleatoria
5. **Decidir Acción**:
   - **Verla** → Pasa a "Viendo"
   - **Más tarde** → Vuelve a la ruleta
   - **Eliminar** → Se quita de la lista
6. **Ver y Valorar** → Marca como vista y añade tu nota
7. **Ver Estadísticas** → Revisa notas de todos los miembros

## 🎨 Estados de Películas

- **pending** (🎲 En Ruleta) - Esperando ser elegida
- **watching** (👀 Viendo) - En progreso
- **watched** (✅ Vista) - Completada con valoraciones
- **dropped** (❌ Dropeada) - Abandonada
- **removed** (🗑️ Eliminada) - Eliminada del grupo

## 🐛 Solución de Problemas

### Error: "Unable to resolve dependency"

**Solución**: Verifica tu conexión a internet y sincroniza Gradle:
```bash
File > Invalidate Caches > Invalidate and Restart
```

### Error: "Supabase authentication failed"

**Solución**:
1. Verifica que `SUPABASE_URL` y `SUPABASE_ANON_KEY` sean correctos
2. Asegúrate de que el schema SQL se ejecutó correctamente
3. En Supabase, verifica que Authentication esté habilitado

### Error: "TMDB API error"

**Solución**:
1. Verifica que tu `TMDB_API_KEY` sea correcta
2. Asegúrate de que tu cuenta TMDB esté activada
3. Verifica que la API key no esté revocada

### La app no se conecta a Supabase

**Solución**:
1. Verifica que RLS (Row Level Security) esté habilitado
2. Revisa las policies en Supabase Dashboard
3. Mira los logs en Supabase Dashboard > Database > Logs

### No aparecen imágenes de películas

**Solución**:
1. Verifica permisos de INTERNET en AndroidManifest
2. Usa un dispositivo/emulador con conexión a internet
3. Revisa que Coil esté configurado correctamente

## 📝 Archivos Importantes

### `supabase_schema.sql`
Contiene TODO el schema de la base de datos:
- Tablas (profiles, groups, movies, etc.)
- Triggers automáticos
- Políticas de seguridad (RLS)
- Vistas útiles
- Funciones auxiliares

### `gradle.properties`
Contiene las configuraciones sensibles:
- URLs de Supabase
- API Keys
- **NO subir a GitHub**

### `build.gradle.kts`
Dependencias del proyecto:
- Supabase Kotlin SDK
- Retrofit + TMDB
- Jetpack Compose
- Coil para imágenes

## 🔒 Seguridad

- ✅ Row Level Security (RLS) habilitado en Supabase
- ✅ Políticas de acceso por usuario/grupo
- ✅ Autenticación JWT
- ✅ API Keys en BuildConfig (no en código)
- ✅ HTTPS para todas las conexiones

## 🆘 Soporte

Si tienes problemas:

1. Revisa la consola de Android Studio (Logcat)
2. Revisa los logs de Supabase Dashboard
3. Verifica que todas las credenciales sean correctas
4. Asegúrate de tener la versión mínima de Android (API 26)

## 📄 Licencia

Este proyecto es de código abierto y está disponible bajo la licencia MIT.

## 🎉 ¡Listo!

Ahora tienes una app completa para elegir películas en grupo. ¡Disfruta! 🍿

---

**Desarrollado con ❤️ usando Kotlin y Jetpack Compose**" 
