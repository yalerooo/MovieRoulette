-- Create Storage Buckets for MovieRoulette App
-- Run this in Supabase SQL Editor

-- Create bucket for profile pictures
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'profile-pictures',
    'profile-pictures',
    true,
    5242880, -- 5MB limit
    ARRAY['image/jpeg', 'image/jpg', 'image/png', 'image/webp']
)
ON CONFLICT (id) DO NOTHING;

-- Create bucket for group pictures
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'group-pictures',
    'group-pictures',
    true,
    5242880, -- 5MB limit
    ARRAY['image/jpeg', 'image/jpg', 'image/png', 'image/webp']
)
ON CONFLICT (id) DO NOTHING;

-- RLS Policy for profile pictures - anyone authenticated can upload
CREATE POLICY "Authenticated users can upload profile pictures"
ON storage.objects
FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'profile-pictures'
);

-- RLS Policy for profile pictures - users can update their own pictures
CREATE POLICY "Users can update their own profile pictures"
ON storage.objects
FOR UPDATE
TO authenticated
USING (
    bucket_id = 'profile-pictures'
)
WITH CHECK (
    bucket_id = 'profile-pictures'
);

-- RLS Policy for profile pictures - anyone can read
CREATE POLICY "Anyone can view profile pictures"
ON storage.objects
FOR SELECT
TO public
USING (
    bucket_id = 'profile-pictures'
);

-- RLS Policy for profile pictures - users can delete their own pictures
CREATE POLICY "Users can delete their own profile pictures"
ON storage.objects
FOR DELETE
TO authenticated
USING (
    bucket_id = 'profile-pictures'
);

-- RLS Policy for group pictures - group members can upload
CREATE POLICY "Authenticated users can upload group pictures"
ON storage.objects
FOR INSERT
TO authenticated
WITH CHECK (
    bucket_id = 'group-pictures'
);

-- RLS Policy for group pictures - group admins can update
CREATE POLICY "Users can update group pictures"
ON storage.objects
FOR UPDATE
TO authenticated
USING (
    bucket_id = 'group-pictures'
)
WITH CHECK (
    bucket_id = 'group-pictures'
);

-- RLS Policy for group pictures - anyone can read
CREATE POLICY "Anyone can view group pictures"
ON storage.objects
FOR SELECT
TO public
USING (
    bucket_id = 'group-pictures'
);

-- RLS Policy for group pictures - group admins can delete
CREATE POLICY "Users can delete group pictures"
ON storage.objects
FOR DELETE
TO authenticated
USING (
    bucket_id = 'group-pictures'
);
