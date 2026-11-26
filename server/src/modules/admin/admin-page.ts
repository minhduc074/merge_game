export const ADMIN_PAGE = `<!doctype html>
<html lang="vi">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Bếp Ngon Admin</title>
<style>
:root{--bg:#fff7ed;--panel:#fffdf8;--ink:#4a2c1a;--muted:#8a6a52;--line:#ead7c3;--accent:#ff8a3d;--green:#42a66b;--red:#d94848;--blue:#4d7fd1}
*{box-sizing:border-box}body{margin:0;background:linear-gradient(180deg,#fff3df,#fff9f0);color:var(--ink);font-family:Inter,system-ui,-apple-system,Segoe UI,sans-serif}button,input,select,textarea{font:inherit}button{border:0;border-radius:10px;padding:8px 12px;background:var(--accent);color:white;font-weight:700;cursor:pointer}button.secondary{background:#efe0cf;color:var(--ink)}button.danger{background:var(--red)}button.small{padding:4px 8px;font-size:12px;border-radius:8px}button:disabled{opacity:.5;cursor:not-allowed}input,select,textarea{border:1px solid var(--line);border-radius:10px;padding:8px 10px;background:white;color:var(--ink)}textarea{width:100%;min-height:420px;font-family:ui-monospace,SFMono-Regular,Consolas,monospace;font-size:12px;line-height:1.45}.shell{max-width:1180px;margin:0 auto;padding:22px}.top{display:flex;align-items:center;justify-content:space-between;margin-bottom:16px}.brand{font-size:24px;font-weight:900}.muted{color:var(--muted);font-size:13px}.tabs{display:flex;gap:8px;margin:14px 0 18px;flex-wrap:wrap}.tabs button{background:#ead9c5;color:var(--ink)}.tabs button.active{background:var(--accent);color:white}.panel{background:rgba(255,253,248,.94);border:1px solid var(--line);border-radius:18px;padding:16px;box-shadow:0 8px 28px rgba(90,50,20,.08)}.grid{display:grid;gap:12px}.cards{grid-template-columns:repeat(auto-fit,minmax(160px,1fr))}.card{background:white;border:1px solid var(--line);border-radius:14px;padding:14px}.num{font-size:28px;font-weight:900;margin-top:4px}table{width:100%;border-collapse:collapse;background:white;border-radius:12px;overflow:hidden}th,td{padding:9px 10px;border-bottom:1px solid #f1e2d0;text-align:left;font-size:14px}th{background:#fff0dc;font-size:12px;text-transform:uppercase;letter-spacing:.04em;color:#7a5335}tr:last-child td{border-bottom:0}.row{display:flex;gap:10px;align-items:center;flex-wrap:wrap}.split{display:grid;grid-template-columns:280px 1fr;gap:14px}.list{display:flex;flex-direction:column;gap:8px}.config-item{padding:10px;border:1px solid var(--line);border-radius:12px;background:white;cursor:pointer}.config-item.active{outline:2px solid var(--accent)}.pill{display:inline-block;border-radius:999px;padding:2px 8px;font-size:11px;font-weight:800;background:#eee}.pill.published{background:#dff5e6;color:#177044}.pill.draft{background:#fff0bd;color:#806000}.pill.archived{background:#eee;color:#666}.error{white-space:pre-wrap;color:var(--red);background:#fff0f0;border:1px solid #f1c7c7;border-radius:10px;padding:10px;margin-top:10px}.ok{color:var(--green);font-weight:800}.login{max-width:390px;margin:12vh auto}.login input{width:100%;margin:7px 0}.actions{display:flex;gap:8px;flex-wrap:wrap;margin:10px 0}.json-status{font-size:13px;margin-left:auto}.mono{font-family:ui-monospace,SFMono-Regular,Consolas,monospace;font-size:12px}@media(max-width:800px){.split{grid-template-columns:1fr}.shell{padding:12px}th,td{font-size:12px;padding:7px}}
</style>
</head>
<body>
<div id="app"><div class="shell"><div class="panel">Đang tải…</div></div></div>
<script>
(function(){
var state={user:null,tab:'dashboard',board:'main',configs:[],config:null,players:[],playerSearch:''};
var app=document.getElementById('app');
function esc(v){return String(v==null?'':v).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]})}
function fmtDate(v){if(!v)return '—';try{return new Date(v).toLocaleString('vi-VN')}catch(e){return String(v)}}
function api(path,opts){
 opts=opts||{};opts.headers=Object.assign({'Content-Type':'application/json'},opts.headers||{});
 if(opts.body&&typeof opts.body!=='string')opts.body=JSON.stringify(opts.body);
 return fetch(path,opts).then(function(r){return r.text().then(function(text){
  var data=null;try{data=text?JSON.parse(text):null}catch(e){data={message:text}}
  if(!r.ok){if(r.status===401){renderLogin();throw new Error(data.message||'Unauthorized')}throw new Error(data.message||('HTTP '+r.status))}
  return data;
 })})
}
function shell(content){
 app.innerHTML='<div class="shell"><div class="top"><div><div class="brand">🍳 Bếp Ngon Admin</div><div class="muted">Xin chào, '+esc(state.user)+' · API /admin cùng origin</div></div><button class="secondary" id="logout">Đăng xuất</button></div>'+
 '<div class="tabs">'+[['dashboard','📊 Tổng quan'],['leaderboard','🏆 Bảng điểm'],['config','⚙️ Config game'],['players','👤 Người chơi'],['analytics','📈 Analytics']].map(function(t){return '<button data-tab="'+t[0]+'" class="'+(state.tab===t[0]?'active':'')+'">'+t[1]+'</button>'}).join('')+'</div>'+content+'</div>';
 document.getElementById('logout').onclick=function(){api('/api/admin/logout',{method:'POST'}).then(function(){renderLogin()})};
 Array.prototype.forEach.call(document.querySelectorAll('[data-tab]'),function(b){b.onclick=function(){state.tab=b.getAttribute('data-tab');render()}});
}
function renderLogin(){
 app.innerHTML='<div class="login panel"><h1>🍳 Bếp Ngon Admin</h1><p class="muted">Đăng nhập để quản lý leaderboard, config, save và analytics.</p><form id="loginForm"><input id="u" placeholder="Username" autocomplete="username" value=""><input id="p" placeholder="Password" type="password" autocomplete="current-password"><button style="width:100%">Đăng nhập</button><div id="err" class="error" style="display:none"></div></form></div>';
 document.getElementById('loginForm').onsubmit=function(ev){ev.preventDefault();api('/api/admin/login',{method:'POST',body:{username:document.getElementById('u').value,password:document.getElementById('p').value}}).then(function(r){state.user=r.username;render()}).catch(function(e){var el=document.getElementById('err');el.style.display='block';el.textContent=e.message})};
}
function render(){
 if(!state.user){renderLogin();return}
 if(state.tab==='dashboard')return renderDashboard();
 if(state.tab==='leaderboard')return renderLeaderboard();
 if(state.tab==='config')return renderConfig();
 if(state.tab==='players')return renderPlayers();
 return renderAnalytics();
}
function renderDashboard(){
 shell('<div class="panel">Đang tải tổng quan…</div>');
 api('/api/admin/dashboard/summary').then(function(d){
  var cards=[['👤 Người chơi',d.players],['🆕 Mới 7 ngày',d.newPlayers7d],['🟢 Active 7 ngày',d.activePlayers7d],['🏁 Score 24h',d.scoreSubmissions24h],['📈 Events 24h',d.analyticsEvents24h],['☁️ Cloud saves',d.cloudSaves],['🚫 Banned',d.bannedPlayers],['⚙️ Config live','v'+d.activeConfigVersion]];
  shell('<div class="grid cards">'+cards.map(function(c){return '<div class="card"><div class="muted">'+c[0]+'</div><div class="num">'+esc(c[1])+'</div></div>'}).join('')+'</div><div class="panel" style="margin-top:14px"><b>Gợi ý vận hành</b><p class="muted">Publish config trước giờ vàng, theo dõi score submissions bất thường và giữ ít nhất một bản config đã publish để rollback nhanh.</p></div>');
 }).catch(showError);
}
function renderLeaderboard(){
 shell('<div class="panel"><div class="row"><b>Board:</b><select id="board"><option value="main">⭐ Main</option><option value="mini">🍉 Thả trái</option><option value="block">🧱 Xếp khối</option></select><button class="secondary" id="reload">Tải lại</button></div><div id="lb">Đang tải…</div></div>');
 var sel=document.getElementById('board');sel.value=state.board;sel.onchange=function(){state.board=sel.value;renderLeaderboard()};
 document.getElementById('reload').onclick=renderLeaderboard;
 api('/api/admin/leaderboards/'+state.board+'?limit=200').then(function(rows){
  var html='<table><thead><tr><th>#</th><th>Người chơi</th><th>Best</th><th>Last</th><th>Config</th><th>Cập nhật</th><th></th></tr></thead><tbody>'+rows.map(function(r){return '<tr><td>'+r.rank+'</td><td>'+esc(r.nickname)+(r.banned?' <span class="pill archived">banned</span>':'')+'<div class="muted mono">'+esc(r.playerId)+'</div></td><td><b>'+r.score+'</b></td><td>'+r.lastScore+'</td><td>'+(r.configVersion||'—')+'</td><td>'+fmtDate(r.updatedAt)+'</td><td><button class="small danger" data-del-score="'+r.id+'">Xoá</button></td></tr>'}).join('')+'</tbody></table>';
  document.getElementById('lb').innerHTML=html;
  Array.prototype.forEach.call(document.querySelectorAll('[data-del-score]'),function(b){b.onclick=function(){if(!confirm('Xoá best score này? Lịch sử submit vẫn giữ.'))return;api('/api/admin/scores/'+b.getAttribute('data-del-score'),{method:'DELETE'}).then(renderLeaderboard).catch(showError)}});
 }).catch(showError);
}
function renderConfig(){
 shell('<div class="split"><div class="panel"><div class="row"><b>Versions</b><button class="small" id="copyActive">+ Copy active</button></div><div id="configList" class="list" style="margin-top:10px">Đang tải…</div></div><div class="panel"><div id="editor">Chọn một config version.</div></div></div>');
 document.getElementById('copyActive').onclick=function(){api('/api/admin/configs/copy-default',{method:'POST'}).then(function(){renderConfig()}).catch(showError)};
 api('/api/admin/configs').then(function(rows){
  state.configs=rows;
  var list=document.getElementById('configList');
  if(!rows.length){list.innerHTML='<div class="muted">Chưa có config. Bấm Copy active để tạo draft đầu tiên.</div>';return}
  list.innerHTML=rows.map(function(r){return '<div class="config-item" data-cfg="'+r.id+'"><b>v'+r.version+'</b> <span class="pill '+r.status.toLowerCase()+'">'+r.status+'</span><div class="muted">'+esc(r.note||'')+'<br>'+fmtDate(r.createdAt)+'</div></div>'}).join('');
  Array.prototype.forEach.call(document.querySelectorAll('[data-cfg]'),function(n){n.onclick=function(){loadConfig(n.getAttribute('data-cfg'))}});
  if(!state.config||!rows.some(function(r){return state.config&&r.id===state.config.id}))loadConfig(rows[0].id);else markActiveConfig();
 }).catch(showError);
}
function markActiveConfig(){
 Array.prototype.forEach.call(document.querySelectorAll('[data-cfg]'),function(n){n.classList.toggle('active',state.config&&n.getAttribute('data-cfg')===state.config.id)});
}
function loadConfig(id){
 api('/api/admin/configs/'+id).then(function(row){
  state.config=row;markActiveConfig();
  var editable=row.status==='DRAFT';
  document.getElementById('editor').innerHTML='<div class="row"><h2 style="margin:0">Config v'+row.version+'</h2><span class="pill '+row.status.toLowerCase()+'">'+row.status+'</span><span class="json-status muted" id="jsonStatus"></span></div><div class="muted">'+esc(row.note||'')+' · tạo '+fmtDate(row.createdAt)+' · publish '+fmtDate(row.publishedAt)+'</div><div class="actions"><button id="validate">Validate</button><button id="save" '+(editable?'':'disabled')+'>Lưu draft</button><button id="publish" '+(editable?'':'disabled')+'>Publish</button><button class="secondary" id="rollback">Rollback về bản này</button></div><textarea id="json" '+(editable?'':'readonly')+'></textarea><div id="cfgMsg"></div>';
  document.getElementById('json').value=JSON.stringify(row.payload,null,2);
  document.getElementById('validate').onclick=validateEditor;
  document.getElementById('save').onclick=saveEditor;
  document.getElementById('publish').onclick=publishEditor;
  document.getElementById('rollback').onclick=function(){if(!confirm('Tạo version mới copy config này và publish ngay?'))return;api('/api/admin/configs/'+row.id+'/rollback',{method:'POST',body:{note:'Rollback to v'+row.version}}).then(function(){renderConfig()}).catch(showError)};
 }).catch(showError);
}
function editorPayload(){
 var raw=document.getElementById('json').value;
 try{return JSON.parse(raw)}catch(e){throw new Error('JSON không hợp lệ: '+e.message)}
}
function cfgMessage(html,isErr){document.getElementById('cfgMsg').innerHTML='<div class="'+(isErr?'error':'ok')+'" style="margin-top:10px">'+html+'</div>'}
function validateEditor(){
 try{var payload=editorPayload();api('/api/admin/configs/validate',{method:'POST',body:{payload:payload}}).then(function(r){cfgMessage(r.valid?'✅ Config hợp lệ':esc(r.errors.join('\\n')),!r.valid)}).catch(function(e){cfgMessage(esc(e.message),true)})}catch(e){cfgMessage(esc(e.message),true)}
}
function saveEditor(){
 try{var payload=editorPayload();api('/api/admin/configs/'+state.config.id,{method:'PUT',body:{payload:payload,note:state.config.note}}).then(function(){cfgMessage('✅ Đã lưu draft',false);renderConfig()}).catch(function(e){cfgMessage(esc(e.message),true)})}catch(e){cfgMessage(esc(e.message),true)}
}
function publishEditor(){
 if(!confirm('Publish config này? Config published hiện tại sẽ archived.'))return;
 api('/api/admin/configs/'+state.config.id+'/publish',{method:'POST'}).then(function(){renderConfig()}).catch(function(e){cfgMessage(esc(e.message),true)});
}
function renderPlayers(){
 shell('<div class="panel"><div class="row"><input id="search" placeholder="Tìm nickname…" value="'+esc(state.playerSearch)+'"><button id="find">Tìm</button></div><div id="players" style="margin-top:12px">Đang tải…</div></div>');
 document.getElementById('find').onclick=function(){state.playerSearch=document.getElementById('search').value;renderPlayers()};
 api('/api/admin/players?limit=200&search='+encodeURIComponent(state.playerSearch)).then(function(rows){
  document.getElementById('players').innerHTML='<table><thead><tr><th>Người chơi</th><th>Best scores</th><th>Cloud save</th><th>Last seen</th><th></th></tr></thead><tbody>'+rows.map(function(p){
   var scores=(p.bestScores||[]).map(function(s){return s.board+': '+s.bestScore}).join('<br>')||'—';
   var save=p.cloudSave?('rev '+p.cloudSave.revision+'<br>'+fmtDate(p.cloudSave.updatedAt)):'—';
   return '<tr><td>'+esc(p.nickname)+(p.bannedAt?' <span class="pill archived">banned</span>':'')+'<div class="muted mono">'+esc(p.id)+'</div></td><td>'+scores+'</td><td>'+save+'</td><td>'+fmtDate(p.lastSeenAt)+'</td><td><button class="small '+(p.bannedAt?'':'danger')+'" data-ban="'+p.id+'" data-banned="'+(p.bannedAt?'1':'0')+'">'+(p.bannedAt?'Unban':'Ban')+'</button></td></tr>'
  }).join('')+'</tbody></table>';
  Array.prototype.forEach.call(document.querySelectorAll('[data-ban]'),function(b){b.onclick=function(){var action=b.getAttribute('data-banned')==='1'?'unban':'ban';if(!confirm(action+' player này?'))return;api('/api/admin/players/'+b.getAttribute('data-ban')+'/'+action,{method:'POST'}).then(renderPlayers).catch(showError)}});
 }).catch(showError);
}
function renderAnalytics(){
 shell('<div class="panel"><h2>Analytics 7 ngày</h2><div id="an">Đang tải…</div></div>');
 api('/api/admin/analytics/summary').then(function(d){
  var rows=d.events||[];
  document.getElementById('an').innerHTML=rows.length?'<table><thead><tr><th>Event</th><th>Count</th></tr></thead><tbody>'+rows.map(function(r){return '<tr><td class="mono">'+esc(r.eventName)+'</td><td>'+r.count+'</td></tr>'}).join('')+'</tbody></table>':'<div class="muted">Chưa có event nào.</div>';
 }).catch(showError);
}
function showError(e){shell('<div class="panel"><h2>Lỗi</h2><div class="error">'+esc(e.message||e)+'</div><button onclick="location.reload()">Tải lại</button></div>')}
api('/api/admin/me').then(function(r){state.user=r.username;render()}).catch(function(){renderLogin()});
})();
</script>
</body>
</html>`;
