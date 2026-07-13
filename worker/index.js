// clock-online worker：北京时间「下次进入时间」后台
// 三种设置模式：manual(直接写) / xml(上传时间表) / interval(间隔循环)
// 对外 /nextentry 始终返回 {time, note}，APK 端兼容无需改动

const KV_CFG = 'nextEntryConfig';

export default {
  async fetch(request, env) {
    try {
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
      const cfg = await loadCfg(env);
      const disp = computeDisplay(cfg, Date.now());
      return json({ time: disp.time, note: disp.note });
    }

    if (p === '/admin') {
      return await handleAdmin(request, env);
    }

    if (p === '/xml') {
      return new Response(xmlBuilderHtml(), { headers: { 'content-type': 'text/html; charset=utf-8' } });
    }

    return new Response('Not Found', { status: 404 });
    } catch (e) {
      return new Response('后台错误: ' + (e && e.stack ? e.stack : String(e)), { status: 500, headers: { 'content-type': 'text/plain; charset=utf-8' } });
    }
  }
};

async function handleAdmin(request, env) {
  const cookie = request.headers.get('cookie') || '';
  const authed = env.ADMIN_TOKEN && cookie.includes('clock_admin=' + env.ADMIN_TOKEN);

  if (request.method === 'POST') {
    const form = await request.formData().catch(() => null);
    if (!form) return new Response('Bad Request', { status: 400 });

    // 登录
    if (form.has('pass')) {
      if (form.get('pass') === env.ADMIN_PASS) {
        return new Response(null, { status: 302, headers: {
          'Location': '/admin',
          'Set-Cookie': 'clock_admin=' + env.ADMIN_TOKEN + '; Path=/; HttpOnly; SameSite=Lax; Max-Age=86400'
        }});
      }
      return new Response(loginHtml('密码错误'), { headers: { 'content-type': 'text/html; charset=utf-8' } });
    }

    if (!authed) return new Response('未授权', { status: 401 });

    // 退出登录
    if (form.get('logout')) {
      return new Response(null, { status: 302, headers: {
        'Location': '/admin',
        'Set-Cookie': 'clock_admin=; Path=/; Max-Age=0'
      }});
    }

    // 已登录：按 mode 保存配置
    const mode = (form.get('mode') || 'manual');
    const cfg = { mode };
    if (mode === 'manual') {
      cfg.manualTime = (form.get('manualTime') || '').toString().slice(0, 100);
    } else if (mode === 'xml') {
      let raw = '';
      const f = form.get('xmlfile');
      if (f && typeof f === 'object' && typeof f.text === 'function') { raw = await f.text(); }
      if (!raw) raw = (form.get('xmltext') || '').toString();
      const entries = parseScheduleXml(raw);
      cfg.xmlEntries = entries;
      cfg.xmlRaw = raw.slice(0, 4000);
    } else if (mode === 'interval') {
      cfg.interval = {
        start: (form.get('start') || '').toString().slice(0, 100),
        unit: (form.get('unit') || 'day'),
        step: parseInt((form.get('step') || '1').toString(), 10) || 1,
        note: (form.get('note') || '').toString().slice(0, 100)
      };
    }
    await saveCfg(env, cfg);
    const disp = computeDisplay(cfg, Date.now());
    return new Response(adminHtml(cfg, '已保存 ✓', disp), { headers: { 'content-type': 'text/html; charset=utf-8' } });
  }

  // GET
  if (!authed) return new Response(loginHtml(), { headers: { 'content-type': 'text/html; charset=utf-8' } });
  const cfg = await loadCfg(env);
  const disp = computeDisplay(cfg, Date.now());
  return new Response(adminHtml(cfg, '', disp), { headers: { 'content-type': 'text/html; charset=utf-8' } });
}

// ---- 配置读写 ----
async function loadCfg(env) {
  try { return JSON.parse(await env.CLOCK_KV.get(KV_CFG) || '{}'); } catch (e) { return {}; }
}
async function saveCfg(env, cfg) {
  await env.CLOCK_KV.put(KV_CFG, JSON.stringify(cfg));
}

