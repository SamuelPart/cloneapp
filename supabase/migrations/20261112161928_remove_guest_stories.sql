-- Stories were removed from SamPlay; drop the table and its persisted records.
drop table if exists public.guest_stories;

notify pgrst, 'reload schema';
