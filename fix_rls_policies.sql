-- FIX: Corregir recursión infinita en políticas RLS
-- Ejecutar este script DESPUÉS de supabase_schema.sql

-- Primero, eliminar las políticas problemáticas
DROP POLICY IF EXISTS "Groups viewable by members" ON public.groups;
DROP POLICY IF EXISTS "Users can create groups" ON public.groups;
DROP POLICY IF EXISTS "Group admins can update groups" ON public.groups;
DROP POLICY IF EXISTS "Group members viewable by group members" ON public.group_members;
DROP POLICY IF EXISTS "Users can join groups" ON public.group_members;
DROP POLICY IF EXISTS "Users can leave groups" ON public.group_members;
DROP POLICY IF EXISTS "Movies viewable by group members" ON public.movies;
DROP POLICY IF EXISTS "Group members can add movies" ON public.movies;
DROP POLICY IF EXISTS "Users can delete movies they added" ON public.movies;
DROP POLICY IF EXISTS "Movie status viewable by group members" ON public.movie_status;
DROP POLICY IF EXISTS "Group members can insert movie status" ON public.movie_status;
DROP POLICY IF EXISTS "Group members can update movie status" ON public.movie_status;
DROP POLICY IF EXISTS "Group members can update status" ON public.movie_status;
DROP POLICY IF EXISTS "Movie ratings viewable by group members" ON public.movie_ratings;
DROP POLICY IF EXISTS "Group members can add ratings" ON public.movie_ratings;

-- Crear función auxiliar para verificar membresía sin recursión
CREATE OR REPLACE FUNCTION public.is_group_member(group_uuid UUID, user_uuid UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.group_members
        WHERE group_id = group_uuid AND user_id = user_uuid
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

-- Crear función para verificar si es admin
CREATE OR REPLACE FUNCTION public.is_group_admin(group_uuid UUID, user_uuid UUID)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1 FROM public.group_members
        WHERE group_id = group_uuid AND user_id = user_uuid AND role = 'admin'
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER STABLE;

-- POLÍTICAS CORREGIDAS PARA GROUPS
CREATE POLICY "Groups viewable by members or creator"
    ON public.groups FOR SELECT
    USING (
        public.is_group_member(id, auth.uid())
        OR created_by = auth.uid()
    );

CREATE POLICY "Users can create groups"
    ON public.groups FOR INSERT
    WITH CHECK (auth.uid() = created_by);

CREATE POLICY "Group admins can update groups"
    ON public.groups FOR UPDATE
    USING (public.is_group_admin(id, auth.uid()));

-- POLÍTICAS CORREGIDAS PARA GROUP_MEMBERS
CREATE POLICY "Group members viewable by group members"
    ON public.group_members FOR SELECT
    USING (public.is_group_member(group_id, auth.uid()));

CREATE POLICY "Users can join groups"
    ON public.group_members FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can leave groups"
    ON public.group_members FOR DELETE
    USING (auth.uid() = user_id);

-- POLÍTICAS CORREGIDAS PARA MOVIES
CREATE POLICY "Movies viewable by group members"
    ON public.movies FOR SELECT
    USING (public.is_group_member(group_id, auth.uid()));

CREATE POLICY "Group members can add movies"
    ON public.movies FOR INSERT
    WITH CHECK (
        public.is_group_member(group_id, auth.uid())
        AND auth.uid() = added_by
    );

CREATE POLICY "Users can delete movies they added"
    ON public.movies FOR DELETE
    USING (auth.uid() = added_by);

-- POLÍTICAS CORREGIDAS PARA MOVIE_STATUS
CREATE POLICY "Movie status viewable by group members"
    ON public.movie_status FOR SELECT
    USING (public.is_group_member(group_id, auth.uid()));

CREATE POLICY "Group members can insert movie status"
    ON public.movie_status FOR INSERT
    WITH CHECK (public.is_group_member(group_id, auth.uid()));

CREATE POLICY "Group members can update movie status"
    ON public.movie_status FOR UPDATE
    USING (public.is_group_member(group_id, auth.uid()));

-- POLÍTICAS CORREGIDAS PARA MOVIE_RATINGS
CREATE POLICY "Movie ratings viewable by group members"
    ON public.movie_ratings FOR SELECT
    USING (
        EXISTS (
            SELECT 1 FROM public.movies m
            WHERE m.id = movie_id
            AND public.is_group_member(m.group_id, auth.uid())
        )
    );

CREATE POLICY "Group members can add ratings"
    ON public.movie_ratings FOR INSERT
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM public.movies m
            WHERE m.id = movie_id
            AND public.is_group_member(m.group_id, auth.uid())
        )
        AND auth.uid() = user_id
    );

DROP FUNCTION IF EXISTS public.join_group_by_code(TEXT);

CREATE OR REPLACE FUNCTION public.join_group_by_code(p_invite_code TEXT)
RETURNS SETOF public.groups
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
    normalized_code TEXT := UPPER(TRIM(p_invite_code));
    group_row public.groups%ROWTYPE;
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Debe iniciar sesión para unirse a un grupo';
    END IF;

    SELECT g.* INTO group_row
    FROM public.groups AS g
    WHERE g.invite_code = normalized_code
    LIMIT 1;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Código de invitación no válido';
    END IF;

    IF public.is_group_member(group_row.id, auth.uid()) THEN
        RAISE EXCEPTION 'Ya eres miembro de este grupo';
    END IF;

    INSERT INTO public.group_members (group_id, user_id, role)
    VALUES (group_row.id, auth.uid(), 'member')
    ON CONFLICT DO NOTHING;

    RETURN NEXT group_row;
    RETURN;
END;
$$;

DROP FUNCTION IF EXISTS public.remove_group_member(UUID, UUID);

CREATE OR REPLACE FUNCTION public.remove_group_member(p_group_id UUID, p_target_user UUID)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
    IF auth.uid() IS NULL THEN
        RAISE EXCEPTION 'Debe iniciar sesión';
    END IF;

    IF NOT public.is_group_admin(p_group_id, auth.uid()) THEN
        RAISE EXCEPTION 'Solo los administradores pueden eliminar miembros';
    END IF;

    DELETE FROM public.group_members
    WHERE group_id = p_group_id AND user_id = p_target_user;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Miembro no encontrado en el grupo';
    END IF;
END;
$$;
