alter table public.shorts_comments
    add column profile_client_id uuid references public.guest_profiles(client_id) on delete set null;
create index shorts_comments_profile_client_idx on public.shorts_comments(profile_client_id) where profile_client_id is not null;

drop policy if exists shorts_comments_insert on public.shorts_comments;
create policy shorts_comments_insert on public.shorts_comments for insert to anon, authenticated
    with check (client_id is not null and profile_client_id is null and char_length(trim(body)) between 1 and 500);

create table public.guest_stories (
    id uuid primary key default gen_random_uuid(),
    profile_client_id uuid not null references public.guest_profiles(client_id) on delete cascade,
    media_url text not null,
    media_type text not null check (media_type in ('image/jpeg', 'image/png', 'image/webp', 'image/gif')),
    caption text not null default '' check (char_length(caption) <= 240),
    created_at timestamptz not null default now(),
    expires_at timestamptz not null default (now() + interval '24 hours')
);
create index guest_stories_active_created_idx on public.guest_stories(expires_at, created_at desc);
create index guest_stories_profile_created_idx on public.guest_stories(profile_client_id, created_at desc);
alter table public.guest_stories enable row level security;
revoke all on table public.guest_stories from public, anon, authenticated;
grant select on table public.guest_stories to anon, authenticated;
create policy guest_stories_public_read on public.guest_stories
    for select to anon, authenticated using (expires_at > now());

notify pgrst, 'reload schema';
