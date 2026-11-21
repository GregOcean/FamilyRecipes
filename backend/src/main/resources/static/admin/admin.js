'use strict';
const $=id=>document.getElementById(id), selected=new Set();let token=sessionStorage.getItem('adminToken')||'',page=0,total=0,rows=[],tab='recipes',reviewId='';
const statusNames={queued:'排队中',running:'搜索中',paused:'已暂停',failed:'有失败',review:'待审核',done:'完成',pending:'等待处理',interrupted:'执行中断'};
function node(tag,text,parent){const n=document.createElement(tag);if(text!=null)n.textContent=text;if(parent)parent.append(n);return n;}
function message(text){$('message').textContent=text;}
async function api(path,body){
 const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),15000);
 try{const response=await fetch('/api/'+path,{method:body===undefined?'GET':'POST',signal:controller.signal,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:body===undefined?undefined:JSON.stringify(body)});let result;try{result=await response.json();}catch{throw Error('服务暂不可用，请稍后重试');}if(!response.ok||result.code!==200){if(response.status===401)logout();throw Error(result.message||'请求失败');}return result.data;
 }catch(e){if(e.name==='AbortError')throw Error('请求超时，保留上次结果；请稍后刷新');throw e;}finally{clearTimeout(timer);}
}
function run(fn){return async(...args)=>{try{message('');await fn(...args);}catch(e){message(e.message);}};}
function logout(){token='';sessionStorage.removeItem('adminToken');$('login').hidden=false;$('app').hidden=true;$('logout').hidden=true;}
async function enter(){await api('admin/monitor');$('login').hidden=true;$('app').hidden=false;$('logout').hidden=false;await show('monitor');}
$('loginForm').onsubmit=run(async e=>{e.preventDefault();const data=await api('auth/login',{email:$('email').value.trim(),password:$('password').value});token=data.token;try{await enter();sessionStorage.setItem('adminToken',token);$('password').value='';}catch(e){logout();throw e;}});
$('logout').onclick=logout;
async function show(name){tab=name;document.querySelectorAll('nav button').forEach(b=>b.classList.toggle('active',b.dataset.tab===name));for(const id of ['recipes','jobs','monitor'])$(id).hidden=id!==name;if(name==='recipes')await loadRecipes();if(name==='jobs')await loadJobs();if(name==='monitor')await monitor();}
document.querySelectorAll('nav button').forEach(b=>b.onclick=run(()=>show(b.dataset.tab)));
function count(){$('selection').textContent=`已选${selected.size}道`;$('create').disabled=selected.size===0;}
async function loadRecipes(){const data=await api(`admin/recipes?q=${encodeURIComponent($('query').value.trim())}&page=${page}&missing=${$('missing').checked}`);rows=data.items;total=data.total;$('recipeRows').replaceChildren();rows.forEach(r=>{const tr=node('tr',null,$('recipeRows'));const check=node('input',null,node('td',null,tr));check.type='checkbox';check.checked=selected.has(r.id);check.setAttribute('aria-label','选择'+r.name);check.onchange=()=>{if(check.checked&&selected.size>=30){check.checked=false;message('每批最多30道');return;}check.checked?selected.add(r.id):selected.delete(r.id);count();};node('td',`#${r.id} ${r.name}`,tr);node('td',r.cover_image?'已有':'缺少',tr);node('td',String(r.link_count),tr);});$('page').textContent=`第${page+1}页 · 共${total}道`;$('prev').disabled=page===0;$('next').disabled=(page+1)*30>=total;count();}
$('searchForm').onsubmit=run(async e=>{e.preventDefault();page=0;await loadRecipes();});$('prev').onclick=run(async()=>{page--;await loadRecipes();});$('next').onclick=run(async()=>{page++;await loadRecipes();});
$('selectPage').onclick=run(async()=>{for(const r of rows){if(selected.size>=30)break;selected.add(r.id);}await loadRecipes();});$('clear').onclick=run(async()=>{selected.clear();await loadRecipes();});
$('create').onclick=run(async()=>{if(!confirm(`为选中的${selected.size}道菜搜索候选？每批最多120次搜索API请求，按服务商计费。`))return;$('create').disabled=true;try{await api('admin/jobs',{recipe_ids:[...selected]});selected.clear();await show('jobs');message('任务已排队，确认候选后才会修改菜谱。');}finally{count();}});
function button(parent,text,fn){const b=node('button',text,parent);b.onclick=run(fn);return b;}
let jobsLoading=false;
async function loadJobs(){
 if(jobsLoading)return;jobsLoading=true;
 try {
 const jobs=await api('admin/jobs');
 const open=new Set([...document.querySelectorAll('#jobRows details[open]')].map(d=>d.dataset.job));
 $('jobRows').replaceChildren();
 if(!jobs.length)node('p','暂无网页任务。旧命令行试点的任务不会自动显示在这里，请先选择菜谱创建新批次。',$('jobRows'));
 for(const j of jobs){
 const row=node('div',null,$('jobRows'));row.className='job';
 node('span',`${j.id.slice(0,8)} · ${statusNames[j.status]||j.status} · ${j.total}道 · ${j.created_at}`,row);
 button(row,'查看候选',()=>review(j.id));
 if(['running','queued'].includes(j.status))button(row,'暂停',async()=>{await api(`admin/jobs/${j.id}/pause`,{});await loadJobs();});
 else button(row,'继续 / 重试',async()=>{await api(`admin/jobs/${j.id}/retry`,{});await loadJobs();});
 if(j.error)node('p',j.error,row);
 if(j.progressError)node('p',j.progressError,row);
 if(j.progress){const p=j.progress;const area=node('div',null,row);area.className='job-progress';
 node('p',`完成 ${p.completed}/${p.total} · 失败 ${p.failed} · 搜索请求 ${p.searchRequests}/120`,area);
 const bar=node('progress',null,area);bar.max=p.total||1;bar.value=p.completed+p.failed;bar.setAttribute('aria-label','已处理菜谱数量');
 const current=p.items.filter(i=>i.status==='running');for(const item of current)node('p',`正在处理：${item.name} — ${item.stage}（单次外部请求最多等待120秒）`,area);
 if(j.status==='queued')node('p','等待执行；同一时间只运行一个批次。',area);
 if(j.status==='running'&&!current.length)node('p','任务已启动，等待工作进程更新进度。',area);
 const detail=node('details',null,area);detail.dataset.job=j.id;detail.open=open.has(j.id);node('summary','每道菜的执行情况',detail);
 for(const item of p.items)node('p',`#${item.id} ${item.name} · ${statusNames[item.status]||item.status}${item.stage?' · '+item.stage:''}${item.status==='done'?` · ${item.links}条教程候选 / ${item.images}张图片候选`:''}${item.error?' · '+item.error:''}`,detail);
 if(p.updatedAt)node('small','最近进度：'+new Date(p.updatedAt).toLocaleString(),area);
 }
 }
 }finally{jobsLoading=false;}
}
function safeLink(parent,url,text){try{const u=new URL(url);if(u.protocol!=='https:')return;const a=node('a',text,parent);a.href=u.href;a.target='_blank';a.rel='noopener noreferrer';}catch{}}
async function review(id){const previous=reviewId;reviewId=id;const job=await api(`admin/jobs/${id}`);if(reviewId!==id)return;const box=$('review'),saved=new Map();
if(previous===id)box.querySelectorAll('article[data-recipe]').forEach(card=>saved.set(card.dataset.recipe,{links:[...card.querySelectorAll('input[type=checkbox]:checked')].map(c=>Number(c.value)),image:card.querySelector('input[type=radio]:checked')?.value}));
box.replaceChildren();node('h2','候选审核 · '+id.slice(0,8),box);node('p','教程按搜索标题、摘要和排名匹配，未检查视频内容。请确认图片确为这道菜。已存在的图片和链接会跳过，不覆盖原封面。',box);node('p','搜索请求：'+JSON.stringify(job.results.requests_by_kind||{}),box);button(box,'刷新候选（保留勾选）',()=>review(id));
const results=job.results.jobs||[];if(!results.length)node('p','尚未产生结果，稍后重新点击“查看候选”。',box);
for(const j of results){const card=node('article',null,box);card.className='card';card.dataset.recipe=String(j.id);const prior=saved.get(String(j.id));const r=j.result||{};node('h2',`#${j.id} ${r.recipe?.name||''} · ${statusNames[j.status]||j.status}`,card);if(j.status!=='done'){node('p',r.error||'正在搜索',card);continue;}if(r.gaps?.length)node('p','待补充：'+r.gaps.join(' / '),card);const linkChecks=[];for(const [i,l] of (r.link_candidates||[]).entries()){const label=node('label',null,card),c=node('input',null,label);c.type='checkbox';c.value=String(i);c.checked=prior?.links.includes(i)||false;linkChecks.push([i,c]);safeLink(label,l.url,' '+l.title);node('p',l.description,card);}
let imageIndex=prior?.image===undefined?null:Number(prior.image);const grid=node('div',null,card);grid.className='grid';for(const [i,img] of (r.images||[]).entries()){const tile=node('div',null,grid);tile.className='candidate';try{const u=new URL(img.url);if(u.protocol==='https:'&&u.hostname==='upload.wikimedia.org'){const preview=node('img',null,tile);preview.src=u.href;preview.alt=img.title;preview.loading='lazy';}}catch{}const label=node('label',null,tile),radio=node('input',null,label);radio.type='radio';radio.name='image-'+j.id;radio.value=String(i);radio.checked=imageIndex===i;radio.onchange=()=>imageIndex=i;label.append(document.createTextNode(' '+img.title));node('p',`${img.author} · ${img.license}`,tile);safeLink(tile,img.page,'查看原图和许可');}
button(card,'不添加图片',()=>{imageIndex=null;card.querySelectorAll('input[type=radio]').forEach(c=>c.checked=false);});const apply=button(card,'确认添加所选内容',async()=>{const links=linkChecks.filter(([,c])=>c.checked).map(([i])=>i);if(!links.length&&imageIndex===null){message('请先选择图片或教程链接');return;}apply.disabled=true;try{await api(`admin/jobs/${id}/apply`,{recipe_id:j.id,links,image:imageIndex});message(`#${j.id} 已添加；重复确认不会重复入库。`);}finally{apply.disabled=false;}});}}

