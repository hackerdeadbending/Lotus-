import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, apikey, content-type, x-client-info",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
  "Content-Type": "application/json; charset=utf-8",
};

function json(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: corsHeaders });
}

function normalizeE164(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const normalized = value.replace(/[\s().-]/g, "");
  return /^\+[1-9]\d{7,14}$/.test(normalized) ? normalized : null;
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "method_not_allowed" }, 405);

  const url = Deno.env.get("SUPABASE_URL");
  const publishableKey = Deno.env.get("SUPABASE_ANON_KEY") ?? Deno.env.get("SUPABASE_PUBLISHABLE_KEY");
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? Deno.env.get("SUPABASE_SECRET_KEY");
  if (!url || !publishableKey || !serviceKey) {
    return json({ error: "server_not_configured" }, 503);
  }

  const authorization = req.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) return json({ error: "unauthorized" }, 401);
  const token = authorization.slice("Bearer ".length);

  try {
    const authClient = createClient(url, publishableKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: authData, error: authError } = await authClient.auth.getUser(token);
    const user = authData.user;
    if (authError || !user) return json({ error: "unauthorized" }, 401);

    // Do not accept a caller-supplied owner ID. Identity comes only from the verified JWT.
    if (!user.phone || !user.phone_confirmed_at) {
      return json({ error: "verified_phone_required" }, 403);
    }

    let body: { phone?: unknown };
    try {
      body = await req.json();
    } catch {
      return json({ error: "invalid_json" }, 400);
    }
    const phone = normalizeE164(body.phone);
    if (!phone) return json({ error: "phone_must_be_e164" }, 400);
    if (phone === user.phone) return json({ error: "cannot_add_self" }, 400);

    // The service key is server-only. Never put it in the Android app.
    const admin = createClient(url, serviceKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });

    // Keep lookup responses generic for unknown numbers to reduce account enumeration.
    const { data: target, error: targetError } = await admin
      .from("lotus_profiles")
      .select("user_id, display_name")
      .eq("phone_e164", phone)
      .eq("phone_verified", true)
      .maybeSingle();

    if (targetError) return json({ error: "lookup_unavailable" }, 503);
    if (!target) return json({ accepted: true, mutual: false });

    const { error: edgeError } = await admin
      .from("lotus_contact_edges")
      .upsert({ owner_id: user.id, contact_id: target.user_id }, { onConflict: "owner_id,contact_id", ignoreDuplicates: true });
    if (edgeError) return json({ error: "lookup_unavailable" }, 503);

    const { data: reciprocal, error: reciprocalError } = await admin
      .from("lotus_contact_edges")
      .select("owner_id")
      .eq("owner_id", target.user_id)
      .eq("contact_id", user.id)
      .maybeSingle();

    if (reciprocalError) return json({ error: "lookup_unavailable" }, 503);
    if (!reciprocal) return json({ accepted: true, mutual: false });

    return json({
      accepted: true,
      mutual: true,
      contact: { id: target.user_id, displayName: target.display_name },
    });
  } catch {
    return json({ error: "internal_error" }, 500);
  }
});
