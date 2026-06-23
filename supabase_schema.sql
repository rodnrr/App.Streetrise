-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Create Profiles/Service Providers table
-- This table links directly to Supabase Auth Users for profile enrichment.
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT,
    company_name TEXT NOT NULL,
    description TEXT,
    phone TEXT,
    website TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Adjust row-level security for Profiles
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- Select policy: Anyone can see provider profiles
CREATE POLICY "Allow public read-access to profiles" 
    ON public.profiles FOR SELECT USING (true);

-- Insert/Update/Delete policy: Users can only change their own profile
CREATE POLICY "Allow users to manage their own profile" 
    ON public.profiles FOR ALL USING (auth.uid() = id);


-- 2. Create Support Services table
-- Stores services offered by different providers
CREATE TABLE IF NOT EXISTS public.support_services (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    description TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'Active' CHECK (status IN ('Active', 'Inactive', 'Full')),
    category TEXT,
    latitude DECIMAL(9,6),
    longitude DECIMAL(9,6),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Adjust row-level security for Support Services
ALTER TABLE public.support_services ENABLE ROW LEVEL SECURITY;

-- Select policy: Anyone can browse services
CREATE POLICY "Allow public read-access to support_services" 
    ON public.support_services FOR SELECT USING (true);

-- Insert/Update/Delete policy: Users can only manage their own services
CREATE POLICY "Allow providers to manage their own support_services" 
    ON public.support_services FOR ALL USING (auth.uid() = provider_id);


-- 3. Create Coordination Requests table
-- Stores client requests sent to providers
CREATE TABLE IF NOT EXISTS public.coordination_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    client_name TEXT NOT NULL,
    service_name TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'Pending' CHECK (status IN ('Pending', 'Approved', 'Declined')),
    contact_info TEXT,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL
);

-- Adjust row-level security for Coordination Requests
ALTER TABLE public.coordination_requests ENABLE ROW LEVEL SECURITY;

-- Select/Update/Delete policy: Providers can only view/manage incoming requests targeting them
CREATE POLICY "Allow providers to view and modify their own incoming requests" 
    ON public.coordination_requests FOR ALL USING (auth.uid() = provider_id);

-- Insert policy: Anyone can insert a request (e.g., clients, dispatchers, form-fillers)
CREATE POLICY "Allow public inserts into coordination_requests" 
    ON public.coordination_requests FOR INSERT WITH CHECK (true);


-- 4. Trigger to automatically provision a profile on signup
-- Highly useful in Supabase to link auth users seamlessly to public.profiles table.
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS trigger AS $$
BEGIN
  INSERT INTO public.profiles (id, email, company_name)
  VALUES (
    new.id, 
    new.email, 
    COALESCE(new.raw_user_meta_data->>'company_name', split_part(new.email, '@', 1))
  );
  RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Trigger execution rules
CREATE OR REPLACE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
