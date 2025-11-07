-- Add image_url column to groups table
ALTER TABLE public.groups
ADD COLUMN IF NOT EXISTS image_url TEXT;

-- Note: profiles table already has avatar_url column
-- No changes needed for profiles table
