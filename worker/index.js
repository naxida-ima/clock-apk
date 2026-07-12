export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const p = url.pathname;

    if (p === '/heartbeat') {
      const id = url.searchParams.get('id') || 'anon';
      let map = {};
      try { map = JSON.parse(await env.CLOCK_KV.get('online') || '{}'); } catch (e) {}
      const now = Date.now();
      map[id] = now;
      for (const k of Object.keys(map)) if (now - map[k] > 60000) delete map[k];
      await env.CLOCK_KV.put('online', JSON.stringify(map));
      return json({ online: Object.keys(map).length });
    }

    if (p === '/nextentry') {
      const t = await env.CLOCK_KV.get('nextEntry') || '';
      return json({ time: t });
    }

    if (p === '/admin') {
      const cookie = request.headers.get('cookie') || '';
      const authed = env.ADMIN_TOKEN && cookie.includes('clock_admin=' + env.ADMIN_TOKEN);
      if (request.method === 'POST') {
        const form = await request.formData().catch(() => null);
        if (form && form.has('pass')) {
          if (form.get('pass') === env.ADMIN_PASS) {
            return new Response(null, { status: 302, headers: {
              'Location': '/admin',
              'Set-Cookie': 'clock_admin=' + env.ADMIN_TOKEN + '; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400'
            }});
          }
          return new Response(loginHtml('密码错误'), { headers: { 'content-type': 'text/html; charset=utf-8' } });
        }
        if (!authed) return new Response('未授权', { status: 401 });
        const t = (form.get('time') || '').toString().slice(0, 100);
        await env.CLOCK_KV.put('nextEntry', t);
        return new Response(adminHtml(t, '已保存'), { headers: { 'content-type': 'text/html; charset=utf-8' } });
      }
      if (!authed) return new Response(loginHtml(), { headers: { 'content-type': 'text/html; charset=utf-8' } });
      const t = await env.CLOCK_KV.get('nextEntry') || '';
      return new Response(adminHtml(t, ''), { headers: { 'content-type': 'text/html; charset=utf-8' } });
    }

    return new Response('Not Found', { status: 404 });
  }
}

function json(o) {
  return new Response(JSON.stringify(o), { headers: { 'content-type': 'application/json; charset=utf-8', 'access-control-allow-origin': '*' } });
}
function loginHtml(err) {
  return '<!doctype html><html lang=zh><meta charset=utf-8><title>管理员登录</title><body style="font-family:sans-serif;max-width:420px;margin:40px auto"><form method=post><h3>管理员登录</h3>' + (err ? '<p style=color:red>'+err+'</p>' : '') + '<p><input name=pass type=password placeholder="管理员密码" required></p><p><button>登录</button></p></form></body></html>';
}
function adminHtml(t, msg) {
  return '<!doctype html><html lang=zh><meta charset=utf-8><title>后台管理</title><body style="font-family:sans-serif;max-width:420px;margin:40px auto"><form method=post><h3>下次进入时间</h3>' + (msg ? '<p style=color:green>'+msg+'</p>' : '') + '<p><input name=time value="'+escapeHtml(t)+'" placeholder="如：今晚8点 / 2026-07-13 20:00" style="width:100%;padding:8px"></p><p><button>保存</button></p></form><hr><p>当前值：'+(t ? escapeHtml(t) : '（未设置）')+'</p></body></html>';
}
function escapeHtml(s) {
  return (s || '').replace(/[&<>"']/g, function(c){ return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]; });
}