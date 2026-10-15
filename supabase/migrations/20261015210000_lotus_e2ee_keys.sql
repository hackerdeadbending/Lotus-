-- Add per-install public keys for client-side ECDH and allow authenticated ciphertext payloads.
alter table public.lotus_profiles
  add column if not exists encryption_public_key text;

alter table public.lotus_messages
  drop constraint if exists lotus_messages_body_check;

alter table public.lotus_messages
  add constraint lotus_messages_body_check
  check (char_length(trim(body)) between 1 and 12000);

comment on column public.lotus_profiles.encryption_public_key is
  'Client-generated P-256 ECDH public key (base64 X.509 SubjectPublicKeyInfo); private key never leaves the device.';