// ---- 时间解析：支持 YYYY-MM-DD HH:mm / MM-DD HH:mm / HH:mm ----
function parseTime(s) {
  s = (s || '').toString().trim();
  if (!s) return null;
  let m = s.match(/^(\d{4})-(\d{1,2})-(\d{1,2})[ T](\d{1,2}):(\d{1,2})$/);
  if (m) return new Date(+m[1], +m[2] - 1, +m[3], +m[4], +m[5]);
  m = s.match(/^(\d{1,2})-(\d{1,2})[ T](\d{1,2}):(\d{1,2})$/);
  if (m) { const n = new Date(); return new Date(n.getFullYear(), +m[1] - 1, +m[2], +m[3], +m[4]); }
  m = s.match(/^(\d{1,2}):(\d{1,2})$/);
  if (m) { const n = new Date(); return new Date(n.getFullYear(), n.getMonth(), n.getDate(), +m[1], +m[2]); }
  const d = new Date(s);
  return isNaN(d.getTime()) ? null : d;
}
function fmt(d) {
  const p = n => (n < 10 ? '0' + n : '' + n);
  return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes());
}

// ---- XML 解析（特定格式）----
// <schedule>
//   <entry time="2026-07-13 20:00" note="回来吃饭"/>
//   <entry time="2026-07-14 19:30" note="看电影"/>
// </schedule>
function parseScheduleXml(xml) {
  const entries = [];
  if (!xml) return entries;
  const re = /<entry\b([^>]*)>/gi;
  let m;
  while ((m = re.exec(xml))) {
    const t = pickAttr(m[1], 'time');
    const n = pickAttr(m[1], 'note');
    const d = parseTime(t);
    if (d) entries.push({ time: t, note: n, ts: d.getTime() });
  }
  entries.sort((a, b) => a.ts - b.ts);
  return entries;
}
function pickAttr(attrs, name) {
  const m = attrs.match(new RegExp(name + '\\s*=\\s*"([^"]*)"', 'i')) ||
            attrs.match(new RegExp(name + "\\s*=\\s*'([^']*)'", 'i'));
  return m ? m[1] : '';
}

// ---- 计算当前应显示给软件端的内容 ----
function computeDisplay(cfg, now) {
  now = now || Date.now();
  cfg = cfg || {};
  if (cfg.mode === 'manual') {
    return { time: cfg.manualTime || '', note: '' };
  }
  if (cfg.mode === 'xml') {
    const list = cfg.xmlEntries || [];
    const future = list.filter(e => e.ts > now);
    const pick = future.length ? future[0] : (list.length ? list[list.length - 1] : null);
    return pick ? { time: pick.time, note: pick.note || '' } : { time: '', note: '' };
  }
  if (cfg.mode === 'interval') {
    const iv = cfg.interval || {};
    const start = parseTime(iv.start || '');
    if (!start) return { time: '', note: '' };
    const unitMs = { hour: 3600e3, day: 86400e3, week: 604800e3 }[iv.unit || 'day'] || 86400e3;
    const step = Math.max(1, parseInt(iv.step || '1', 10) || 1);
    const period = step * unitMs;
    let t = start.getTime();
    if (t <= now) {
      const n = Math.ceil((now - t) / period);
      t += n * period;
    }
    return { time: fmt(new Date(t)), note: iv.note || '' };
  }
  return { time: '', note: '' };
}

