-- MovieRoulette Database Schema for Supabase
-- Ejecutar este script en el SQL Editor de Supabase

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =============================================
-- TABLA: profiles (extiende auth.users de Supabase)
-- =============================================
CREATE TABLE public.profiles (
    id UUID REFERENCES auth.users(id) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    display_name VARCHAR(100),
    avatar_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- =============================================
-- TABLA: groups (grupos de amigos)
-- =============================================
CREATE TABLE public.groups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) NOT NULL,
    invite_code VARCHAR(8) UNIQUE NOT NULL,
    created_by UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Índice para búsquedas rápidas por código de invitación
CREATE INDEX idx_groups_invite_code ON public.groups(invite_code);

-- =============================================
-- TABLA: group_members (miembros de grupos)
-- =============================================
CREATE TABLE public.group_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID REFERENCES public.groups(id) ON DELETE CASCADE,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    role VARCHAR(20) DEFAULT 'member', -- 'admin' o 'member'
    joined_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(group_id, user_id)
);

-- Índices para consultas frecuentes
CREATE INDEX idx_group_members_group ON public.group_members(group_id);
CREATE INDEX idx_group_members_user ON public.group_members(user_id);

-- =============================================
-- TABLA: movies (películas del grupo)
-- =============================================
CREATE TABLE public.movies (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID REFERENCES public.groups(id) ON DELETE CASCADE,
    tmdb_id INTEGER NOT NULL, -- ID de TMDB
    title VARCHAR(255) NOT NULL,
    original_title VARCHAR(255),
    overview TEXT,
    poster_path TEXT,
    backdrop_path TEXT,
    release_date DATE,
    runtime INTEGER, -- en minutos
    genres JSONB, -- array de géneros
    added_by UUID REFERENCES public.profiles(id) ON DELETE SET NULL,
    added_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(group_id, tmdb_id)
);

-- Índices
CREATE INDEX idx_movies_group ON public.movies(group_id);
CREATE INDEX idx_movies_tmdb ON public.movies(tmdb_id);

-- =============================================
-- TABLA: movie_status (estado de películas por grupo)
-- =============================================
CREATE TABLE public.movie_status (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    movie_id UUID REFERENCES public.movies(id) ON DELETE CASCADE,
    group_id UUID REFERENCES public.groups(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'pending', -- 'pending', 'watching', 'watched', 'dropped', 'removed'
    started_watching_at TIMESTAMP WITH TIME ZONE,
    finished_watching_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(movie_id, group_id)
);

-- Índices
CREATE INDEX idx_movie_status_movie ON public.movie_status(movie_id);
CREATE INDEX idx_movie_status_group ON public.movie_status(group_id);
CREATE INDEX idx_movie_status_status ON public.movie_status(status);

-- =============================================
-- TABLA: movie_ratings (valoraciones individuales)
-- =============================================
CREATE TABLE public.movie_ratings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    movie_id UUID REFERENCES public.movies(id) ON DELETE CASCADE,
    user_id UUID REFERENCES public.profiles(id) ON DELETE CASCADE,
    rating DECIMAL(3,1) CHECK (rating >= 0 AND rating <= 10), -- Nota de 0 a 10
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    UNIQUE(movie_id, user_id)
);

-- Índices
CREATE INDEX idx_movie_ratings_movie ON public.movie_ratings(movie_id);
CREATE INDEX idx_movie_ratings_user ON public.movie_ratings(user_id);

-- =============================================
-- FUNCIONES Y TRIGGERS
-- =============================================

-- Función para generar código de invitación único
CREATE OR REPLACE FUNCTION generate_invite_code()
RETURNS TEXT AS $$
DECLARE
    chars TEXT := 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    result TEXT := '';
    i INTEGER;
BEGIN
    FOR i IN 1..8 LOOP
        result := result || substr(chars, floor(random() * length(chars) + 1)::int, 1);
    END LOOP;
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- Trigger para actualizar updated_at automáticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Aplicar trigger a las tablas
CREATE TRIGGER update_profiles_updated_at BEFORE UPDATE ON public.profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_groups_updated_at BEFORE UPDATE ON public.groups
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_movie_status_updated_at BEFORE UPDATE ON public.movie_status
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_movie_ratings_updated_at BEFORE UPDATE ON public.movie_ratings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Trigger para crear perfil automáticamente al registrarse
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, username, display_name)
    VALUES (
        NEW.id,
        COALESCE(NEW.raw_user_meta_data->>'username', split_part(NEW.email, '@', 1)),
        COALESCE(NEW.raw_user_meta_data->>'display_name', split_part(NEW.email, '@', 1))
    );
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- =============================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- =============================================

-- Habilitar RLS en todas las tablas
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.groups ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.group_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.movies ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.movie_status ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.movie_ratings ENABLE ROW LEVEL SECURITY;

