import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "authorization, apikey, content-type", "Access-Control-Allow-Methods": "GET, POST, OPTIONS" };
const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: { ...cors, "Content-Type": "application/json" } });
const escapeHtml = (value: string) => value.replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;" })[char]!);
const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const admin = createClient(supabaseUrl, serviceKey, { auth: { persistSession: false } });
const bucket = "samplay-profiles";
const imageTypes: Record<string, string> = { "image/jpeg": "jpg", "image/png": "png", "image/webp": "webp", "image/gif": "gif" };

async function ownerHash(token: string) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(token));
  return [...new Uint8Array(digest)].map((part) => part.toString(16).padStart(2, "0")).join("");
}
async function ensureBucket() {
  const { error } = await admin.storage.createBucket(bucket, { public: true, fileSizeLimit: 3 * 1024 * 1024, allowedMimeTypes: Object.keys(imageTypes) });
  if (error && !/already exists/i.test(error.message)) throw error;
}
async function saveImage(clientId: string, field: "avatar" | "cover", base64: string, mime: string) {
  const extension = imageTypes[mime];
  if (!extension || base64.length > 4_194_304) throw new Error("Imagen inválida o demasiado grande (máximo 3 MB).");
  await ensureBucket();
  const bytes = Uint8Array.from(atob(base64), (char) => char.charCodeAt(0));
  if (bytes.byteLength > 3 * 1024 * 1024) throw new Error("La imagen supera 3 MB.");
  const path = `${clientId}/${field}.${extension}`;
  const { error } = await admin.storage.from(bucket).upload(path, bytes, { contentType: mime, upsert: true, cacheControl: "3600" });
  if (error) throw error;
  return admin.storage.from(bucket).getPublicUrl(path).data.publicUrl;
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: cors });
  try {
    if (request.method === "GET") {
      const username = new URL(request.url).searchParams.get("username")?.toLowerCase();
      if (!username || !/^[a-z0-9_.]{3,20}$/.test(username)) return json({ error: "Usuario inválido." }, 400);
      const { data, error } = await admin.from("guest_profiles").select("client_id,display_name,username,avatar_url,cover_url,cover_animated,updated_at").eq("username", username).maybeSingle();
      if (error) throw error;
      if (!data) return json({ error: "No encontramos ese perfil." }, 404);
      if ((request.headers.get("accept") ?? "").includes("text/html")) {
        const title = `${escapeHtml(data.display_name)} (@${escapeHtml(data.username)}) · SamPlay`;
        const image = data.avatar_url ? `<meta property="og:image" content="${escapeHtml(data.avatar_url)}">` : "";
        const cover = data.cover_url ? `background-image:url('${escapeHtml(data.cover_url)}')` : "";
        const avatar = data.avatar_url ? `<img class="avatar" src="${escapeHtml(data.avatar_url)}" alt="">` : `<div class="avatar fallback">${escapeHtml(data.display_name.slice(0, 1).toUpperCase())}</div>`;
        const html = `<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta property="og:title" content="${title}"><meta property="og:description" content="Mira el perfil de @${escapeHtml(data.username)} en SamPlay.">${image}<title>${title}</title><style>*{box-sizing:border-box}body{margin:0;background:#0d0e12;color:#fff;font:16px system-ui;display:grid;min-height:100vh;place-items:center;padding:22px}.card{width:min(100%,520px);overflow:hidden;border-radius:24px;background:#171920;box-shadow:0 18px 80px #0008}.cover{height:220px;background:linear-gradient(135deg,#442336,#171a2a,#28234d);background-size:cover;background-position:center}.content{padding:0 24px 26px}.avatar{width:92px;height:92px;border-radius:50%;object-fit:cover;border:4px solid #171920;margin-top:-46px;position:relative;background:#28234d}.fallback{display:grid;place-items:center;font-size:34px;font-weight:700}.name{margin:12px 0 2px;font-size:26px}.handle{margin:0 0 22px;color:#ff8ba0}.button{display:block;text-align:center;text-decoration:none;color:#fff;background:#db3159;border-radius:12px;padding:14px;font-weight:700}</style></head><body><main class="card"><div class="cover" style="${cover}"></div><section class="content">${avatar}<h1 class="name">${escapeHtml(data.display_name)}</h1><p class="handle">@${escapeHtml(data.username)}</p><a class="button" href="samplay://profile/${encodeURIComponent(data.username)}">Abrir en SamPlay</a></section></main></body></html>`;
        return new Response(html, { headers: { ...cors, "Content-Type": "text/html; charset=utf-8" } });
      }
      return json(data);
    }
    if (request.method !== "POST") return json({ error: "Método no permitido." }, 405);
    const body = await request.json();
    const clientId = String(body.client_id ?? "");
    const token = String(body.owner_token ?? "");
    const displayName = String(body.display_name ?? "").trim();
    const username = String(body.username ?? "").trim().toLowerCase();
    if (!/^[0-9a-f-]{36}$/i.test(clientId) || !/^[0-9a-f-]{36}$/i.test(token)) return json({ error: "No se pudo validar este perfil." }, 400);
    if (displayName.length < 1 || displayName.length > 40 || !/^[a-z0-9_.]{3,20}$/.test(username)) return json({ error: "Revisa tu nombre (1–40) y @usuario (3–20; letras, números, _ o .)." }, 400);
    const { data: existing, error: findError } = await admin.from("guest_profiles").select("client_id,owner_token_hash,avatar_url,cover_url").eq("client_id", clientId).maybeSingle();
    if (findError) throw findError;
    const tokenHash = await ownerHash(token);
    if (existing && existing.owner_token_hash !== tokenHash) return json({ error: "Este perfil pertenece a otra instalación." }, 403);
    const avatarUrl = body.avatar_base64 ? await saveImage(clientId, "avatar", String(body.avatar_base64), String(body.avatar_mime ?? "")) : (body.remove_avatar ? null : existing?.avatar_url ?? null);
    const coverUrl = body.cover_base64 ? await saveImage(clientId, "cover", String(body.cover_base64), String(body.cover_mime ?? "")) : (body.remove_cover ? null : existing?.cover_url ?? null);
    const row = { client_id: clientId, display_name: displayName, username, avatar_url: avatarUrl, cover_url: coverUrl, cover_animated: Boolean(body.cover_animated), owner_token_hash: tokenHash, updated_at: new Date().toISOString() };
    const { data, error } = await admin.from("guest_profiles").upsert(row, { onConflict: "client_id" }).select("client_id,display_name,username,avatar_url,cover_url,cover_animated,updated_at").single();
    if (error) return json({ error: /guest_profiles_username_ci_idx|duplicate key/i.test(error.message) ? "Ese @usuario ya está ocupado. Prueba otro." : error.message }, 409);
    return json(data);
  } catch (error) {
    console.error(error);
    return json({ error: error instanceof Error ? error.message : "No se pudo guardar el perfil." }, 500);
  }
});
