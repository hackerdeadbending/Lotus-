-- One-time, expiring invite codes allow email-authenticated Lotus users to start chats.
-- Only the Edge Function service role can read or mutate invite rows.
create table if not exists public.lotus_invites (
  id uuid primary key default gen_random_uuid(),
  creator_id uuid not null references auth.users(id) on delete cascade,
  token_hash text not null unique,
  created_at timestamptz not null default now(),
  expires_at timestamptz not null default (now() + interval '7 days'),
  accepted_by uuid references auth.users(id) on delete set null,
  accepted_at timestamptz,
  constraint lotus_invites_token_hash_length check (char_length(token_hash) = 64),
  constraint lotus_invites_not_self check (accepted_by is null or accepted_by <> creator_id)
);

create index if not exists lotus_invites_creator_created_idx
  on public.lotus_invites (creator_id, created_at desc);

alter table public.lotus_invites enable row level security;
revoke all on public.lotus_invites from public, anon, authenticated;
