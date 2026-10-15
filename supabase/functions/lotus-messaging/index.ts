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
    if (!user.phone || !user.phone_confirmed_at) return reply({ error: "verified_phone_required" }, 403);

    let body: { action?: unknown; contactId?: unknown; conversationId?: unknown; text?: unknown };
    try {
      body = await req.json();
    } catch {
      return reply({ error: "invalid_json" }, 400);
    }

    const admin = createClient(url, serviceKey, { auth: { persistSession: false, autoRefreshToken: false } });
    const action = body.action;

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
          admin.from("lotus_profiles").select("display_name").eq("user_id", peerId).maybeSingle(),
          admin.from("lotus_messages").select("body, created_at").eq("conversation_id", conversation.id)
            .order("created_at", { ascending: false }).limit(1).maybeSingle(),
        ]);
        chats.push({
          id: conversation.id,
          peerId,
          displayName: profile?.display_name ?? "Lotus contact",
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

      const { data: target } = await admin.from("lotus_profiles").select("user_id, phone_verified, display_name")
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
      return reply({ messages: messages ?? [] });
    }

    if (action === "send_message") {
      const text = typeof body.text === "string" ? body.text.trim() : "";
      if (!text || text.length > 4000) return reply({ error: "message_length_invalid" }, 400);
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
