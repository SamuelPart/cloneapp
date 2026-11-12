-- Public clients can read profile/social data, but all writes must go through the Edge Function.
revoke all on table public.guest_profiles from public, anon, authenticated;
grant select (client_id, display_name, username, avatar_url, cover_url, cover_animated, updated_at)
    on table public.guest_profiles to anon, authenticated;

revoke all on table public.shorts_reactions, public.shorts_comments, public.shorts_shares
    from public, anon, authenticated;
grant select on table public.shorts_reactions, public.shorts_comments, public.shorts_shares
    to anon, authenticated;

drop policy if exists shorts_reactions_insert on public.shorts_reactions;
drop policy if exists shorts_reactions_delete on public.shorts_reactions;
drop policy if exists shorts_comments_insert on public.shorts_comments;
drop policy if exists shorts_shares_insert on public.shorts_shares;

-- Anonymous installs use a random per-install token; only its one-way hash is stored server-side.
create table public.shorts_client_tokens (
    client_id uuid primary key,
    token_hash text not null check (token_hash ~ '^[0-9a-f]{64}$'),
    created_at timestamptz not null default now(),
    last_seen_at timestamptz not null default now(),
    expires_at timestamptz not null default (now() + interval '365 days')
);
alter table public.shorts_client_tokens enable row level security;
revoke all on table public.shorts_client_tokens from public, anon, authenticated;
grant all on table public.shorts_client_tokens to service_role;

-- Atomic per-install, per-action window counters for comments/reactions/shares and profile writes.
create table public.shorts_rate_limits (
    client_id uuid not null,
    action text not null check (action in ('reaction', 'share', 'anonymous_comment', 'profile_comment', 'profile')),
    window_started_at timestamptz not null,
    request_count integer not null check (request_count >= 1),
    primary key (client_id, action)
);
alter table public.shorts_rate_limits enable row level security;
revoke all on table public.shorts_rate_limits from public, anon, authenticated;
grant all on table public.shorts_rate_limits to service_role;

create function public.consume_shorts_rate_limit(
    p_client_id uuid,
    p_action text,
    p_limit integer,
    p_window_seconds integer
) returns boolean
language plpgsql
security definer
set search_path = ''
as $function$
declare
    v_now timestamptz := pg_catalog.clock_timestamp();
    v_allowed boolean;
begin
    if p_client_id is null or p_action not in ('reaction', 'share', 'anonymous_comment', 'profile_comment', 'profile')
       or p_limit < 1 or p_limit > 100 or p_window_seconds < 1 or p_window_seconds > 3600 then
        return false;
    end if;

    insert into public.shorts_rate_limits (client_id, action, window_started_at, request_count)
    values (p_client_id, p_action, v_now, 1)
    on conflict (client_id, action) do update
    set window_started_at = case
            when v_now >= public.shorts_rate_limits.window_started_at + (p_window_seconds * interval '1 second') then v_now
            else public.shorts_rate_limits.window_started_at
        end,
        request_count = case
            when v_now >= public.shorts_rate_limits.window_started_at + (p_window_seconds * interval '1 second') then 1
            else public.shorts_rate_limits.request_count + 1
        end
    returning request_count <= p_limit into v_allowed;

    return v_allowed;
end;
$function$;
revoke all on function public.consume_shorts_rate_limit(uuid, text, integer, integer) from public, anon, authenticated;
grant execute on function public.consume_shorts_rate_limit(uuid, text, integer, integer) to service_role;

notify pgrst, 'reload schema';