const bytes=value=>value==null?'不可用':value>=1073741824?(value/1073741824).toFixed(1)+' GB':value>=1048576?(value/1048576).toFixed(1)+' MB':value>=1024?(value/1024).toFixed(1)+' KB':Math.round(value)+' B';
const number=(value,digits=0)=>value==null?'不可用':Number(value).toLocaleString('zh-CN',{maximumFractionDigits:digits});
const duration=value=>value==null?'不可用':value>=86400?(value/86400).toFixed(1)+' 天':value>=3600?(value/3600).toFixed(1)+' 小时':Math.round(value/60)+' 分钟';
function metric(parent,label,value,detail='',ratio=null,state=''){
 const card=node('div',null,parent);card.className='metric '+state;node('span',label,card).className='metric-label';node('strong',value,card);if(detail)node('span',detail,card).className='detail';
 if(ratio!=null&&Number.isFinite(ratio)){const meter=node('div',null,card);meter.className='meter';node('span',null,meter).style.width=Math.min(100,Math.max(0,ratio*100))+'%';if(!state&&ratio>.9)card.classList.add('bad');else if(!state&&ratio>.75)card.classList.add('warn');}return card;
}
function table(parent,headers,rows,empty='暂无数据'){
 parent.replaceChildren();if(!rows.length){node('p',empty,parent);return;}const t=node('table',null,parent),head=node('tr',null,node('thead',null,t));headers.forEach(h=>node('th',h,head));const body=node('tbody',null,t);rows.forEach(row=>{const tr=node('tr',null,body);row.forEach(value=>node('td',String(value??'不可用'),tr));});
}
let monitoring=false,lastMonitor=0;
async function monitor(){
 if(monitoring)return;monitoring=true;
 try{
 const m=await api('admin/metrics');if(tab!=='monitor')return;
 $('monitorError').hidden=true;lastMonitor=Date.now();$('observedAt').textContent='最近采样 '+new Date(m.observedAt).toLocaleString()+' · 空闲时请求量为 0；不可用指标不会以 0 代替';$('refreshState').textContent='已连接 · '+new Date().toLocaleTimeString();
 const h=m.hardware||{},a=m.application||{},t=m.traffic||{},d=m.database||{};
 for(const id of ['hardwareMetrics','applicationMetrics','trafficMetrics','taskMetrics','databaseMetrics'])$(id).replaceChildren();
 const hardware=$('hardwareMetrics');metric(hardware,'CPU 使用率',number(h.cpuPercent,1)+' %',number(h.cpuCores)+' 核 · '+(h.architecture||''),h.cpuPercent==null?null:h.cpuPercent/100);
 if(h.memoryAvailableBytes!=null)metric(hardware,'系统内存',bytes(h.memoryTotalBytes==null?null:h.memoryTotalBytes-h.memoryAvailableBytes),bytes(h.memoryAvailableBytes)+' 可用 / '+bytes(h.memoryTotalBytes),h.memoryTotalBytes?1-h.memoryAvailableBytes/h.memoryTotalBytes:null);else metric(hardware,'物理内存',bytes(h.memoryTotalBytes),'空闲 '+bytes(h.memoryFreeBytes)+'（不含可回收缓存）');
 metric(hardware,'磁盘可用',bytes(h.diskFreeBytes),'总计 '+bytes(h.diskTotalBytes),h.diskTotalBytes?1-h.diskFreeBytes/h.diskTotalBytes:null);
 metric(hardware,'系统负载',number(h.loadAverage,2),h.os||'');metric(hardware,'网络接收',h.network?.rxBytesPerSecond==null?(h.network?.available?'等待采样':'不可用'):bytes(h.network.rxBytesPerSecond)+'/s',h.network?.available?'非回环网卡合计':'当前系统无法提供');metric(hardware,'网络发送',h.network?.txBytesPerSecond==null?(h.network?.available?'等待采样':'不可用'):bytes(h.network.txBytesPerSecond)+'/s');
 const app=$('applicationMetrics');metric(app,'应用状态',a.status==='up'?'运行中':'不可用','Java '+(a.javaVersion||''),null,a.status==='up'?'good':'bad');metric(app,'持续运行',duration(a.uptimeSeconds));metric(app,'JVM 内存',bytes(a.heapUsedBytes),'最大 '+bytes(a.heapMaxBytes),a.heapMaxBytes?a.heapUsedBytes/a.heapMaxBytes:null);metric(app,'线程',number(a.threads),'守护线程 '+number(a.daemonThreads));metric(app,'死锁线程',number(a.deadlockedThreads),'当前快照',null,a.deadlockedThreads?'bad':'good');metric(app,'GC 累计耗时',number(a.gcMillis)+' ms',number(a.gcCount)+' 次 · 本次进程启动以来');
 const traffic=$('trafficMetrics');metric(traffic,'请求速率',number(t.requestsPerSecond,2)+'/s',number(t.requests)+' 次 / 最近 60 秒');metric(traffic,'P95 响应',number(t.p95Millis,1)+' ms','平均 '+number(t.meanMillis,1)+' ms');metric(traffic,'服务端错误',number(t.serverErrors),'HTTP 5xx · 最近 60 秒',null,t.serverErrors?'bad':'good');metric(traffic,'客户端错误',number(t.clientErrors),'HTTP 4xx · 最近 60 秒',null,t.clientErrors?'warn':'');metric(traffic,'处理中',number(t.active),'本次启动累计 '+number(t.totalSinceStart)+' 次');metric(traffic,'API 响应流量',bytes(t.outBytesPerSecond)+'/s','请求体 '+bytes(t.inBytesPerSecond)+'/s');
 table($('routeMetrics'),['接口','请求 / 60秒','平均延迟','5xx'],(t.routes||[]).map(r=>[r.route,r.requests,number(r.meanMillis,1)+' ms',r.errors]),'最近 60 秒没有 API 请求');
 for(const [kind,title] of [['translation','内容与聊天翻译'],['classification','菜谱打标与食材提取']]){
 const task=m.tasks?.[kind]||{},card=node('div',null,$('taskMetrics'));card.className='metric';node('h2',title,card);node('p',!task.enabled?'服务器未启用':!task.configured?'模型密钥未配置':task.paused?'已暂停，当前请求完成后停止':'按队列处理',card);node('small',task.model||'模型未设置',card);
 const counts=node('div',null,card);counts.className='task-counts';for(const item of task.counts||[]){const label=node('span',statusNames[item.status]||({ready:'已完成',complete:'已完成',needs_review:'待确认'}[item.status])||item.status,counts);node('b',number(item.count),label);}if(task.unavailable)node('p','任务统计暂不可用',card);
 const actions=node('div',null,card);actions.className='task-actions';const toggle=button(actions,task.paused?'恢复队列':'暂停队列',async()=>{await api(`admin/tasks/${kind}/${task.paused?'resume':'pause'}`,{});await monitor();});toggle.disabled=!task.enabled||!task.configured;
 const retry=button(actions,'重试失败项',async()=>{if(!confirm('将失败项重新排队，处理时会调用已配置的模型。继续？'))return;await api(`admin/tasks/${kind}/retry`,{});await monitor();message('失败项已重新排队');});retry.disabled=!task.enabled||!task.configured||!(task.counts||[]).some(c=>c.status==='failed'&&c.count>0);
 }
 const catalog=node('div',null,$('taskMetrics'));catalog.className='metric';node('h2','图片与教程补充',catalog);node('p','搜索候选、审核后入库。每批状态与每道菜的进度独立展示。',catalog);for(const c of m.tasks?.catalog||[])node('p',`${statusNames[c.status]||c.status} · ${c.count} 批`,catalog);button(catalog,'查看补充任务',()=>show('jobs'));
 const database=$('databaseMetrics'),pool=d.pool||{},server=d.server||{};metric(database,'数据库状态',d.status==='up'?'连接正常':'连接失败','探测 '+number(d.latencyMillis,1)+' ms',null,d.status==='up'?'good':'bad');metric(database,'应用连接池',number(pool.active)+' / '+number(pool.max),'活跃 / 上限 · 空闲 '+number(pool.idle),pool.max?pool.active/pool.max:null);metric(database,'等待连接',number(pool.waiting),'应用内等待连接的线程',null,pool.waiting?'warn':'good');metric(database,'MySQL 连接',number(server.Threads_connected),'执行中 '+number(server.Threads_running));metric(database,'慢查询累计',number(server.Slow_queries),'MySQL 启动以来');metric(database,'行锁等待',number(server.Innodb_row_lock_current_waits),'当前等待数量');
 table($('databaseTables'),['数据表','估计行数','数据与索引体积'],(d.tables||[]).map(r=>[r.name,number(r.estimatedRows),bytes(r.bytes)]),d.status!=='up'?'数据库不可用':d.tablesUnavailable?'当前数据库账号无权读取表统计':'暂无表统计');
 }catch(e){$('monitorError').hidden=false;$('monitorError').textContent=e.message+(lastMonitor?'；下面保留 '+new Date(lastMonitor).toLocaleTimeString()+' 的采样结果。':'');$('refreshState').textContent='连接异常';throw e;}finally{monitoring=false;}
}
$('refreshJobs').onclick=run(loadJobs);$('refreshMonitor').onclick=run(monitor);
let nextRefresh=0;
setInterval(()=>{if(!token||document.hidden||$('app').hidden||Date.now()<nextRefresh)return;nextRefresh=Date.now()+Number($('refreshInterval').value);if(tab==='jobs')loadJobs().catch(e=>message(e.message));else if(tab==='monitor'&&$('autoRefresh').checked)monitor().catch(()=>{});},1000);
if(token)run(enter)();
