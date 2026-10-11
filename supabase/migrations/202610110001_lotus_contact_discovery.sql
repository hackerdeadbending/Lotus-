-- Lotus contact discovery foundation.
-- Phone verification is owned by Supabase Auth; clients cannot set phone_verified.
create schema if not exists lotus_private;
revoke all on schema lotus_private from public, anon, authenticated;

create table if not exists public.lotus_profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  phone_e164 text unique,
  phone_verified boolean not null default false,
  display_name text not null default 'Lotus user',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table if not exists public.lotus_contact_edges (
  owner_id uuid not null references auth.users(id) on delete cascade,
  contact_id uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (owner_id, contact_id),
  constraint lotus_contact_edges_no_self check (owner_id <> contact_id)
);

alter table public.lotus_profiles enable row level security;
alter table public.lotus_contact_edges enable row level security;

revoke all on public.lotus_profiles from anon, authenticated;
grant select on public.lotus_profiles to authenticated;
drop policy if exists "Users can read own Lotus profile" on public.lotus_profiles;
create policy "Users can read own Lotus profile"
  on public.lotus_profiles for select to authenticated
  using ((select auth.uid()) = user_id);

revoke all on public.lotus_contact_edges from anon, authenticated;
grant select on public.lotus_contact_edges to authenticated;
drop policy if exists "Users can read their own contact edges" on public.lotus_contact_edges;
create policy "Users can read their own contact edges"
  on public.lotus_contact_edges for select to authenticated
  using ((select auth.uid()) = owner_id);
-- Intentionally no client INSERT/UPDATE/DELETE policies. Only the authenticated
-- Edge Function using a server-only secret may create discovery edges.

create or replace function lotus_private.sync_lotus_profile_from_auth()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_verified boolean;
  v_name text;
begin
  v_verified := new.phone is not null and new.phone_confirmed_at is not null;
  v_name := coalesce(
    nullif(trim(new.raw_user_meta_data ->> 'full_name'), ''),
    nullif(trim(new.raw_user_meta_data ->> 'name'), ''),
    'Lotus user'
  );

  insert into public.lotus_profiles (user_id, phone_e164, phone_verified, display_name, updated_at)
  values (new.id, case when v_verified then new.phone else null end, v_verified, v_name, now())
  on conflict (user_id) do update
    set phone_e164 = excluded.phone_e164,
        phone_verified = excluded.phone_verified,
        display_name = excluded.display_name,
        updated_at = now();

  return new;
end;
$$;

revoke all on function lotus_private.sync_lotus_profile_from_auth() from public, anon, authenticated;

drop trigger if exists lotus_profile_sync_after_auth_write on auth.users;
create trigger lotus_profile_sync_after_auth_write
  after insert or update of phone, phone_confirmed_at, raw_user_meta_data
  on auth.users
  for each row execute function lotus_private.sync_lotus_profile_from_auth();

-- Backfill existing Auth users safely; only confirmed phone numbers are discoverable.
insert into public.lotus_profiles (user_id, phone_e164, phone_verified, display_name, updated_at)
select
  u.id,
  case when u.phone is not null and u.phone_confirmed_at is not null then u.phone else null end,
  (u.phone is not null and u.phone_confirmed_at is not null),
  coalesce(nullif(trim(u.raw_user_meta_data ->> 'full_name'), ''), nullif(trim(u.raw_user_meta_data ->> 'name'), ''), 'Lotus user'),
  now()
from auth.users u
on conflict (user_id) do update
set phone_e164 = excluded.phone_e164,
    phone_verified = excluded.phone_verified,
    display_name = excluded.display_name,
    updated_at = now();

create index if not exists lotus_contact_edges_contact_id_idx
  on public.lotus_contact_edges (contact_id, owner_id);

-- Durable per-user lookup throttling (10 attempts per rolling minute).
create table if not exists public.lotus_lookup_rate_limits (
  user_id uuid primary key references auth.users(id) on delete cascade,
  window_started_at timestamptz not null default now(),
  attempts integer not null default 0 check (attempts >= 0)
);
alter table public.lotus_lookup_rate_limits enable row level security;
revoke all on public.lotus_lookup_rate_limits from anon, authenticated;
revoke all on public.lotus_lookup_rate_limits from public;

create or replace function public.lotus_consume_contact_lookup_limit(p_user_id uuid)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_attempts integer;
begin
  insert into public.lotus_lookup_rate_limits (user_id, window_started_at, attempts)
  values (p_user_id, now(), 1)
  on conflict (user_id) do update
    set window_started_at = case
          when public.lotus_lookup_rate_limits.window_started_at < now() - interval '1 minute'
            then now()
          else public.lotus_lookup_rate_limits.window_started_at
        end,
        attempts = case
          when public.lotus_lookup_rate_limits.window_started_at < now() - interval '1 minute'
            then 1
          else public.lotus_lookup_rate_limits.attempts + 1
        end
  returning attempts into v_attempts;

  return v_attempts <= 10;
end;
$$;
revoke all on function public.lotus_consume_contact_lookup_limit(uuid) from public, anon, authenticated;
grant execute on function public.lotus_consume_contact_lookup_limit(uuid) to service_role;
