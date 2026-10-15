-- Additive direct-message foundation for Lotus. No existing data is removed.
create table if not exists public.lotus_conversations (
  id uuid primary key default gen_random_uuid(),
  participant_a uuid not null references auth.users(id) on delete cascade,
  participant_b uuid not null references auth.users(id) on delete cascade,
  created_at timestamptz not null default now(),
  last_message_at timestamptz,
  constraint lotus_conversations_distinct_participants check (participant_a <> participant_b),
  constraint lotus_conversations_sorted_participants check (participant_a < participant_b),
  constraint lotus_conversations_unique_pair unique (participant_a, participant_b)
);

create table if not exists public.lotus_messages (
  id uuid primary key default gen_random_uuid(),
  conversation_id uuid not null references public.lotus_conversations(id) on delete cascade,
  sender_id uuid not null references auth.users(id) on delete cascade,
  body text not null check (char_length(trim(body)) between 1 and 4000),
  created_at timestamptz not null default now()
);

create index if not exists lotus_messages_conversation_created_idx
  on public.lotus_messages (conversation_id, created_at desc);

alter table public.lotus_conversations enable row level security;
alter table public.lotus_messages enable row level security;

revoke all on public.lotus_conversations from anon, authenticated;
revoke all on public.lotus_messages from anon, authenticated;
grant select on public.lotus_conversations to authenticated;
grant select on public.lotus_messages to authenticated;

drop policy if exists "Members can read their Lotus conversations" on public.lotus_conversations;
create policy "Members can read their Lotus conversations"
  on public.lotus_conversations for select to authenticated
  using ((select auth.uid()) = participant_a or (select auth.uid()) = participant_b);

drop policy if exists "Conversation members can read messages" on public.lotus_messages;
create policy "Conversation members can read messages"
  on public.lotus_messages for select to authenticated
  using (
    exists (
      select 1 from public.lotus_conversations c
      where c.id = conversation_id
        and ((select auth.uid()) = c.participant_a or (select auth.uid()) = c.participant_b)
    )
  );

-- Writes are intentionally server-mediated by the authenticated lotus-messaging Edge Function.
-- Clients cannot create conversations or forge sender IDs through PostgREST.
