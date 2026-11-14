create table public.guest_profiles (
    client_id uuid primary key,
    display_name text not null check (char_length(trim(display_name)) between 1 and 40),
    username text not null check (username ~ '^[a-z0-9_.]{3,20}$'),
    avatar_url text,
    cover_url text,
    cover_animated boolean not null default false,
    owner_token_hash text not null,
    updated_at timestamptz not null default now()
);

create unique index guest_profiles_username_ci_idx on public.guest_profiles (lower(username));
alter table public.guest_profiles enable row level security;
revoke all on table public.guest_profiles from public, anon, authenticated;
grant select on table public.guest_profiles to anon, authenticated;
create policy guest_profiles_public_read on public.guest_profiles
    for select to anon, authenticated using (true);
