# ⚡ Setup Rápido - MovieRoulette

## 1️⃣ Configurar Supabase (5 minutos)

1. **Crear proyecto**: https://supabase.com → New Project
2. **Ejecutar SQL**: 
   - Ve a SQL Editor
   - Pega TODO el contenido de `supabase_schema.sql`
   - Click RUN
3. **Desactivar confirmación de email (para desarrollo)**:
   - Ve a Authentication → Providers → Email
   - Desactiva "Confirm email"
   - Esto permite registro directo sin confirmar email
4. **Obtener credenciales**:
   - Settings → API
   - Copia: `Project URL` y `anon public key`

## 2️⃣ Configurar TMDB (3 minutos)

1. **Crear cuenta**: https://www.themoviedb.org/signup
2. **Obtener API Key**:
   - Settings → API → Create
   - Type: Website
   - Copia tu API Key

## 3️⃣ Configurar Proyecto (2 minutos)

Edita `gradle.properties`:

```properties
SUPABASE_URL=https://tu-proyecto.supabase.co
SUPABASE_ANON_KEY=tu_clave_anon_aqui
TMDB_API_KEY=tu_api_key_tmdb_aqui
```

## 4️⃣ Ejecutar

```bash
# En Android Studio
File → Sync Project with Gradle Files
Click Run ▶️
```

## ✅ Listo!

Tu app está lista para usar. 

### Flujo de uso:
1. Registro → 2. Crear grupo → 3. Añadir películas → 4. Girar ruleta → 5. Valorar

---

### 🆘 ¿Problemas?

**Error de compilación**: File → Invalidate Caches → Restart

**Supabase no conecta**: Verifica que el SQL se ejecutó correctamente

**TMDB no funciona**: Verifica tu API Key en themoviedb.org

### 📚 Documentación completa

Lee `README.md` para instrucciones detalladas.

---

**¡Disfruta de Movie Roulette! 🎬🍿**
