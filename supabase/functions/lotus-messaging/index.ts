import { createClient } from "npm:@supabase/supabase-js@2";

const headers = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};

function reply(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers });
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers });
  if (req.method !== "POST") return reply({ error: "method_not_allowed" }, 405);

  const url = Deno.env.get("SUPABASE_URL");
  const publishableKey = Deno.env.get("SUPABASE_ANON_KEY") ?? Deno.env.get("SUPABASE_PUBLISHABLE_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? Deno.env.get("SUPABASE_SECRET_KEY");
  if (!url || !publishableKey || !serviceKey) return reply({ error: "server_not_configured" }, 503);

  const authorization = req.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) return reply({ error: "unauthorized" }, 401);
  const token = authorization.slice("Bearer ".length);

  try {
    const auth = createClient(url, publishableKey, { auth: { persistSession: false, autoRefreshToken: false } });
    const { data: authData, error: authError } = await auth.auth.getUser(token);
    const user = authData.user;
    if (authError || !user) return reply({ error: "unauthorized" }, 401);

    // Email-authenticated accounts may use existing conversations. Starting a new chat
    // still requires reciprocal contact edges below, and contact discovery separately
    // requires a verified phone number in lookup-contact.

    let body: { action?: unknown; contactId?: unknown; conversationId?: unknown; text?: unknown; inviteCode?: unknown; displayName?: unknown; encryptionPublicKey?: unknown };
    try {
      body = await req.json();
    } catch {
      return reply({ error: "invalid_json" }, 400);
    }

    const admin = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });
    const action = body.action;

    if (action === "publish_encryption_key") {
      const publicKey = typeof body.encryptionPublicKey === "string" ? body.encryptionPublicKey : "";
      if (!/^[A-Za-z0-9+/]{80,200}={0,2}$/.test(publicKey)) {
        return reply({ error: "encryption_public_key_invalid" }, 400);
      }
      const { data: currentProfile, error: profileError } = await admin.from("lotus_profiles")
        .select("encryption_public_key")
        .eq("user_id", user.id)
        .maybeSingle();
      if (profileError || !currentProfile) return reply({ error: "profile_unavailable" }, 503);
      if (currentProfile.encryption_public_key && currentProfile.encryption_public_key !== publicKey) {
        return reply({ error: "encryption_key_already_registered" }, 409);
      }
      const { error } = await admin.from("lotus_profiles")
        .update({ encryption_public_key: publicKey, updated_at: new Date().toISOString() })
        .eq("user_id", user.id);
      if (error) return reply({ error: "encryption_key_publish_failed" }, 503);
      return reply({ ok: true });
    }

    if (action === "get_profile") {
      const { data: profile, error } = await admin.from("lotus_profiles")
        .select("display_name, encryption_public_key")
        .eq("user_id", user.id)
        .maybeSingle();
      if (error) return reply({ error: "profile_unavailable" }, 503);
      return reply({
        displayName: profile?.display_name ?? "Lotus user",
        encryptionPublicKey: profile?.encryption_public_key ?? null,
      });
    }

    if (action === "update_profile") {
      const displayName = typeof body.displayName === "string" ? body.displayName.trim().replace(/\s+/g, " ") : "";
      if (displayName.length < 2 || displayName.length > 40) {
        return reply({ error: "display_name_length_invalid" }, 400);
      }
      const { data: profile, error } = await admin.from("lotus_profiles")
        .update({ display_name: displayName, updated_at: new Date().toISOString() })
        .eq("user_id", user.id)
        .select("display_name")
        .single();
      if (error || !profile) return reply({ error: "profile_update_failed" }, 503);
      return reply({ displayName: profile.display_name });
    }

    if (action === "create_invite") {
      const { count, error: countError } = await admin.from("lotus_invites")
        .select("id", { count: "exact", head: true })
        .eq("creator_id", user.id)
        .gt("created_at", new Date(Date.now() - 60 * 60 * 1000).toISOString());
      if (countError) return reply({ error: "invite_create_failed" }, 503);
      if ((count ?? 0) >= 10) return reply({ error: "invite_rate_limited" }, 429);

      const random = new Uint8Array(12);
      crypto.getRandomValues(random);
      const inviteCode = Array.from(random, (value) => value.toString(16).padStart(2, "0")).join("").toUpperCase();
      const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(inviteCode));
      const tokenHash = Array.from(new Uint8Array(digest), (value) => value.toString(16).padStart(2, "0")).join("");
      const { error } = await admin.from("lotus_invites").insert({
        creator_id: user.id,
        token_hash: tokenHash,
        expires_at: new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString(),
      });
      if (error) return reply({ error: "invite_create_failed" }, 503);
      return reply({ inviteCode, expiresInDays: 7 });
    }

    if (action === "accept_invite") {
      const inviteCode = typeof body.inviteCode === "string" ? body.inviteCode.trim().toUpperCase() : "";
      if (!/^[A-F0-9]{24}$/.test(inviteCode)) return reply({ error: "invite_code_invalid" }, 400);
      const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(inviteCode));
      const tokenHash = Array.from(new Uint8Array(digest), (value) => value.toString(16).padStart(2, "0")).join("");
      const { data: invite } = await admin.from("lotus_invites")
        .select("id, creator_id")
        .eq("token_hash", tokenHash)
        .is("accepted_by", null)
        .gt("expires_at", new Date().toISOString())
        .maybeSingle();
      if (!invite) return reply({ error: "invite_invalid_or_expired" }, 404);
      if (invite.creator_id === user.id) return reply({ error: "invite_cannot_accept_own" }, 400);

      const { data: claimed, error: claimError } = await admin.from("lotus_invites")
        .update({ accepted_by: user.id, accepted_at: new Date().toISOString() })
        .eq("id", invite.id)
        .is("accepted_by", null)
        .gt("expires_at", new Date().toISOString())
        .select("id")
        .maybeSingle();
      if (claimError || !claimed) return reply({ error: "invite_invalid_or_expired" }, 409);

      const [participantA, participantB] = [invite.creator_id, user.id].sort();
      const { data: conversation, error: conversationError } = await admin.from("lotus_conversations")
        .upsert({ participant_a: participantA, participant_b: participantB }, { onConflict: "participant_a,participant_b" })
        .select("id, participant_a, participant_b, created_at, last_message_at").single();
      if (conversationError || !conversation) return reply({ error: "chat_create_failed" }, 503);

      const { data: profile } = await admin.from("lotus_profiles")
        .select("display_name, encryption_public_key")
        .eq("user_id", invite.creator_id)
        .maybeSingle();
      return reply({
        chat: {
          id: conversation.id,
          peerId: invite.creator_id,
          displayName: profile?.display_name ?? "Lotus contact",
          peerPublicKey: profile?.encryption_public_key ?? null,
          lastMessage: "",
          lastMessageAt: conversation.created_at,
        },
      });
    }

    if (action === "list_chats") {
      const { data: conversations, error } = await admin
        .from("lotus_conversations")
        .select("id, participant_a, participant_b, created_at, last_message_at")
        .or(`participant_a.eq.${user.id},participant_b.eq.${user.id}`)
        .order("last_message_at", { ascending: false, nullsFirst: false });

      if (error) return reply({ error: "chat_list_unavailable" }, 503);
      const chats = [];
      for (const conversation of conversations ?? []) {
        const peerId = conversation.participant_a === user.id ? conversation.participant_b : conversation.participant_a;
        const [{ data: profile }, { data: lastMessage }] = await Promise.all([
          admin.from("lotus_profiles").select("display_name, encryption_public_key").eq("user_id", peerId).maybeSingle(),
          admin.from("lotus_messages").select("body, created_at").eq("conversation_id", conversation.id)
            .order("created_at", { ascending: false }).limit(1).maybeSingle(),
        ]);
        chats.push({
          id: conversation.id,
          peerId,
          displayName: profile?.display_name ?? "Lotus contact",
          peerPublicKey: profile?.encryption_public_key ?? null,
          lastMessage: lastMessage?.body ?? "",
          lastMessageAt: lastMessage?.created_at ?? conversation.created_at,
        });
      }
      return reply({ chats });
    }

    if (action === "start_chat") {
      const contactId = typeof body.contactId === "string" ? body.contactId : "";
      if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(contactId)) {
        return reply({ error: "invalid_contact" }, 400);
      }
      if (contactId === user.id) return reply({ error: "cannot_chat_with_self" }, 400);

      const { data: target } = await admin.from("lotus_profiles").select("user_id, phone_verified, display_name, encryption_public_key")
        .eq("user_id", contactId).eq("phone_verified", true).maybeSingle();
      if (!target) return reply({ error: "contact_unavailable" }, 404);

      const [{ data: forward }, { data: reverse }] = await Promise.all([
        admin.from("lotus_contact_edges").select("owner_id").eq("owner_id", user.id).eq("contact_id", contactId).maybeSingle(),
        admin.from("lotus_contact_edges").select("owner_id").eq("owner_id", contactId).eq("contact_id", user.id).maybeSingle(),
      ]);
      if (!forward || !reverse) return reply({ error: "mutual_contact_required" }, 403);

      const [participantA, participantB] = [user.id, contactId].sort();
      const { data: conversation, error } = await admin.from("lotus_conversations")
        .upsert({ participant_a: participantA, participant_b: participantB }, { onConflict: "participant_a,participant_b" })
        .select("id, participant_a, participant_b, created_at, last_message_at").single();
      if (error || !conversation) return reply({ error: "chat_create_failed" }, 503);
      return reply({
        chat: {
          id: conversation.id,
          peerId: contactId,
          displayName: target.display_name ?? "Lotus contact",
          peerPublicKey: target.encryption_public_key ?? null,
          lastMessage: "",
          lastMessageAt: conversation.created_at,
        },
      });
    }

    const conversationId = typeof body.conversationId === "string" ? body.conversationId : "";
    if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(conversationId)) {
      return reply({ error: "invalid_conversation" }, 400);
    }

    const { data: conversation } = await admin.from("lotus_conversations")
      .select("id, participant_a, participant_b").eq("id", conversationId).maybeSingle();
    if (!conversation || (conversation.participant_a !== user.id && conversation.participant_b !== user.id)) {
      return reply({ error: "conversation_unavailable" }, 404);
    }

    if (action === "list_messages") {
      const { data: messages, error } = await admin.from("lotus_messages")
        .select("id, sender_id, body, created_at")
        .eq("conversation_id", conversationId)
        .order("created_at", { ascending: true })
        .limit(200);
      if (error) return reply({ error: "messages_unavailable" }, 503);
      return reply({ messages: (messages ?? []).map((message) => ({ ...message, mine: message.sender_id === user.id })) });
    }

    if (action === "send_message") {
      const text = typeof body.text === "string" ? body.text.trim() : "";
      if (!text.startsWith("LOTUS1:")) return reply({ error: "encryption_required" }, 400);
      if (text.length > 12000) return reply({ error: "message_length_invalid" }, 400);
      const { data: message, error } = await admin.from("lotus_messages")
        .insert({ conversation_id: conversationId, sender_id: user.id, body: text })
        .select("id, sender_id, body, created_at").single();
      if (error || !message) return reply({ error: "message_send_failed" }, 503);
      const { error: updateError } = await admin.from("lotus_conversations")
        .update({ last_message_at: message.created_at }).eq("id", conversationId);
      if (updateError) return reply({ error: "message_send_failed" }, 503);
      return reply({ message });
    }

    return reply({ error: "unknown_action" }, 400);
  } catch {
    return reply({ error: "internal_error" }, 500);
  }
});
