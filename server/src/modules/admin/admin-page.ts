export const ADMIN_PAGE = `<!doctype html>
<html lang="vi">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Bếp Ngon Admin</title>
<style>
:root{
  --bg:#fff7ed;--panel:#fffdf8;--ink:#4a2c1a;--muted:#8a6a52;--line:#ead7c3;
  --accent:#ff8a3d;--accent-dark:#df681f;--green:#42a66b;--red:#d94848;--blue:#4d7fd1;
  --gold:#d99a22;--cream:#fff4e4;--shadow:0 10px 30px rgba(90,50,20,.08)
}
*{box-sizing:border-box}
body{margin:0;background:linear-gradient(180deg,#fff3df,#fff9f0);color:var(--ink);font-family:Inter,system-ui,-apple-system,Segoe UI,sans-serif}
button,input,select,textarea{font:inherit}
button{border:0;border-radius:10px;padding:8px 12px;background:var(--accent);color:white;font-weight:750;cursor:pointer;transition:transform .14s ease,box-shadow .14s ease,background .14s ease}
button:hover:not(:disabled){background:var(--accent-dark);transform:translateY(-1px);box-shadow:0 5px 14px rgba(223,104,31,.18)}
button.secondary{background:#efe0cf;color:var(--ink)}
button.secondary:hover:not(:disabled){background:#e5d1b9;box-shadow:none}
button.ghost{background:transparent;color:var(--accent-dark);border:1px solid #f0c39d}
button.ghost:hover:not(:disabled){background:#fff1e2;box-shadow:none}
button.danger{background:var(--red)}
button.small{padding:4px 8px;font-size:12px;border-radius:8px}
button.icon{width:32px;height:32px;padding:0;display:inline-grid;place-items:center;border-radius:9px}
button:disabled{opacity:.48;cursor:not-allowed;transform:none;box-shadow:none}
input,select,textarea{border:1px solid var(--line);border-radius:10px;padding:8px 10px;background:white;color:var(--ink);outline:none;transition:border-color .14s ease,box-shadow .14s ease}
input:focus,select:focus,textarea:focus{border-color:var(--accent);box-shadow:0 0 0 3px rgba(255,138,61,.16)}
input:disabled,select:disabled,textarea:disabled{background:#f6eee4;color:#947763;cursor:not-allowed}
textarea{width:100%;min-height:180px;font-family:ui-monospace,SFMono-Regular,Consolas,monospace;font-size:12px;line-height:1.45}
.shell{max-width:1240px;margin:0 auto;padding:22px}
.top{display:flex;align-items:center;justify-content:space-between;margin-bottom:16px;gap:14px}
.brand{font-size:24px;font-weight:900;letter-spacing:-.02em}
.muted{color:var(--muted);font-size:13px;line-height:1.45}
.mono{font-family:ui-monospace,SFMono-Regular,Consolas,monospace;font-size:12px}
.tabs{display:flex;gap:8px;margin:14px 0 18px;flex-wrap:wrap}
.tabs button{background:#ead9c5;color:var(--ink)}
.tabs button.active{background:var(--accent);color:white;box-shadow:0 5px 16px rgba(255,138,61,.22)}
.panel{background:rgba(255,253,248,.96);border:1px solid var(--line);border-radius:18px;padding:16px;box-shadow:var(--shadow)}
.grid{display:grid;gap:12px}
.cards{grid-template-columns:repeat(auto-fit,minmax(160px,1fr))}
.card{background:white;border:1px solid var(--line);border-radius:14px;padding:14px}
.num{font-size:28px;font-weight:900;margin-top:4px}
table{width:100%;border-collapse:collapse;background:white;border-radius:12px;overflow:hidden}
th,td{padding:9px 10px;border-bottom:1px solid #f1e2d0;text-align:left;font-size:14px;vertical-align:middle}
th{background:#fff0dc;font-size:12px;text-transform:uppercase;letter-spacing:.04em;color:#7a5335}
tr:last-child td{border-bottom:0}
.row{display:flex;gap:10px;align-items:center;flex-wrap:wrap}
.split{display:grid;grid-template-columns:292px minmax(0,1fr);gap:14px;align-items:start}
.list{display:flex;flex-direction:column;gap:8px}
.config-item{padding:10px;border:1px solid var(--line);border-radius:12px;background:#fff;cursor:pointer;transition:border-color .14s ease,transform .14s ease,box-shadow .14s ease}
.config-item:hover{border-color:#e7ae79;transform:translateY(-1px)}
.config-item.active{border-color:var(--accent);box-shadow:0 0 0 3px rgba(255,138,61,.13)}
.pill{display:inline-flex;align-items:center;gap:4px;padding:2px 7px;border-radius:999px;font-size:11px;font-weight:850;text-transform:uppercase;letter-spacing:.04em;vertical-align:middle}
.pill.draft{background:#fff0c2;color:#8a5b00}.pill.published{background:#dff5e7;color:#187344}.pill.archived{background:#eee4d9;color:#806552}.pill.dirty{background:#ffe1d6;color:#aa3f18;border:1px solid #f0a98d}
.actions{display:flex;gap:8px;margin:12px 0;flex-wrap:wrap}
.error{color:#a72f2f;background:#ffe8e8;border:1px solid #f0aaaa;padding:8px 10px;border-radius:10px;white-space:pre-wrap;line-height:1.45}
.ok{color:#237a49;background:#e4f7ec;border:1px solid #a8dfc0;padding:8px 10px;border-radius:10px;line-height:1.45}
.login{max-width:420px;margin:10vh auto;padding:28px}
.login input{width:100%;margin:7px 0;display:block}
.login button{margin-top:8px}
.empty{padding:28px;text-align:center;color:var(--muted);border:1px dashed var(--line);border-radius:14px;background:#fffaf2}

.editor-head{display:flex;justify-content:space-between;align-items:flex-start;gap:14px;border-bottom:1px solid var(--line);padding-bottom:12px;margin-bottom:12px}
.editor-head h2{margin:0;font-size:23px;letter-spacing:-.02em}
.editor-actions{display:flex;gap:8px;flex-wrap:wrap;justify-content:flex-end}
.note-row{display:grid;grid-template-columns:110px minmax(0,1fr);gap:10px;align-items:center;margin:8px 0 12px}
.note-row label{font-size:13px;font-weight:800;color:#7a5335}
.config-nav{display:flex;gap:8px;flex-wrap:wrap;margin:12px 0 14px;padding:8px;background:#fff1df;border:1px solid #efd6bc;border-radius:14px}
.config-nav button{background:transparent;color:#805236;border:1px solid transparent;padding:7px 10px;font-size:13px}
.config-nav button:hover:not(:disabled){background:#ffe5c9;box-shadow:none;transform:none}
.config-nav button.active{background:var(--accent);color:white;border-color:var(--accent-dark)}
.config-section{background:#fff;border:1px solid var(--line);border-radius:16px;padding:15px}
.section-head{display:flex;justify-content:space-between;gap:12px;align-items:flex-start;margin-bottom:12px}
.section-head h3{margin:0;font-size:18px}
.section-head p{margin:4px 0 0}
.form-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(190px,1fr));gap:12px}
.field label{display:block;font-size:12px;font-weight:850;color:#7a5335;margin-bottom:5px}
.field input,.field select{width:100%}
.hint{font-size:12px;color:var(--muted);margin-top:4px;line-height:1.35}
.subsection{margin-top:16px;padding-top:14px;border-top:1px dashed #ead7c3}
.subsection h4{margin:0 0 10px;font-size:15px}
.gen-picker{display:grid;grid-template-columns:repeat(auto-fit,minmax(145px,1fr));gap:8px;margin-bottom:14px}
.gen-pill{background:#fff7ed;color:var(--ink);border:1px solid var(--line);padding:9px;text-align:left;box-shadow:none}
.gen-pill:hover:not(:disabled){background:#fff0dc;transform:none;box-shadow:none}
.gen-pill.active{border-color:var(--accent);background:#ffe2c7;box-shadow:0 0 0 3px rgba(255,138,61,.12)}
.gen-pill .gen-name{display:block;font-weight:850;font-size:13px}
.gen-pill .gen-id{display:block;font-size:11px;color:var(--muted);margin-top:2px}
.gen-editor{border:1px solid #f0d8c1;background:#fffaf4;border-radius:14px;padding:13px}
.gen-editor-head{display:flex;justify-content:space-between;align-items:center;gap:10px;margin-bottom:12px}
.gen-editor-head h4{margin:0;font-size:16px}
.sum-badge{display:inline-flex;align-items:center;gap:5px;border-radius:999px;padding:5px 9px;font-size:12px;font-weight:850}
.sum-badge.good{background:#dff5e7;color:#187344}.sum-badge.bad{background:#ffe2df;color:#a7352d}
.spawn-table{display:flex;flex-direction:column;gap:8px}
.spawn-row{display:grid;grid-template-columns:minmax(190px,1.35fr) 92px minmax(130px,1fr) 34px;gap:8px;align-items:center;padding:8px;background:white;border:1px solid #f1e0ce;border-radius:12px}
.spawn-row input,.spawn-row select{width:100%}
.weight-track{height:10px;background:#f3e5d5;border-radius:999px;overflow:hidden;position:relative}
.weight-fill{height:100%;background:linear-gradient(90deg,#ff9d58,#ffc45f);border-radius:999px;min-width:0;transition:width .16s ease}
.weight-label{font-size:11px;color:var(--muted);margin-top:3px;text-align:right}
.bracket-table{display:flex;flex-direction:column;gap:8px}
.bracket-row{display:grid;grid-template-columns:minmax(130px,1fr) minmax(130px,1fr) 34px;gap:8px;align-items:center;padding:8px;background:#fff;border:1px solid #f1e0ce;border-radius:12px}
.bracket-row input{width:100%}
.inline-metric{display:flex;align-items:center;gap:8px;flex-wrap:wrap;padding:9px 10px;background:#fff7eb;border:1px solid #efd8be;border-radius:12px;margin-top:8px}
.metric-bar{display:flex;height:12px;overflow:hidden;border-radius:999px;background:#f2e4d2;margin-top:8px;border:1px solid #ead4bd}
.metric-seg{min-width:0;transition:width .16s ease}
.health-panel{margin-top:12px;border:1px solid #ead7c3;background:#fffaf3;border-radius:14px;padding:11px}
.health-panel.good{border-color:#b8e4ca;background:#f3fcf6}.health-panel.bad{border-color:#efb1aa;background:#fff5f3}
.health-title{font-weight:850;font-size:13px;margin-bottom:5px}
.health-list{margin:6px 0 0;padding-left:18px;color:#96362f;font-size:13px;line-height:1.45}
.health-ok{color:#237a49;font-size:13px}
.summary-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(220px,1fr));gap:10px}
.summary-card{background:#fffaf4;border:1px solid #efdbC5;border-radius:13px;padding:11px}
.summary-card b{display:block;margin-bottom:6px}
.summary-card ul{margin:0;padding-left:18px;color:#6f4b32;font-size:13px;line-height:1.5}
.warn{color:#9a5b08;background:#fff4d7;border:1px solid #efd18e;padding:8px 10px;border-radius:10px;font-size:13px;line-height:1.4}
@media(max-width:900px){.split{grid-template-columns:1fr}.editor-head{flex-direction:column}.editor-actions{justify-content:flex-start}.spawn-row{grid-template-columns:1fr 82px 34px}.spawn-row .weight-viz{grid-column:1/4}.note-row{grid-template-columns:1fr}.top{align-items:flex-start}}
</style>
</head>
<body>
<div id="app"><div class="shell"><div class="panel">Đang tải…</div></div></div>
<script>
(function(){
'use strict';
var state={user:null,tab:'dashboard',board:'main',configs:[],config:null,players:[],playerSearch:'',draft:null,draftNote:'',dirty:false,configSection:'generators',activeGen:'gen_fruit_1',flash:''};
var app=document.getElementById('app');

var ITEMS={
fruit_1:['🍒','Cherry'],fruit_2:['🍓','Dâu tây'],fruit_3:['🍊','Cam'],fruit_4:['🍎','Táo đỏ'],fruit_5:['🍇','Nho'],fruit_6:['🍉','Dưa hấu'],fruit_7:['🍍','Dứa'],fruit_8:['🥗','Đĩa trái cây'],fruit_9:['🍹','Sinh tố'],fruit_10:['🍰','Bánh trái cây'],fruit_11:['🎂','Bánh VIP'],
bakery_1:['🌾','Lúa mì'],bakery_2:['🍞','Bánh mì'],bakery_3:['🥐','Croissant'],bakery_4:['🍪','Cookie'],bakery_5:['🍩','Donut'],bakery_6:['🧁','Cupcake'],bakery_7:['🥞','Pancake'],bakery_8:['🍰','Bánh kem'],bakery_9:['🎂','Bánh sinh nhật'],bakery_10:['👑','Bánh hoàng gia'],
drink_1:['💧','Nước lọc'],drink_2:['🍵','Trà nóng'],drink_3:['🥛','Sữa tươi'],drink_4:['☕','Cà phê'],drink_5:['🧋','Trà sữa'],drink_6:['🥤','Nước ép'],drink_7:['🍹','Mocktail'],drink_8:['🍸','Party drink'],
viet_1:['🍚','Cơm trắng'],viet_2:['🥖','Bánh mì kẹp'],viet_3:['🍜','Phở bò'],viet_4:['🌯','Gỏi cuốn'],viet_5:['🍲','Bún chả'],viet_6:['🍱','Mâm cơm'],viet_7:['🏆','Tiệc VIP'],
rare_fruit_1:['🥭','Xoài cát'],rare_fruit_2:['🍑','Đào tiên'],rare_fruit_3:['🍈','Dưa lưới hoàng kim'],
rare_bakery_1:['🥧','Bánh tart trứng'],rare_bakery_2:['🧇','Waffle mật ong'],rare_bakery_3:['🍮','Bánh flan caramel'],
rare_drink_1:['🧉','Trà matcha'],rare_drink_2:['🍷','Sangria trái cây'],rare_drink_3:['🍾','Champagne VIP'],
rare_viet_1:['🌶️','Mì cay'],rare_viet_2:['🍤','Tôm sú nướng'],rare_viet_3:['🦞','Tôm hùm bơ tỏi']
};
var GENERATORS={
gen_fruit_1:['🧺','Giỏ trái cây'],gen_fruit_2:['🛒','Xe trái cây'],gen_fruit_3:['🚚','Xe tải trái cây'],
gen_bakery_1:['📦','Hộp bánh'],gen_bakery_2:['🏪','Tiệm bánh'],gen_bakery_3:['🏭','Nhà máy bánh'],
gen_drink_1:['🧃','Máy pha chế'],gen_drink_2:['🧊','Tủ lạnh mini'],gen_drink_3:['🍶','Quầy bar'],
gen_viet_1:['🍳','Bếp nhỏ'],gen_viet_2:['🔥','Bếp lớn'],gen_viet_3:['🏮','Nhà hàng']
};
var ITEM_IDS=Object.keys(ITEMS);
var GEN_IDS=Object.keys(GENERATORS);
var SECTIONS=[['meta','🧾 Thông tin'],['generators','🧺 Generators'],['economy','⚡ Energy'],['orders','📦 Orders'],['rewards','🏆 Thưởng'],['review','✅ Tổng kết']];

function esc(v){return String(v==null?'':v).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]})}
function fmtDate(v){if(!v)return '—';try{return new Date(v).toLocaleString('vi-VN')}catch(e){return String(v)}}
function clone(v){return JSON.parse(JSON.stringify(v))}
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
 Array.prototype.forEach.call(document.querySelectorAll('[data-tab]'),function(b){b.onclick=function(){var next=b.getAttribute('data-tab');if(state.tab==='config'&&next!=='config'&&state.dirty&&!confirm('Draft config có thay đổi chưa lưu. Rời khỏi tab và bỏ thay đổi?'))return;state.tab=next;render()}});
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
  if(state.tab!=='dashboard')return;
  var cards=[['👤 Người chơi',d.players],['🆕 Mới 7 ngày',d.newPlayers7d],['🟢 Active 7 ngày',d.activePlayers7d],['🏁 Score 24h',d.scoreSubmissions24h],['📈 Events 24h',d.analyticsEvents24h],['☁️ Cloud saves',d.cloudSaves],['🚫 Banned',d.bannedPlayers],['⚙️ Config live','v'+d.activeConfigVersion]];
  shell('<div class="grid cards">'+cards.map(function(c){return '<div class="card"><div class="muted">'+c[0]+'</div><div class="num">'+esc(c[1])+'</div></div>'}).join('')+'</div><div class="panel" style="margin-top:14px"><b>Gợi ý vận hành</b><p class="muted">Publish config trước giờ vàng, theo dõi score submissions bất thường và giữ ít nhất một bản config đã publish để rollback nhanh.</p></div>');
 }).catch(function(e){showErrorIf('dashboard',e)});
}
function renderLeaderboard(){
 shell('<div class="panel"><div class="row"><b>Board:</b><select id="board"><option value="main">⭐ Main</option><option value="mini">🍉 Thả trái</option><option value="block">🧱 Xếp khối</option></select><button class="secondary" id="reload">Tải lại</button></div><div id="lb">Đang tải…</div></div>');
 var sel=document.getElementById('board');sel.value=state.board;sel.onchange=function(){state.board=sel.value;renderLeaderboard()};
 document.getElementById('reload').onclick=renderLeaderboard;
 api('/api/admin/leaderboards/'+state.board+'?limit=200').then(function(rows){
  if(state.tab!=='leaderboard')return;
  var html='<table><thead><tr><th>#</th><th>Người chơi</th><th>Best</th><th>Last</th><th>Config</th><th>Cập nhật</th><th></th></tr></thead><tbody>'+rows.map(function(r){return '<tr><td>'+r.rank+'</td><td>'+esc(r.nickname)+(r.banned?' <span class="pill archived">banned</span>':'')+'<div class="muted mono">'+esc(r.playerId)+'</div></td><td><b>'+r.score+'</b></td><td>'+r.lastScore+'</td><td>'+(r.configVersion||'—')+'</td><td>'+fmtDate(r.updatedAt)+'</td><td><button class="small danger" data-del-score="'+r.id+'">Xoá</button></td></tr>'}).join('')+'</tbody></table>';
  document.getElementById('lb').innerHTML=html;
  Array.prototype.forEach.call(document.querySelectorAll('[data-del-score]'),function(b){b.onclick=function(){if(!confirm('Xoá best score này? Lịch sử submit vẫn giữ.'))return;api('/api/admin/scores/'+b.getAttribute('data-del-score'),{method:'DELETE'}).then(renderLeaderboard).catch(showError)}});
 }).catch(function(e){showErrorIf('leaderboard',e)});
}

function renderConfig(){
 shell('<div class="split"><div class="panel"><div class="row"><b>Versions</b><button class="small" id="copyActive">+ Copy active</button></div><div id="configList" class="list" style="margin-top:10px">Đang tải…</div></div><div class="panel"><div id="editor">Chọn một config version.</div></div></div>');
 document.getElementById('copyActive').onclick=function(){api('/api/admin/configs/copy-default',{method:'POST'}).then(function(){renderConfig()}).catch(showError)};
 api('/api/admin/configs').then(function(rows){
  if(state.tab!=='config')return;
  state.configs=rows;
  var list=document.getElementById('configList');
  if(!list)return;
  if(!rows.length){list.innerHTML='<div class="muted">Chưa có config. Bấm Copy active để tạo draft đầu tiên.</div>';return}
  list.innerHTML=rows.map(function(r){return '<div class="config-item" data-cfg="'+r.id+'"><b>v'+r.version+'</b> <span class="pill '+r.status.toLowerCase()+'">'+r.status+'</span><div class="muted">'+esc(r.note||'')+'<br>'+fmtDate(r.createdAt)+'</div></div>'}).join('');
  Array.prototype.forEach.call(document.querySelectorAll('[data-cfg]'),function(n){n.onclick=function(){if(state.dirty&&!confirm('Draft hiện tại có thay đổi chưa lưu. Bỏ thay đổi và mở version khác?'))return;loadConfig(n.getAttribute('data-cfg'))}});
  if(!state.config||!rows.some(function(r){return state.config&&r.id===state.config.id}))loadConfig(rows[0].id);else loadConfig(state.config.id);
 }).catch(function(e){showErrorIf('config',e)});
}
function markActiveConfig(){
 Array.prototype.forEach.call(document.querySelectorAll('[data-cfg]'),function(n){n.classList.toggle('active',state.config&&n.getAttribute('data-cfg')===state.config.id)});
}
function loadConfig(id){
 api('/api/admin/configs/'+id).then(function(row){
  if(state.tab!=='config')return;
  state.config=row;
  state.draft=normalizeConfigShape(clone(row.payload));
  state.draftNote=row.note||'';
  state.dirty=false;
  if(!GENERATORS[state.activeGen])state.activeGen=GEN_IDS[0];
  markActiveConfig();
  renderConfigEditor();
 }).catch(function(e){showErrorIf('config',e)});
}
function normalizeConfigShape(payload){
 payload=payload||{};
 payload.generators=payload.generators||{};
 payload.energy=payload.energy||{};
 payload.generatorTiming=payload.generatorTiming||{};
 payload.orders=payload.orders||{};
 payload.orders.countWeights=payload.orders.countWeights||{};
 if(payload.orders.tierWindow==null)payload.orders.tierWindow=3;
 if(payload.orders.tierLevelSpan==null)payload.orders.tierLevelSpan=30;
 delete payload.orders.baseOrderTier;
 delete payload.orders.maxOrderTier;
 delete payload.orders.familyUnlockLevel;
 payload.xp=payload.xp||{};
 payload.rewards=payload.rewards||{};
 payload.rewards.miniEnergyBrackets=payload.rewards.miniEnergyBrackets||[];
 payload.rewards.blockEnergyBrackets=payload.rewards.blockEnergyBrackets||[];
 GEN_IDS.forEach(function(id){if(!payload.generators[id])payload.generators[id]={maxCharges:6,spawnTable:[{itemId:familyOfGen(id)+'_1',weight:100}]};payload.generators[id].spawnTable=payload.generators[id].spawnTable||[]});
 return payload;
}
function isEditable(){return state.config&&state.config.status==='DRAFT'}
function disabledAttr(){return isEditable()?'':'disabled'}
function dirtyPill(){return state.dirty?' <span class="pill dirty">chưa lưu</span>':''}
function renderConfigEditor(){
 var row=state.config;
 if(!row)return;
 var editable=isEditable();
 var flash=state.flash||'';state.flash='';
 document.getElementById('editor').innerHTML=
 '<div class="editor-head"><div><h2>Config v'+row.version+' <span class="pill '+row.status.toLowerCase()+'">'+row.status+'</span><span id="dirtyPill">'+dirtyPill()+'</span></h2><div class="muted">Tạo '+fmtDate(row.createdAt)+' · publish '+fmtDate(row.publishedAt)+' · bởi '+esc(row.createdBy||'—')+'</div></div>'+
 '<div class="editor-actions"><button class="secondary" id="validate">Validate</button><button id="save" '+(editable?'':'disabled')+'>Lưu draft</button><button id="publish" '+(editable?'':'disabled')+'>Publish</button><button class="ghost" id="rollback">Rollback về bản này</button></div></div>'+
 '<div class="note-row"><label for="cfgNote">Ghi chú version</label><input id="cfgNote" placeholder="VD: giảm nhẹ spawn rare sau event" value="'+esc(state.draftNote)+'" '+(editable?'':'disabled')+'></div>'+
 '<div class="config-nav">'+SECTIONS.map(function(s){return '<button data-section="'+s[0]+'" class="'+(state.configSection===s[0]?'active':'')+'">'+s[1]+'</button>'}).join('')+'</div>'+
 '<div id="configBody"></div><div id="cfgHealth"></div><div id="cfgMsg">'+(flash?'<div class="ok" style="margin-top:10px">'+esc(flash)+'</div>':'')+'</div>';
 document.getElementById('validate').onclick=validateEditor;
 document.getElementById('save').onclick=saveEditor;
 document.getElementById('publish').onclick=publishEditor;
 document.getElementById('rollback').onclick=function(){if(!confirm('Tạo version mới copy config này và publish ngay?'))return;api('/api/admin/configs/'+row.id+'/rollback',{method:'POST',body:{note:'Rollback to v'+row.version}}).then(function(){renderConfig()}).catch(showError)};
 document.getElementById('cfgNote').oninput=function(e){state.draftNote=e.target.value;markDirty()};
 Array.prototype.forEach.call(document.querySelectorAll('[data-section]'),function(b){b.onclick=function(){state.configSection=b.getAttribute('data-section');renderConfigEditor()}});
 refreshConfigBody();
}
function refreshConfigBody(){
 var body=document.getElementById('configBody');
 if(!body||!state.draft)return;
 if(state.configSection==='meta')body.innerHTML=metaSectionHtml();
 else if(state.configSection==='generators')body.innerHTML=generatorsSectionHtml();
 else if(state.configSection==='economy')body.innerHTML=economySectionHtml();
 else if(state.configSection==='orders')body.innerHTML=ordersSectionHtml();
 else if(state.configSection==='rewards')body.innerHTML=rewardsSectionHtml();
 else body.innerHTML=reviewSectionHtml();
 bindConfigBody();
 updateConfigHealth();
}
function field(label,path,value,opts){
 opts=opts||{};
 var unit=opts.unit?'<span class="hint">'+opts.unit+'</span>':'';
 var hint=opts.hint?'<div class="hint">'+opts.hint+'</div>':'';
 return '<div class="field"><label>'+label+'</label><input type="number" data-path="'+path+'" value="'+esc(value==null?'':value)+'" min="'+(opts.min==null?0:opts.min)+'" max="'+(opts.max==null?1000000:opts.max)+'" step="'+(opts.step||1)+'" '+disabledAttr()+'>'+unit+hint+'</div>';
}
function msField(label,path,value,opts){
 opts=opts||{};
 var seconds=value==null?'':Math.round(Number(value)/1000);
 var hint=(opts.hint?opts.hint+' · ':'')+'Lưu dưới dạng '+(value==null?'—':esc(value))+' ms';
 return field(label,path+'.seconds',seconds,{min:opts.min,max:opts.max,hint:hint});
}
function metaSectionHtml(){
 var p=state.draft;
 return '<div class="config-section"><div class="section-head"><div><h3>Thông tin version</h3><p class="muted">Các trường meta kiểm soát schema và app tối thiểu được nhận config.</p></div></div><div class="form-grid">'+
 field('Schema version','schemaVersion',p.schemaVersion,{min:1,max:100,hint:'Tăng khi đổi cấu trúc payload'})+
 field('Min app versionCode','minAppVersionCode',p.minAppVersionCode,{min:1,max:100000,hint:'App thấp hơn version này cần ignore config mới'})+
 '</div><div class="subsection"><h4>Cách dùng an toàn</h4><div class="warn">Tạo draft → chỉnh bằng form → Validate → Publish. Config đang publish sẽ tự archived, rollback tạo version mới thay vì ghi đè lịch sử.</div></div></div>';
}
function generatorLabel(id){var g=GENERATORS[id];return g?g[0]+' '+g[1]:id}
function itemLabel(id){var it=ITEMS[id];return it?it[0]+' '+it[1]:id}
function tierOf(id){var m=String(id).match(/_([0-9]+)$/);return m?Number(m[1]):0}
function familyOfGen(id){return id.replace(/^gen_/,'').replace(/_[0-9]+$/,'')}
function generatorsSectionHtml(){
 var gens=state.draft.generators||{};
 if(!GENERATORS[state.activeGen])state.activeGen=GEN_IDS[0];
 var picker=GEN_IDS.map(function(id){
  var g=GENERATORS[id];
  return '<button class="gen-pill '+(id===state.activeGen?'active':'')+'" data-gen="'+id+'"><span class="gen-name">'+g[0]+' '+g[1]+'</span><span class="gen-id">'+id+'</span></button>';
 }).join('');
 var gen=gens[state.activeGen]||{maxCharges:0,spawnTable:[]};
 var rows=(gen.spawnTable||[]).map(function(entry,i){
  var weight=Number(entry.weight)||0;
  return '<div class="spawn-row" data-row="'+i+'"><select data-path="generators.'+state.activeGen+'.spawnTable.'+i+'.itemId" data-item-select="1" '+disabledAttr()+'>'+ITEM_IDS.map(function(id){return '<option value="'+id+'" '+(id===entry.itemId?'selected':'')+'>'+esc(itemLabel(id))+' · t'+tierOf(id)+'</option>'}).join('')+'</select>'+
  '<input type="number" min="0" max="100" step="1" data-path="generators.'+state.activeGen+'.spawnTable.'+i+'.weight" data-spawn-weight="1" value="'+esc(entry.weight)+'" '+disabledAttr()+'>'+
  '<div class="weight-viz"><div class="weight-track"><div class="weight-fill" data-weight-bar="'+i+'" style="width:'+Math.max(0,Math.min(100,weight))+'%"></div></div><div class="weight-label" data-weight-label="'+i+'">'+weight+'%</div></div>'+
  '<button class="danger icon" data-del-spawn="'+i+'" title="Xoá item" '+disabledAttr()+'>×</button></div>';
 }).join('');
 var rareId='rare_'+familyOfGen(state.activeGen)+'_1';
 var canRare=ITEMS[rareId]?'data-add-rare="'+rareId+'"':'disabled';
 return '<div class="config-section"><div class="section-head"><div><h3>Generator spawn tables</h3><p class="muted">Chọn generator, chỉnh số lượt sạc và tỉ lệ spawn. Tổng weight của mỗi generator bắt buộc bằng 100.</p></div></div>'+
 '<div class="gen-picker">'+picker+'</div><div class="gen-editor"><div class="gen-editor-head"><h4>'+esc(generatorLabel(state.activeGen))+'</h4><span class="sum-badge" data-sum-badge>—</span></div><div class="form-grid">'+
 field('Số lượt sạc tối đa','generators.'+state.activeGen+'.maxCharges',gen.maxCharges,{min:1,max:60,hint:'Mặc định bundle là 6/12/18 theo tier gen; app sẽ dùng giá trị đã publish'})+
 '</div><div class="subsection"><div class="row" style="justify-content:space-between"><h4 style="margin:0">Bảng spawn</h4><div class="row"><button class="small secondary" data-add-row '+disabledAttr()+'>+ Thêm item</button><button class="small ghost" '+canRare+' '+disabledAttr()+'>+ Rare phù hợp</button></div></div><div class="spawn-table" style="margin-top:10px">'+rows+'</div><div class="hint" style="margin-top:8px">Convention hiện tại: tier 1 weight cao nhất, gen tier N chỉ nên spawn tới tier N+1, rare thường weight 2.</div></div></div></div>';
}
function economySectionHtml(){
 var p=state.draft;
 return '<div class="config-section"><div class="section-head"><div><h3>Energy và timing generator</h3><p class="muted">Nhập theo giây cho dễ đọc; hệ thống tự đổi sang milliseconds khi lưu.</p></div></div><div class="form-grid">'+
 field('Energy cap','energy.cap',p.energy.cap,{min:10,max:1000,hint:'Trần regen tự nhiên'})+
 field('Energy mỗi spawn','energy.perSpawn',p.energy.perSpawn,{min:0,max:20,hint:'0 nghĩa là spawn không tốn energy'})+
 msField('Hồi 1 energy mỗi','energy.regenMs',p.energy.regenMs,{min:5,max:3600,hint:'giây/lần hồi'})+
 msField('Cooldown khi gen hết lượt','generatorTiming.cooldownMs',p.generatorTiming.cooldownMs,{min:5,max:3600,hint:'giây'})+
 msField('Hồi 1 lượt gen mỗi','generatorTiming.regenMs',p.generatorTiming.regenMs,{min:5,max:3600,hint:'giây'})+
 '</div><div class="inline-metric"><span>⚡</span><b id="energyPreview">—</b><span class="muted">thời gian nạp đầy energy cap bằng regen tự nhiên</span></div></div>';
}
function ordersSectionHtml(){
 var p=state.draft;
 return '<div class="config-section"><div class="section-head"><div><h3>Order pacing và độ khó</h3><p class="muted">Kiểm soát nhịp xuất hiện order, số món trong order và tier item được hỏi.</p></div></div><div class="form-grid">'+
 msField('Khoảng spawn tối thiểu','orders.intervalMinMs',p.orders.intervalMinMs,{min:2,max:300,hint:'giây'})+
 msField('Khoảng spawn tối đa','orders.intervalMaxMs',p.orders.intervalMaxMs,{min:2,max:600,hint:'giây'})+
 field('Tối đa order active','orders.maxActive',p.orders.maxActive,{min:1,max:10})+
 field('Tier window','orders.tierWindow',p.orders.tierWindow,{min:1,max:10,hint:'max = min + giá trị này; cửa sổ trượt theo chiều dài chuỗi'})+
 field('Tier level span','orders.tierLevelSpan',p.orders.tierLevelSpan,{min:5,max:100,hint:'min tier = level × chainLen / span + 1 — span nhỏ leo nhanh'})+
 field('Rare order weight','orders.rareWeight',p.orders.rareWeight,{min:0,max:100,hint:'Weight flat cho mỗi rare item'})+
 '</div><div class="inline-metric"><span>🎯</span><span data-tier-preview>—</span></div>'+
 '<div class="subsection"><h4>Tỉ lệ số món / order <span class="sum-badge" data-count-badge>—</span></h4><div class="form-grid">'+
 field('1 món','orders.countWeights.one',p.orders.countWeights.one,{min:0,max:100})+
 field('2 món','orders.countWeights.two',p.orders.countWeights.two,{min:0,max:100})+
 field('3 món','orders.countWeights.three',p.orders.countWeights.three,{min:0,max:100})+
 '</div><div class="metric-bar"><div class="metric-seg" data-count-seg="one" style="background:#ff9d58"></div><div class="metric-seg" data-count-seg="two" style="background:#ffc45f"></div><div class="metric-seg" data-count-seg="three" style="background:#79c983"></div></div></div><div class="hint" style="margin-top:10px">Order chỉ hỏi family có gen machine trên bàn — viet/drink chưa ra máy thì không bao giờ bị đòi.</div></div>';
}
function tierWindowRange(chainLen,level,span,win){
 if(chainLen<1)return null;
 var rawMin=Math.floor(level*chainLen/Math.max(1,span))+1;
 if(chainLen>win){
  var mn=Math.max(1,Math.min(chainLen-win,rawMin));
  return [mn,mn+win];
 }
 return [Math.max(1,Math.min(chainLen,rawMin)),chainLen];
}
var ORDER_CHAINS=[['Trái cây',11],['Bánh',10],['Đồ uống',8],['Món Việt',7],['Rare',3]];
function bracketRows(kind){
 var rows=(state.draft.rewards[kind]||[]).map(function(row,i){
  return '<div class="bracket-row"><input type="number" min="0" max="1000000" step="1" data-path="rewards.'+kind+'.'+i+'.0" value="'+esc(row[0])+'" placeholder="Từ điểm" '+disabledAttr()+'><input type="number" min="1" max="10000" step="1" data-path="rewards.'+kind+'.'+i+'.1" value="'+esc(row[1])+'" placeholder="Số điểm cho 1⚡" '+disabledAttr()+'><button class="danger icon" data-del-bracket="'+kind+':'+i+'" '+disabledAttr()+'>×</button></div>';
 }).join('');
 return rows||'<div class="empty">Chưa có bracket nào.</div>';
}
function bracketSection(kind,title,examples){
 return '<div class="subsection"><div class="row" style="justify-content:space-between"><h4 style="margin:0">'+title+'</h4><button class="small secondary" data-add-bracket="'+kind+'" '+disabledAttr()+'>+ Thêm mốc</button></div><div class="muted" style="margin:6px 0 8px">Mỗi hàng: từ mốc điểm → cần bao nhiêu điểm cho 1⚡. Mốc phải tăng dần.</div><div class="bracket-table">'+bracketRows(kind)+'</div><div class="inline-metric"><span>Ước tính:</span><span data-bracket-preview="'+kind+'">—</span></div><div class="hint">'+examples+'</div></div>';
}
function rewardsSectionHtml(){
 var p=state.draft;
 return '<div class="config-section"><div class="section-head"><div><h3>XP và phần thưởng</h3><p class="muted">Chỉnh đường cong level, combo/QTE và energy thưởng từ minigame.</p></div></div><div class="subsection" style="border-top:0;padding-top:0;margin-top:0"><h4>XP cần để lên level</h4><div class="form-grid">'+
 field('Base XP','xp.base',p.xp.base,{min:1,max:10000})+
 field('Hệ số tuyến tính','xp.linear',p.xp.linear,{min:0,max:10000})+
 field('Hệ số bậc hai','xp.quadratic',p.xp.quadratic,{min:0,max:10000,hint:'Công thức: base + level×linear + level²×quadratic'})+
 '</div><div class="inline-metric"><span>📈</span><span data-xp-preview>—</span></div></div><div class="subsection"><h4>Combo và QTE</h4><div class="form-grid">'+
 msField('Cửa sổ combo','rewards.comboWindowMs',p.rewards.comboWindowMs,{min:5,max:600,hint:'giây'})+
 field('Combo tối thiểu','rewards.comboMin',p.rewards.comboMin,{min:2,max:20})+
 field('QTE energy','rewards.qteEnergy',p.rewards.qteEnergy,{min:0,max:500})+
 field('QTE coins','rewards.qteCoins',p.rewards.qteCoins,{min:0,max:5000})+
 '</div></div>'+
 bracketSection('miniEnergyBrackets','🍉 Energy thưởng Thả trái','Ví dụ mặc định: 0→10, 100→25, 300→40, 700→60; điểm càng cao mỗi energy càng đắt.')+
 bracketSection('blockEnergyBrackets','🧱 Energy thưởng Xếp khối','Ví dụ mặc định: 0→40, 800→65, 2500→100, 7000→160 vì điểm xếp khối thường cao hơn.')+
 '</div>';
}
function reviewSectionHtml(){
 return '<div class="config-section"><div class="section-head"><div><h3>Tổng kết trước khi publish</h3><p class="muted">Đọc nhanh tác động chính thay vì phải tự parse JSON.</p></div></div><div class="summary-grid">'+
 '<div class="summary-card"><b>🧺 Generators</b><ul><li>'+GEN_IDS.length+' generator được cấu hình</li><li data-review-genbad>—</li><li>Weight mỗi bảng phải đủ 100%</li></ul></div>'+
 '<div class="summary-card"><b>⚡ Energy</b><ul><li>Cap: '+esc(state.draft.energy.cap)+'</li><li>Cost/spawn: '+esc(state.draft.energy.perSpawn)+'</li><li data-review-energy>—</li></ul></div>'+
 '<div class="summary-card"><b>📦 Orders</b><ul><li>Tối đa '+esc(state.draft.orders.maxActive)+' order active</li><li>Window: min+'+esc(state.draft.orders.tierWindow)+' · span '+esc(state.draft.orders.tierLevelSpan)+'</li><li data-review-orderpace>—</li></ul></div>'+
 '<div class="summary-card"><b>🏆 Rewards</b><ul><li data-review-xp>—</li><li data-review-mini>—</li><li data-review-block>—</li></ul></div>'+
 '</div><div class="subsection"><h4>Checklist vận hành</h4><div class="warn">Nên validate sau khi chỉnh, publish ngoài giờ cao điểm nếu thay đổi lớn, và ghi chú rõ lý do balance để rollback dễ hiểu.</div></div></div>';
}

function bindConfigBody(){
 Array.prototype.forEach.call(document.querySelectorAll('[data-path]'),function(el){
  el.oninput=function(){
   var path=el.getAttribute('data-path');
   var isSeconds=path.slice(-8)==='.seconds';
   if(isSeconds)path=path.slice(0,-8);
   var raw=el.value;
   var n=el.hasAttribute('data-item-select')?raw:(raw===''?NaN:Number(raw));
   if(isSeconds)n=Math.round(n*1000);
   setPath(state.draft,path,n);
   markDirty();
   if(el.hasAttribute('data-spawn-weight'))updateSpawnVisuals();
   if(el.hasAttribute('data-item-select'))updateSpawnVisuals();
   updateConfigHealth();
  };
  el.onchange=el.oninput;
 });
 Array.prototype.forEach.call(document.querySelectorAll('[data-gen]'),function(b){b.onclick=function(){state.activeGen=b.getAttribute('data-gen');refreshConfigBody()}});
 Array.prototype.forEach.call(document.querySelectorAll('[data-del-spawn]'),function(b){b.onclick=function(){var i=Number(b.getAttribute('data-del-spawn'));state.draft.generators[state.activeGen].spawnTable.splice(i,1);markDirty();refreshConfigBody()}});
 var add=document.querySelector('[data-add-row]');
 if(add)add.onclick=function(){state.draft.generators[state.activeGen].spawnTable.push({itemId:ITEM_IDS[0],weight:0});markDirty();refreshConfigBody()};
 var addRare=document.querySelector('[data-add-rare]');
 if(addRare)addRare.onclick=function(){var id=addRare.getAttribute('data-add-rare');state.draft.generators[state.activeGen].spawnTable.push({itemId:id,weight:2});markDirty();refreshConfigBody()};
 Array.prototype.forEach.call(document.querySelectorAll('[data-del-bracket]'),function(b){b.onclick=function(){var parts=b.getAttribute('data-del-bracket').split(':');state.draft.rewards[parts[0]].splice(Number(parts[1]),1);markDirty();refreshConfigBody()}});
 Array.prototype.forEach.call(document.querySelectorAll('[data-add-bracket]'),function(b){b.onclick=function(){var kind=b.getAttribute('data-add-bracket');var rows=state.draft.rewards[kind];var last=rows.length?Number(rows[rows.length-1][0])||0:0;rows.push([last+100,kind==='miniEnergyBrackets'?40:100]);markDirty();refreshConfigBody()}});
}
function setPath(obj,path,value){
 var keys=String(path).split('.');
 var cur=obj;
 for(var i=0;i<keys.length-1;i++){
  var key=keys[i];
  var nextKey=keys[i+1];
  if(cur[key]==null)cur[key]=/^[0-9]+$/.test(nextKey)?[]:{};
  cur=cur[key];
 }
 cur[keys[keys.length-1]]=value;
}
function markDirty(){
 if(!isEditable())return;
 state.dirty=true;
 var el=document.getElementById('dirtyPill');
 if(el)el.innerHTML=dirtyPill();
}
function isInt(v,min,max){return Number.isInteger(v)&&v>=min&&v<=max}
function validateClientConfig(p){
 var errors=[];
 if(!isInt(p.schemaVersion,1,100))errors.push('Schema version phải là số nguyên 1..100');
 if(p.minAppVersionCode!==undefined&&!isInt(p.minAppVersionCode,1,100000))errors.push('Min app versionCode phải là số nguyên 1..100000');
 GEN_IDS.forEach(function(id){
  var g=p.generators&&p.generators[id];
  if(!g){errors.push('Thiếu generator '+id);return}
  if(!isInt(g.maxCharges,1,60))errors.push(generatorLabel(id)+': số lượt sạc phải 1..60');
  if(!Array.isArray(g.spawnTable)||!g.spawnTable.length){errors.push(generatorLabel(id)+': spawn table không được trống');return}
  var sum=0;
  g.spawnTable.forEach(function(e,i){
   if(!ITEMS[e.itemId])errors.push(generatorLabel(id)+' dòng '+(i+1)+': item không hợp lệ');
   if(!isInt(e.weight,0,100))errors.push(generatorLabel(id)+' dòng '+(i+1)+': weight phải 0..100');else sum+=e.weight;
  });
  if(sum!==100)errors.push(generatorLabel(id)+': tổng weight đang '+sum+'%, cần đúng 100%');
 });
 if(!isInt(p.energy.cap,10,1000))errors.push('Energy cap phải 10..1000');
 if(!isInt(p.energy.perSpawn,0,20))errors.push('Energy/spawn phải 0..20');
 if(!isInt(p.energy.regenMs,5000,3600000))errors.push('Energy regen phải 5s..1h');
 if(!isInt(p.generatorTiming.cooldownMs,5000,3600000))errors.push('Gen cooldown phải 5s..1h');
 if(!isInt(p.generatorTiming.regenMs,5000,3600000))errors.push('Gen regen phải 5s..1h');
 if(!isInt(p.orders.intervalMinMs,2000,300000))errors.push('Order interval min phải 2s..5m');
 if(!isInt(p.orders.intervalMaxMs,2000,600000))errors.push('Order interval max phải 2s..10m');
 if(isInt(p.orders.intervalMinMs,2000,300000)&&isInt(p.orders.intervalMaxMs,2000,600000)&&p.orders.intervalMinMs>p.orders.intervalMaxMs)errors.push('Order interval min không được lớn hơn max');
 if(!isInt(p.orders.maxActive,1,10))errors.push('Max active orders phải 1..10');
 if(!isInt(p.orders.tierWindow,1,10))errors.push('Tier window phải 1..10');
 if(!isInt(p.orders.tierLevelSpan,5,100))errors.push('Tier level span phải 5..100');
 if(!isInt(p.orders.rareWeight,0,100))errors.push('Rare order weight phải 0..100');
 var cw=p.orders.countWeights||{};
 if(!isInt(cw.one,0,100)||!isInt(cw.two,0,100)||!isInt(cw.three,0,100))errors.push('Order count weights phải 0..100');
 else if(cw.one+cw.two+cw.three!==100)errors.push('Order count weights đang '+(cw.one+cw.two+cw.three)+'%, cần đúng 100%');
 if(!isInt(p.xp.base,1,10000))errors.push('XP base phải 1..10000');
 if(!isInt(p.xp.linear,0,10000))errors.push('XP linear phải 0..10000');
 if(!isInt(p.xp.quadratic,0,10000))errors.push('XP quadratic phải 0..10000');
 if(!isInt(p.rewards.comboWindowMs,5000,600000))errors.push('Combo window phải 5s..10m');
 if(!isInt(p.rewards.comboMin,2,20))errors.push('Combo min phải 2..20');
 if(!isInt(p.rewards.qteEnergy,0,500))errors.push('QTE energy phải 0..500');
 if(!isInt(p.rewards.qteCoins,0,5000))errors.push('QTE coins phải 0..5000');
 validateBracketsClient(p.rewards.miniEnergyBrackets,'Thả trái',errors);
 validateBracketsClient(p.rewards.blockEnergyBrackets,'Xếp khối',errors);
 return errors;
}
function validateBracketsClient(rows,label,errors){
 if(!Array.isArray(rows)||!rows.length){errors.push(label+': cần ít nhất một bracket');return}
 var prev=-1;
 rows.forEach(function(row,i){
  if(!Array.isArray(row)||row.length!==2){errors.push(label+' dòng '+(i+1)+': bracket phải gồm [mốc điểm, rate]');return}
  if(!isInt(row[0],0,1000000)||!isInt(row[1],1,10000))errors.push(label+' dòng '+(i+1)+': giá trị ngoài range');
  if(row[0]<=prev)errors.push(label+' dòng '+(i+1)+': mốc điểm phải tăng dần');
  prev=row[0];
 });
}
function updateSpawnVisuals(){
 var gen=state.draft.generators[state.activeGen];
 if(!gen)return;
 var sum=0;
 (gen.spawnTable||[]).forEach(function(e,i){
  var w=Number(e.weight)||0;sum+=w;
  var bar=document.querySelector('[data-weight-bar="'+i+'"]');if(bar)bar.style.width=Math.max(0,Math.min(100,w))+'%';
  var label=document.querySelector('[data-weight-label="'+i+'"]');if(label)label.textContent=(Number.isFinite(w)?w:0)+'%';
 });
 Array.prototype.forEach.call(document.querySelectorAll('[data-sum-badge]'),function(el){
  el.textContent='Tổng '+sum+'% / 100%';
  el.className='sum-badge '+(sum===100?'good':'bad');
 });
}
function updateCountVisuals(){
 var cw=state.draft.orders.countWeights||{};
 var sum=(Number(cw.one)||0)+(Number(cw.two)||0)+(Number(cw.three)||0);
 var badge=document.querySelector('[data-count-badge]');
 if(badge){badge.textContent='Tổng '+sum+'% / 100%';badge.className='sum-badge '+(sum===100?'good':'bad')}
 ['one','two','three'].forEach(function(k){var seg=document.querySelector('[data-count-seg="'+k+'"]');if(seg)seg.style.width=Math.max(0,Math.min(100,Number(cw[k])||0))+'%'});
}
function bracketEnergy(rows,score){
 var energy=0;
 for(var i=0;i<rows.length;i++){
  var from=Number(rows[i][0])||0;
  var rate=Number(rows[i][1])||1;
  var to=i+1<rows.length?Number(rows[i+1][0])||0:2147483647;
  if(score<=from)break;
  energy+=Math.floor((Math.min(score,to)-from)/Math.max(1,rate));
 }
 return energy;
}
function updateDerivedText(){
 var p=state.draft;
 var ep=document.getElementById('energyPreview');
 if(ep){
  var regen=Number(p.energy.regenMs)||0;
  var cap=Number(p.energy.cap)||0;
  ep.textContent=regen>0?('~'+Math.ceil(cap*regen/60000)+' phút'):'—';
 }
 var xp=document.querySelector('[data-xp-preview]');
 if(xp)xp.textContent='Lv10 cần '+xpNeed(p,10)+' XP · Lv20 cần '+xpNeed(p,20)+' XP · Lv30 cần '+xpNeed(p,30)+' XP';
 Array.prototype.forEach.call(document.querySelectorAll('[data-bracket-preview]'),function(el){
  var kind=el.getAttribute('data-bracket-preview');
  var rows=p.rewards[kind]||[];
  el.textContent='1.000đ ≈ '+bracketEnergy(rows,1000)+'⚡ · 5.000đ ≈ '+bracketEnergy(rows,5000)+'⚡';
 });
 var genBad=document.querySelector('[data-review-genbad]');
 if(genBad){
  var bad=GEN_IDS.filter(function(id){var g=p.generators[id];var sum=(g.spawnTable||[]).reduce(function(a,e){return a+(Number(e.weight)||0)},0);return sum!==100});
  genBad.textContent=bad.length?('Cần sửa: '+bad.map(generatorLabel).join(', ')):'Tất cả bảng spawn đều đủ 100%';
 }
 var re=document.querySelector('[data-review-energy]');
 if(re)re.textContent='Regen 1⚡ mỗi '+Math.round((Number(p.energy.regenMs)||0)/1000)+'s';
 var tp=document.querySelector('[data-tier-preview]');
 if(tp){
  var span=Number(p.orders.tierLevelSpan)||30,win=Number(p.orders.tierWindow)||3;
  tp.innerHTML=ORDER_CHAINS.map(function(c){
   return '<b>'+c[0]+'</b> '+[1,10,20,30].map(function(lv){var r=tierWindowRange(c[1],lv,span,win);return 'Lv'+lv+' t'+r[0]+'-'+r[1]}).join(' · ');
  }).join('<br>');
 }
 var ro=document.querySelector('[data-review-orderpace]');
 if(ro)ro.textContent='Nhịp order '+Math.round((Number(p.orders.intervalMinMs)||0)/1000)+'s → '+Math.round((Number(p.orders.intervalMaxMs)||0)/1000)+'s';
 var rx=document.querySelector('[data-review-xp]');
 if(rx)rx.textContent='XP Lv20: '+xpNeed(p,20);
 var rm=document.querySelector('[data-review-mini]');
 if(rm)rm.textContent='Thả trái 2.000đ ≈ '+bracketEnergy(p.rewards.miniEnergyBrackets||[],2000)+'⚡';
 var rb=document.querySelector('[data-review-block]');
 if(rb)rb.textContent='Xếp khối 2.500đ ≈ '+bracketEnergy(p.rewards.blockEnergyBrackets||[],2500)+'⚡';
}
function xpNeed(p,level){return (Number(p.xp.base)||0)+level*(Number(p.xp.linear)||0)+level*level*(Number(p.xp.quadratic)||0)}
function updateConfigHealth(){
 updateSpawnVisuals();
 updateCountVisuals();
 updateDerivedText();
 var host=document.getElementById('cfgHealth');
 if(!host||!state.draft)return;
 var errors=validateClientConfig(state.draft);
 if(errors.length){
  host.className='health-panel bad';
  host.innerHTML='<div class="health-title">⚠️ Cần sửa '+errors.length+' vấn đề trước khi publish</div><ul class="health-list">'+errors.slice(0,8).map(function(e){return '<li>'+esc(e)+'</li>'}).join('')+(errors.length>8?'<li>… và '+(errors.length-8)+' lỗi khác</li>':'')+'</ul>';
 }else{
  host.className='health-panel good';
  host.innerHTML='<div class="health-title health-ok">✅ Form hợp lệ theo rule client</div><div class="muted">Vẫn nên bấm Validate để kiểm tra bằng schema server trước khi publish.</div>';
 }
}
function editorPayload(){
 return normalizeConfigShape(clone(state.draft));
}
function cfgMessage(html,isErr){document.getElementById('cfgMsg').innerHTML='<div class="'+(isErr?'error':'ok')+'" style="margin-top:10px">'+html+'</div>'}
function escLines(lines){return lines.map(function(line){return esc(line)}).join('<br>')}
function validateEditor(){
 var payload=editorPayload();
 api('/api/admin/configs/validate',{method:'POST',body:{payload:payload}}).then(function(r){cfgMessage(r.valid?'✅ Config hợp lệ theo schema server':escLines(r.errors),!r.valid)}).catch(function(e){cfgMessage(esc(e.message),true)});
}
function saveEditor(){
 var errors=validateClientConfig(state.draft);
 if(errors.length&&!confirm('Client đang thấy '+errors.length+' lỗi. Vẫn lưu draft để chỉnh tiếp?'))return;
 var payload=editorPayload();
 api('/api/admin/configs/'+state.config.id,{method:'PUT',body:{payload:payload,note:state.draftNote}}).then(function(){state.flash='✅ Đã lưu draft';renderConfig()}).catch(function(e){cfgMessage(esc(e.message),true)});
}
function publishEditor(){
 var errors=validateClientConfig(state.draft);
 if(errors.length){cfgMessage('⚠️ Chưa thể publish:<br>'+escLines(errors.slice(0,5)),true);return}
 if(state.dirty){cfgMessage('⚠️ Bạn có thay đổi chưa lưu. Bấm Lưu draft trước, rồi Publish.',true);return}
 if(!confirm('Publish config này? Config published hiện tại sẽ archived.'))return;
 api('/api/admin/configs/'+state.config.id+'/publish',{method:'POST'}).then(function(){renderConfig()}).catch(function(e){cfgMessage(esc(e.message),true)});
}

function renderPlayers(){
 shell('<div class="panel"><div class="row"><input id="search" placeholder="Tìm nickname…" value="'+esc(state.playerSearch)+'"><button id="find">Tìm</button></div><div id="players" style="margin-top:12px">Đang tải…</div></div>');
 document.getElementById('find').onclick=function(){state.playerSearch=document.getElementById('search').value;renderPlayers()};
 api('/api/admin/players?limit=200&search='+encodeURIComponent(state.playerSearch)).then(function(rows){
  if(state.tab!=='players')return;
  var playersHost=document.getElementById('players');
  if(!playersHost)return;
  playersHost.innerHTML='<table><thead><tr><th>Người chơi</th><th>Best scores</th><th>Cloud save</th><th>Last seen</th><th></th></tr></thead><tbody>'+rows.map(function(p){
   var scores=(p.bestScores||[]).map(function(s){return s.board+': '+s.bestScore}).join('<br>')||'—';
   var save=p.cloudSave?('rev '+p.cloudSave.revision+'<br>'+fmtDate(p.cloudSave.updatedAt)):'—';
   return '<tr><td>'+esc(p.nickname)+(p.bannedAt?' <span class="pill archived">banned</span>':'')+'<div class="muted mono">'+esc(p.id)+'</div></td><td>'+scores+'</td><td>'+save+'</td><td>'+fmtDate(p.lastSeenAt)+'</td><td><button class="small '+(p.bannedAt?'':'danger')+'" data-ban="'+p.id+'" data-banned="'+(p.bannedAt?'1':'0')+'">'+(p.bannedAt?'Unban':'Ban')+'</button></td></tr>'
  }).join('')+'</tbody></table>';
  Array.prototype.forEach.call(document.querySelectorAll('[data-ban]'),function(b){b.onclick=function(){var action=b.getAttribute('data-banned')==='1'?'unban':'ban';if(!confirm(action+' player này?'))return;api('/api/admin/players/'+b.getAttribute('data-ban')+'/'+action,{method:'POST'}).then(renderPlayers).catch(showError)}});
 }).catch(function(e){showErrorIf('players',e)});
}
function renderAnalytics(){
 shell('<div class="panel"><h2>Analytics 7 ngày</h2><div id="an">Đang tải…</div></div>');
 api('/api/admin/analytics/summary').then(function(d){
  if(state.tab!=='analytics')return;
  var rows=d.events||[];
  var host=document.getElementById('an');
  if(!host)return;
  host.innerHTML=rows.length?'<table><thead><tr><th>Event</th><th>Count</th></tr></thead><tbody>'+rows.map(function(r){return '<tr><td class="mono">'+esc(r.eventName)+'</td><td>'+r.count+'</td></tr>'}).join('')+'</tbody></table>':'<div class="muted">Chưa có event nào.</div>';
 }).catch(function(e){showErrorIf('analytics',e)});
}
function showErrorIf(tab,e){if(state.tab===tab)showError(e)}
function showError(e){shell('<div class="panel"><h2>Lỗi</h2><div class="error">'+esc(e.message||e)+'</div><button onclick="location.reload()">Tải lại</button></div>')}
api('/api/admin/me').then(function(r){state.user=r.username;render()}).catch(function(){renderLogin()});
})();
</script>
</body>
</html>`;
