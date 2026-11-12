-- Keep the API roles limited to the operations the Shorts UI needs.
-- client_id is a guest-install pseudonym, not a verified Supabase account.
revoke all on table public.shorts_reactions, public.shorts_comments, public.shorts_shares from public, anon, authenticated;

grant select, insert, delete on table public.shorts_reactions to anon, authenticated;
grant select, insert on table public.shorts_comments to anon, authenticated;
grant select, insert on table public.shorts_shares to anon, authenticated;
