create table public.shorts_reactions (
    clip_id text not null check (char_length(trim(clip_id)) between 1 and 2048),
    client_id uuid not null,
    created_at timestamptz not null default now(),
    primary key (clip_id, client_id)
);

create table public.shorts_comments (
    id uuid primary key default gen_random_uuid(),
    clip_id text not null check (char_length(trim(clip_id)) between 1 and 2048),
    client_id uuid not null,
    body text not null check (char_length(trim(body)) between 1 and 500),
    created_at timestamptz not null default now()
);

create table public.shorts_shares (
    id uuid primary key default gen_random_uuid(),
    clip_id text not null check (char_length(trim(clip_id)) between 1 and 2048),
    client_id uuid not null,
    created_at timestamptz not null default now()
);

create index shorts_comments_clip_created_idx on public.shorts_comments (clip_id, created_at desc);
create index shorts_shares_clip_idx on public.shorts_shares (clip_id);

alter table public.shorts_reactions enable row level security;
alter table public.shorts_comments enable row level security;
alter table public.shorts_shares enable row level security;

grant select, insert, delete on public.shorts_reactions to anon, authenticated;
grant select, insert on public.shorts_comments to anon, authenticated;
grant select, insert on public.shorts_shares to anon, authenticated;

drop policy if exists shorts_reactions_read on public.shorts_reactions;
create policy shorts_reactions_read on public.shorts_reactions for select to anon, authenticated using (true);
drop policy if exists shorts_reactions_insert on public.shorts_reactions;
create policy shorts_reactions_insert on public.shorts_reactions for insert to anon, authenticated with check (client_id is not null);
drop policy if exists shorts_reactions_delete on public.shorts_reactions;
create policy shorts_reactions_delete on public.shorts_reactions for delete to anon, authenticated using (client_id is not null);

drop policy if exists shorts_comments_read on public.shorts_comments;
create policy shorts_comments_read on public.shorts_comments for select to anon, authenticated using (true);
drop policy if exists shorts_comments_insert on public.shorts_comments;
create policy shorts_comments_insert on public.shorts_comments for insert to anon, authenticated with check (client_id is not null and char_length(trim(body)) between 1 and 500);

drop policy if exists shorts_shares_read on public.shorts_shares;
create policy shorts_shares_read on public.shorts_shares for select to anon, authenticated using (true);
drop policy if exists shorts_shares_insert on public.shorts_shares;
create policy shorts_shares_insert on public.shorts_shares for insert to anon, authenticated with check (client_id is not null);
