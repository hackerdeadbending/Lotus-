# Lotus backend foundation

This directory contains the first backend slice for Lotus contact discovery. It is source code only until it is deployed to a **dedicated Lotus Supabase project**. Do not connect it to the SoloPro project.

## Included

- `migrations/202610110001_lotus_contact_discovery.sql`: profile mirror from Supabase Auth, verified-phone gating, RLS-protected profile/contact tables, and a database-backed lookup throttle.
- `functions/lookup-contact/index.ts`: authenticated phone lookup; only returns a profile when the other account has also added the caller. Unknown and non-mutual numbers receive the same response shape.

## Required setup before this can work

1. Create a separate Supabase project for Lotus.
2. Enable Phone Auth and configure a real SMS provider in that project's Auth settings. Do not treat a phone number as verified until Supabase Auth confirms it.
3. Apply the migration to the Lotus project.
4. Deploy the `lookup-contact` Edge Function with JWT verification enabled.
5. Keep the service-role/secret key on the server only. Never add it to Android resources, source code, or build config.
6. Test with two verified test accounts: A looks up B, B looks up A, then both should see a mutual result. Also test unverified accounts, unknown numbers, malformed numbers, and the rate limit.

## Important limitations

- The Android app is not yet wired to this Edge Function.
- The contact picker is a user-selected number input; the server cannot prove that a number is saved in the phone's address book. The current mutual check means both accounts explicitly added each other's verified phone number in Lotus.
- This is not end-to-end encrypted messaging and does not make the current demo chats private.
- Before public launch, add abuse monitoring, retention/deletion flows, and privacy review. Rate limiting here is per account; it does not replace IP/device abuse controls.
