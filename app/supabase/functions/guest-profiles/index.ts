const cors = { "Access-Control-Allow-Origin": "*", "Access-Control-Allow-Headers": "authorization, apikey, content-type", "Access-Control-Allow-Methods": "GET, POST, OPTIONS" };
const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: { ...cors, "Content-Type": "application/json" } });
const escapeHtml = (value: string) => value.replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "\"": "&quot;", "'": "&#39;" })[char]!);
const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const imageTypes: Record<string, string> = { "image/jpeg": "jpg", "image/png": "png", "image/webp": "webp", "image/gif": "gif" };

async function ownerHash(token: string) {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(token));
  return [...new Uint8Array(digest)].map((part) => part.toString(16).padStart(2, "0")).join("");
}
function apiHeaders(extra: Record<string, string> = {}) {
  return { apikey: serviceKey, Authorization: `Bearer ${serviceKey}`, ...extra };
}
async function databaseRequest(method: string, table: string, params: Record<string, string> = {}, body?: unknown, prefer?: string) {
  const query = new URLSearchParams(params).toString();
  const response = await fetch(`${supabaseUrl}/rest/v1/${table}${query ? `?${query}` : ""}`, {
    method,
    headers: apiHeaders({ Accept: "application/json", ...(body == null ? {} : { "Content-Type": "application/json" }), ...(prefer ? { Prefer: prefer } : {}) }),
    ...(body == null ? {} : { body: JSON.stringify(body) }),
  });
  const text = await response.text();
  if (!response.ok) throw new Error(`Database request failed (${response.status}): ${text.slice(0, 300)}`);
  return text ? JSON.parse(text) : null;
}
async function ownerProfile(clientId: string, token: string) {
  const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
  if (!uuid.test(clientId) || !uuid.test(token)) return null;
  const rows = await databaseRequest("GET", "guest_profiles", { select: "client_id,owner_token_hash", client_id: `eq.${clientId}`, limit: "1" });
  const profile = rows?.[0];
  return profile?.owner_token_hash === await ownerHash(token) ? profile : null;
}
async function authenticatedShortsClient(clientId: string, token: string) {
  const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[1-8][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;
  if (!uuid.test(clientId) || !uuid.test(token)) return false;
  const tokenHash = await ownerHash(token);
  const params = { select: "token_hash,expires_at", client_id: `eq.${clientId}`, limit: "1" };
  let rows = await databaseRequest("GET", "shorts_client_tokens", params);
  if (!rows?.length) {
    await databaseRequest("POST", "shorts_client_tokens", { on_conflict: "client_id" }, {
      client_id: clientId, token_hash: tokenHash,
    }, "resolution=ignore-duplicates,return=minimal");
    rows = await databaseRequest("GET", "shorts_client_tokens", params);
  }
  const client = rows?.[0];
  if (client?.token_hash !== tokenHash) return false;
  await databaseRequest("PATCH", "shorts_client_tokens", { client_id: `eq.${clientId}` }, {
    last_seen_at: new Date().toISOString(),
    expires_at: new Date(Date.now() + 365 * 24 * 60 * 60 * 1000).toISOString(),
  }, "return=minimal");
  return true;
}

async function consumeWriteLimit(clientId: string, action: string, limit: number) {
  return await databaseRequest("POST", "rpc/consume_shorts_rate_limit", {}, {
    p_client_id: clientId, p_action: action, p_limit: limit, p_window_seconds: 60,
  }) === true;
}

function validClipId(value: unknown): value is string {
  return typeof value === "string" && value.trim().length >= 1 && value.length <= 2048;
}

async function uploadImage(bucket: string, clientId: string, path: string, base64: string, mime: string) {
  const extension = imageTypes[mime];
  if (!extension || !base64 || base64.length > 4_194_304) throw new Error("Elige una imagen JPG, PNG, WebP o GIF de hasta 3 MB.");
  const { error: bucketError } = await (async () => {
    const response = await fetch(`${supabaseUrl}/storage/v1/bucket`, {
      method: "POST",
      headers: apiHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({ id: bucket, name: bucket, public: true, file_size_limit: 3 * 1024 * 1024, allowed_mime_types: Object.keys(imageTypes) }),
    });
    const text = await response.text();
    if (response.ok || /already exists|duplicate/i.test(text)) return { error: null };
    return { error: new Error(`Storage bucket creation failed (${response.status}): ${text.slice(0, 250)}`) };
  })();
  if (bucketError) throw bucketError;

  const bytes = Uint8Array.from(atob(base64), (char) => char.charCodeAt(0));
  if (bytes.byteLength > 3 * 1024 * 1024) throw new Error("La imagen supera 3 MB.");
  const objectPath = `${clientId}/${path}.${extension}`;
  const encodedPath = objectPath.split("/").map(encodeURIComponent).join("/");
  const response = await fetch(`${supabaseUrl}/storage/v1/object/${encodeURIComponent(bucket)}/${encodedPath}`, {
    method: "POST",
    headers: apiHeaders({ "Content-Type": mime, "x-upsert": "true", "cache-control": "3600" }),
    body: bytes,
  });
  const text = await response.text();
  if (!response.ok) throw new Error(`Image upload failed (${response.status}): ${text.slice(0, 300)}`);
  return { url: `${supabaseUrl}/storage/v1/object/public/${encodeURIComponent(bucket)}/${encodedPath}`, mime };
}

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: cors });
  try {
    if (request.method === "GET") {
      const url = new URL(request.url);
      const username = url.searchParams.get("username")?.toLowerCase();
      if (!username || !/^[a-z0-9_.]{3,20}$/.test(username)) return json({ error: "Usuario inválido." }, 400);
      const profiles = await databaseRequest("GET", "guest_profiles", {
        select: "client_id,display_name,username,avatar_url,cover_url,cover_animated,updated_at", username: `eq.${username}`, limit: "1",
      });
      const data = profiles?.[0];
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
    const declaredLength = Number(request.headers.get("content-length") ?? 0);
    if (declaredLength > 9 * 1024 * 1024) return json({ error: "La solicitud supera el tamaño permitido." }, 413);
    const rawBody = await request.text();
    if (new TextEncoder().encode(rawBody).byteLength > 9 * 1024 * 1024) return json({ error: "La solicitud supera el tamaño permitido." }, 413);
    let body: Record<string, unknown>;
    try {
      body = JSON.parse(rawBody);
      if (!body || typeof body !== "object" || Array.isArray(body)) return json({ error: "Solicitud inválida." }, 400);
    } catch {
      return json({ error: "Solicitud inválida." }, 400);
    }
    const action = String(body.action ?? "profile");
    const clientId = String(body.client_id ?? "");
    const token = String(body.owner_token ?? "");
    if (action === "reaction" || action === "share" || action === "anonymous_comment") {
      const clipId = body.clip_id;
      if (!validClipId(clipId)) return json({ error: "Identificador de Short inválido." }, 400);
      if (!await authenticatedShortsClient(clientId, String(body.client_token ?? ""))) {
        return json({ error: "No se pudo validar esta instalación." }, 401);
      }
      const writeLimit = action === "anonymous_comment" ? 5 : action === "share" ? 15 : 30;
      if (!await consumeWriteLimit(clientId, action, writeLimit)) {
        return json({ error: "Demasiadas acciones seguidas. Inténtalo más tarde." }, 429);
      }
      if (action === "reaction") {
        if (typeof body.liked !== "boolean") return json({ error: "Reacción inválida." }, 400);
        if (body.liked) {
          await databaseRequest("POST", "shorts_reactions", { on_conflict: "clip_id,client_id" }, {
            clip_id: clipId, client_id: clientId,
          }, "resolution=ignore-duplicates,return=minimal");
        } else {
          await databaseRequest("DELETE", "shorts_reactions", { clip_id: `eq.${clipId}`, client_id: `eq.${clientId}` });
        }
        return json({ ok: true });
      }
      if (action === "share") {
        await databaseRequest("POST", "shorts_shares", {}, { clip_id: clipId, client_id: clientId }, "return=minimal");
        return json({ ok: true }, 201);
      }
      const comment = String(body.body ?? "").trim();
      if (comment.length < 1 || comment.length > 500) return json({ error: "El comentario debe tener entre 1 y 500 caracteres." }, 400);
      await databaseRequest("POST", "shorts_comments", {}, {
        clip_id: clipId, client_id: clientId, profile_client_id: null, body: comment,
      }, "return=minimal");
      return json({ ok: true }, 201);
    }
    if (action === "comment") {
      if (!await ownerProfile(clientId, token)) return json({ error: "Guarda tu perfil antes de comentar como usuario." }, 403);
      const clipId = String(body.clip_id ?? "");
      const comment = String(body.body ?? "").trim();
      if (!validClipId(clipId) || comment.length < 1 || comment.length > 500) return json({ error: "El comentario debe tener entre 1 y 500 caracteres." }, 400);
      if (!await consumeWriteLimit(clientId, "profile_comment", 5)) {
        return json({ error: "Demasiados comentarios seguidos. Inténtalo más tarde." }, 429);
      }
      const rows = await databaseRequest("POST", "shorts_comments", { select: "id,body,created_at,profile:guest_profiles!shorts_comments_profile_client_id_fkey(client_id,display_name,username,avatar_url)" }, {
        clip_id: clipId, client_id: clientId, profile_client_id: clientId, body: comment,
      }, "return=representation");
      return json(rows?.[0] ?? {}, 201);
    }
    if (action !== "profile") return json({ error: "Acción no permitida." }, 400);

    const displayName = String(body.display_name ?? "").trim();
    const username = String(body.username ?? "").trim().toLowerCase();
    if (!/^[0-9a-f-]{36}$/i.test(clientId) || !/^[0-9a-f-]{36}$/i.test(token)) return json({ error: "No se pudo validar este perfil." }, 400);
    if (displayName.length < 1 || displayName.length > 40 || !/^[a-z0-9_.]{3,20}$/.test(username)) return json({ error: "Revisa tu nombre (1–40) y @usuario (3–20; letras, números, _ o .)." }, 400);
    const existingRows = await databaseRequest("GET", "guest_profiles", {
      select: "owner_token_hash,avatar_url,cover_url", client_id: `eq.${clientId}`, limit: "1",
    });
    const existing = existingRows?.[0];
    if (existing && existing.owner_token_hash !== await ownerHash(token)) return json({ error: "Este perfil pertenece a otra instalación." }, 403);
    if (!await consumeWriteLimit(clientId, "profile", 5)) return json({ error: "Demasiados cambios seguidos. Inténtalo más tarde." }, 429);
    const avatarUrl = body.avatar_base64 ? (await uploadImage("samplay-profiles", clientId, `avatar-${crypto.randomUUID()}`, String(body.avatar_base64), String(body.avatar_mime ?? ""))).url : (body.remove_avatar ? null : existing?.avatar_url ?? null);
    const coverUrl = body.cover_base64 ? (await uploadImage("samplay-profiles", clientId, `cover-${crypto.randomUUID()}`, String(body.cover_base64), String(body.cover_mime ?? ""))).url : (body.remove_cover ? null : existing?.cover_url ?? null);
    const row = { client_id: clientId, display_name: displayName, username, avatar_url: avatarUrl, cover_url: coverUrl, cover_animated: Boolean(body.cover_animated), owner_token_hash: await ownerHash(token), updated_at: new Date().toISOString() };
    const rows = await databaseRequest("POST", "guest_profiles", { on_conflict: "client_id", select: "client_id,display_name,username,avatar_url,cover_url,cover_animated,updated_at" }, row, "resolution=merge-duplicates,return=representation");
    return json(rows?.[0] ?? {}, 200);
  } catch (error) {
    console.error(error);
    return json({ error: "No se pudo completar la acción." }, 500);
  }
});