// ---- 响应辅助 ----
function json(o) {
  return new Response(JSON.stringify(o), {
    headers: { 'content-type': 'application/json; charset=utf-8', 'access-control-allow-origin': '*' }
  });
}
function htmlHeaders() { return new Headers([['content-type', 'text/html; charset=utf-8']]); }
function escapeHtml(s) {
  s = (s == null) ? '' : String(s);
  return s.replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

// ---- 页面 ----
function loginHtml(err) {
  return page(`<div class="card">
    <h2>🔐 管理员登录</h2>
    ${err ? `<p class="err">${escapeHtml(err)}</p>` : ''}
    <form method="post">
      <input name="pass" type="password" placeholder="管理员密码" autocomplete="current-password" required>
      <button type="submit">登录</button>
    </form>
  </div>`);
}

function adminHtml(cfg, msg, disp) {
  cfg = cfg || {};
  const mode = cfg.mode || 'manual';
  const preview = (disp && disp.time)
    ? `下次进入 <b>${escapeHtml(disp.time)}</b>${disp.note ? ' · ' + escapeHtml(disp.note) : ''}`
    : '（未设置）';

  return page(`<div class="card">
    <div class="top">
      <h2>⏰ 下次进入时间 · 后台</h2>
      <form method="post" class="logout">
        <button type="submit" name="logout" value="1" class="ghost">退出</button>
      </form>
    </div>

    <div class="preview">软件端当前显示：<span>${preview}</span></div>
    ${msg ? `<div class="ok">${escapeHtml(msg)}</div>` : ''}

    <div class="tabs">
      <button type="button" class="tab ${mode === 'manual' ? 'on' : ''}" onclick="show('manual',this)">✍️ 直接写</button>
      <button type="button" class="tab ${mode === 'xml' ? 'on' : ''}" onclick="show('xml',this)">📄 上传XML</button>
      <button type="button" class="tab ${mode === 'interval' ? 'on' : ''}" onclick="show('interval',this)">🔁 间隔循环</button>
    </div>

    <form method="post" id="f-manual" class="pane ${mode === 'manual' ? 'on' : ''}">
      <input type="hidden" name="mode" value="manual">
      <label>时间（如 今晚8点 或 2026-07-13 20:00）</label>
      <input name="manualTime" value="${escapeHtml(cfg.manualTime || '')}" placeholder="2026-07-13 20:00">
      <button type="submit">保存</button>
    </form>

    <form method="post" id="f-xml" class="pane ${mode === 'xml' ? 'on' : ''}" enctype="multipart/form-data">
      <input type="hidden" name="mode" value="xml">
      <label>粘贴 XML（或选择文件上传）</label>
      <textarea name="xmltext" placeholder='<schedule>&#10;  <entry time="2026-07-13 20:00" note="回来吃饭"/>&#10;  <entry time="2026-07-14 19:30" note="看电影"/>&#10;</schedule>'></textarea>
      <label class="file">或上传 .xml 文件：<input type="file" name="xmlfile" accept=".xml,text/xml"></label>
      <p class="hint">格式：每个 &lt;entry time="..." note="..."/&gt;，后台自动显示「未来最近」的一个时间点。</p>
      <button type="submit">解析并保存</button>
    </form>

    <form method="post" id="f-interval" class="pane ${mode === 'interval' ? 'on' : ''}">
      <input type="hidden" name="mode" value="interval">
      <label>起始时间</label>
      <input name="start" value="${escapeHtml((cfg.interval && cfg.interval.start) || '')}" placeholder="2026-07-13 20:00">
      <div class="row">
        <div><label>每隔</label><input name="step" type="number" min="1" value="${escapeHtml((cfg.interval && cfg.interval.step) || '1')}"></div>
        <div><label>单位</label>
          <select name="unit">
            <option value="hour" ${cfg.interval && cfg.interval.unit === 'hour' ? 'selected' : ''}>小时</option>
            <option value="day" ${(!cfg.interval || cfg.interval.unit === 'day') ? 'selected' : ''}>天</option>
            <option value="week" ${cfg.interval && cfg.interval.unit === 'week' ? 'selected' : ''}>周</option>
          </select>
        </div>
      </div>
      <label>备注（可选，如「回来」）</label>
      <input name="note" value="${escapeHtml((cfg.interval && cfg.interval.note) || '')}" placeholder="回来">
      <p class="hint">后台自动算出「下一个未来的周期点」并显示给软件端。</p>
      <button type="submit">保存</button>
    </form>
  </div>
  <script>
    function show(m, el){
      document.querySelectorAll('.tab').forEach(t=>t.classList.remove('on'));
      document.querySelectorAll('.pane').forEach(p=>p.classList.remove('on'));
      el.classList.add('on');
      document.getElementById('f-'+m).classList.add('on');
    }
  </script>`);
}

function page(body, title) {
  return `<!doctype html><html lang="zh"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>${title || '后台管理'}</title>
<style>
  :root{--bg:#0f1220;--card:#1a1f35;--fg:#e8ecff;--mut:#8b93b8;--acc:#6c8cff;--acc2:#45e0c0;--ok:#3ddc97;--err:#ff6b81}
  *{box-sizing:border-box}
  body{margin:0;font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,"PingFang SC","Microsoft YaHei",sans-serif;background:radial-gradient(1200px 600px at 70% -10%,#26305c 0%,var(--bg) 60%);color:var(--fg);min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px}
  .card{width:100%;max-width:480px;background:var(--card);border:1px solid #2a3358;border-radius:18px;padding:24px;box-shadow:0 20px 60px rgba(0,0,0,.45)}
  h2{margin:0 0 14px;font-size:20px}
  .top{display:flex;align-items:center;justify-content:space-between}
  label{display:block;font-size:13px;color:var(--mut);margin:14px 0 6px}
  input,select,textarea{100%;padding:11px 12px;border-radius:10px;border:1px solid #313b66;background:#11152a;color:var(--fg);font-size:14px;outline:none}
  input:focus,select:focus,textarea:focus{border-color:var(--acc)}
  textarea{min-height:120px;resize:vertical;font-family:ui-monospace,Menlo,Consolas,monospace;font-size:13px}
  button{margin-top:14px;padding:11px 16px;border:0;border-radius:10px;background:linear-gradient(135deg,var(--acc),var(--acc2));color:#0c1024;font-weight:600;font-size:14px;cursor:pointer}
  button.ghost{background:transparent;border:1px solid #313b66;color:var(--mut);margin:0}
  .logout{margin:0}
  .preview{margin-top:4px;padding:12px 14px;background:#11152a;border-radius:10px;font-size:14px;color:var(--mut)}
  .preview span{color:var(--acc2);font-weight:600}
  .ok{margin-top:12px;padding:10px 12px;background:rgba(61,220,151,.12);border:1px solid rgba(61,220,151,.4);color:var(--ok);border-radius:10px;font-size:13px}
  .err{color:var(--err);font-size:13px;margin:0 0 8px}
  .hint{color:var(--mut);font-size:12px;margin:8px 0 0;line-height:1.5}
  .tabs{display:flex;gap:8px;margin:18px 0 6px}
  .tab{flex:1;margin:0;background:#11152a;border:1px solid #313b66;color:var(--mut);font-weight:500}
  .tab.on{background:linear-gradient(135deg,var(--acc),var(--acc2));color:#0c1024;border-color:transparent}
  .pane{display:none}
  .pane.on{display:block}
  .row{display:flex;gap:12px}.row>div{flex:1}
  .file{font-size:13px;color:var(--mut)}
</style></head>
<body>${body}</body></html>`;
}

function xmlBuilderHtml() {
  const inner = `<div class="card">
  <h2>📄 可视化 XML 生成器</h2>
  <p class="hint">给「北京时间」后台用。填时间和备注，自动生成后台需要的 XML，复制后贴到后台「上传XML」模式即可。时间格式如 <b>2026-07-13 20:00</b>（也支持 07-13 20:00 或 20:00）。</p>
  <div id="rows"></div>
  <button type="button" class="ghost" onclick="addRow()">+ 添加一行</button>
  <div class="preview" style="margin-top:16px">生成的 XML：</div>
  <textarea id="out" readonly style="margin-top:6px"></textarea>
  <div class="tabs" style="margin-top:12px">
    <button type="button" onclick="copyXml()">复制 XML</button>
    <button type="button" onclick="downloadXml()">下载 .xml</button>
    <button type="button" class="ghost" onclick="document.getElementById('imp').style.display='block'">导入已有 XML</button>
  </div>
  <div id="imp" style="display:none;margin-top:12px">
    <textarea id="impbox" placeholder="粘贴已有 XML 进行编辑..."></textarea>
    <button type="button" onclick="importXml()">解析并回填</button>
  </div>
  <style>.rowitem{display:flex;gap:8px;margin:8px 0}.rowitem input{flex:1}.rowitem .n{flex:2}</style>
</div>
<script>
function esc(s){return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');}
function rowHtml(t,n){return '<div class="rowitem"><input class="t" placeholder="2026-07-13 20:00" value="'+esc(t)+'"><input class="n" placeholder="备注，如 回来吃饭" value="'+esc(n)+'"><button type="button" class="ghost" onclick="this.parentNode.remove();gen()">✕</button></div>';}
function addRow(){document.getElementById('rows').insertAdjacentHTML('beforeend',rowHtml('',''));gen();}
function gen(){var rows=document.querySelectorAll('#rows .rowitem');var xml='<schedule>';rows.forEach(function(r){var t=r.querySelector('.t').value.trim();var n=r.querySelector('.n').value.trim();if(t){xml+='<entry time="'+esc(t)+'" note="'+esc(n)+'"/>';}});xml+='</schedule>';document.getElementById('out').value=xml;}
function copyXml(){var o=document.getElementById('out');o.select();try{navigator.clipboard.writeText(o.value);}catch(e){}document.execCommand('copy');alert('已复制 XML 到剪贴板');}
function downloadXml(){var b=new Blob([document.getElementById('out').value],{type:'application/xml'});var a=document.createElement('a');a.href=URL.createObjectURL(b);a.download='schedule.xml';a.click();}
function importXml(){var x=document.getElementById('impbox').value;var re=/<entry\b([^>]*)>/gi;var m;document.getElementById('rows').innerHTML='';while((m=re.exec(x))){var t=(m[1].match(/time\s*=\s*"([^"]*)"/i)||[])[1]||'';var n=(m[1].match(/note\s*=\s*"([^"]*)"/i)||[])[1]||'';document.getElementById('rows').insertAdjacentHTML('beforeend',rowHtml(t,n));}gen();}
addRow();
</script>`;
  return page(inner, 'XML 生成器');
}