-- PROFILES: Los usuarios pueden ver todos los perfiles, pero solo editar el suyo
CREATE POLICY "Profiles are viewable by everyone"
    ON public.profiles FOR SELECT
    USING (true);

CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE
    USING (auth.uid() = id);

-- GROUPS: Solo miembros pueden ver los grupos
CREATE POLICY "Groups viewable by members"
    ON public.groups FOR SELECT
    USING (
        id IN (
            SELECT group_id FROM public.group_members
            WHERE user_id = auth.uid()
        )
    );

CREATE POLICY "Users can create groups"
    ON public.groups FOR INSERT
    WITH CHECK (auth.uid() = created_by);

CREATE POLICY "Group admins can update groups"
    ON public.groups FOR UPDATE
    USING (
        id IN (
            SELECT group_id FROM public.group_members
            WHERE user_id = auth.uid() AND role = 'admin'
        )
    );

-- GROUP_MEMBERS: Los miembros pueden ver otros miembros del mismo grupo
CREATE POLICY "Group members viewable by group members"
    ON public.group_members FOR SELECT
    USING (
        group_id IN (
            SELECT group_id FROM public.group_members
            WHERE user_id = auth.uid()
        )
    );

CREATE POLICY "Users can join groups"
    ON public.group_members FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can leave groups"
    ON public.group_members FOR DELETE
    USING (auth.uid() = user_id);

-- MOVIES: Los miembros del grupo pueden ver y añadir películas
CREATE POLICY "Movies viewable by group members"
    ON public.movies FOR SELECT
    USING (
        group_id IN (
            SELECT group_id FROM public.group_members
            WHERE user_id = auth.uid()
        )
    );

CREATE POLICY "Group members can add movies"
    ON public.movies FOR INSERT
    WITH CHECK (
        group_id IN (
            SELECT group_id FROM public.group_members
            WHERE user_id = auth.uid()
        )
        AND auth.uid() = added_by
    );

CREATE POLICY "Users can delete movies they added"
    ON public.movies FOR DELETE
    USING (auth.uid() = added_by);

-- MOVIE_STATUS: Los miembros del grupo pueden ver y actualizar estados
CREATE POLICY "Movie status viewable by group members"
    ON public.movie_status FOR SELECT
    USING (
        group_id IN (
            SELECT group_id FROM public.group_members
            WHERE user_id = auth.uid()
        )
    );

CREATE POLICY "Group members can update movie status"
    ON public.movie_status FOR ALL
    USING (
        group_id IN (
            SELECT group_id FROM public.group_members
            WHERE user_id = auth.uid()
        )
    );

-- MOVIE_RATINGS: Los usuarios pueden ver todas las valoraciones de películas de su grupo
CREATE POLICY "Ratings viewable by group members"
    ON public.movie_ratings FOR SELECT
    USING (
        movie_id IN (
            SELECT m.id FROM public.movies m
            INNER JOIN public.group_members gm ON m.group_id = gm.group_id
            WHERE gm.user_id = auth.uid()
        )
    );

CREATE POLICY "Users can add/update own ratings"
    ON public.movie_ratings FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- =============================================
-- VISTAS ÚTILES
-- =============================================

-- Vista para obtener películas con su promedio de valoraciones
CREATE OR REPLACE VIEW public.movies_with_ratings AS
SELECT 
    m.*,
    COUNT(mr.id) as total_ratings,
    ROUND(AVG(mr.rating)::numeric, 1) as average_rating,
    ms.status
FROM public.movies m
LEFT JOIN public.movie_ratings mr ON m.id = mr.movie_id
LEFT JOIN public.movie_status ms ON m.id = ms.movie_id
GROUP BY m.id, ms.status;

-- Vista para obtener miembros de grupos con información del usuario
CREATE OR REPLACE VIEW public.group_members_with_profiles AS
SELECT 
    gm.*,
    p.username,
    p.display_name,
    p.avatar_url
FROM public.group_members gm
INNER JOIN public.profiles p ON gm.user_id = p.id;

-- =============================================
-- DATOS DE EJEMPLO (opcional)
-- =============================================

-- Puedes añadir datos de prueba aquí si lo necesitas
-- INSERT INTO public.profiles (id, username, display_name) VALUES ...

-- =============================================
-- NOTAS IMPORTANTES
-- =============================================

-- 1. Asegúrate de tener activada la autenticación en Supabase
-- 2. Configura las variables de entorno en tu app Android:
--    - SUPABASE_URL
--    - SUPABASE_ANON_KEY
-- 3. Para TMDB API necesitarás una API key gratuita de https://www.themoviedb.org/settings/api
-- 4. Este schema soporta múltiples grupos por usuario
-- 5. Los códigos de invitación son únicos y de 8 caracteres

