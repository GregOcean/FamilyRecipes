// UI smoke test against static pages with deterministic API fixtures. No provider calls or production writes.
const {chromium}=require('playwright');
const assert=require('node:assert/strict');
(async()=>{
 const browser=await chromium.launch({headless:true,channel:'chrome'});
 try {
 const page=await browser.newPage({viewport:{width:1280,height:900}}), errors=[];page.on('pageerror',e=>errors.push(e.message));
 let created=false,applied=false,reset=false;
 const id='12345678-1234-4234-8234-123456789012';
 await page.route('**/api/**',async route=>{
  const req=route.request(),p=new URL(req.url()).pathname;let data={};
  if(p==='/api/auth/login')data={token:'fixture-token'};
  else if(p==='/api/admin/monitor')data={database:'ok',uptimeSeconds:3600,heapUsedMB:120,heapMaxMB:512,diskFreeMB:1000,searchConfigured:true,jobs:[],translations:[],recentFailures:[]};
  else if(p==='/api/admin/metrics')data={observedAt:'2026-11-19T14:00:00Z',hardware:{os:'Linux',architecture:'amd64',cpuCores:2,cpuPercent:18.5,memoryTotalBytes:4294967296,memoryAvailableBytes:2147483648,diskTotalBytes:80000000000,diskFreeBytes:60000000000,loadAverage:0.4,network:{available:true,rxBytesPerSecond:1024,txBytesPerSecond:4096}},application:{status:'up',javaVersion:'17',uptimeSeconds:3600,heapUsedBytes:120000000,heapMaxBytes:512000000,threads:32,daemonThreads:20,deadlockedThreads:0,gcCount:12,gcMillis:230},traffic:{windowSeconds:60,requests:120,requestsPerSecond:2,active:1,totalSinceStart:4500,p95Millis:38,meanMillis:14,clientErrors:1,serverErrors:0,outBytesPerSecond:2048,inBytesPerSecond:1024,routes:[{route:'GET /api/recipes/{id}',requests:20,meanMillis:16,errors:0}]},database:{status:'up',latencyMillis:2,pool:{active:1,idle:4,total:5,max:10,waiting:0},server:{Threads_connected:8,Threads_running:1,Slow_queries:0,Innodb_row_lock_current_waits:0},tables:[{name:'recipe',estimatedRows:3718,bytes:1200000}]},tasks:{translation:{enabled:true,configured:true,paused:false,model:'fixture',counts:[{status:'ready',count:40}]},classification:{enabled:false,configured:false,paused:false,counts:[]},catalog:[]}};
  else if(p==='/api/admin/recipes')data={items:[{id:13,name:'宫保鸡丁',cover_image:null,link_count:0}],total:1};
  else if(p==='/api/admin/jobs'&&req.method()==='POST'){assert.deepEqual(req.postDataJSON(),{recipe_ids:[13]});created=true;data={id};}
  else if(p==='/api/admin/jobs')data=created?[{id,status:'review',total:1,created_at:'2026-11-15',progress:{completed:1,failed:0,running:0,total:1,searchRequests:2,items:[{id:13,name:'宫保鸡丁',status:'done',stage:'',links:1,images:0,error:''}]}}]:[];
  else if(p===`/api/admin/jobs/${id}`)data={results:{jobs:[{id:13,status:'done',result:{recipe:{name:'宫保鸡丁'},link_candidates:[{url:'https://www.youtube.com/watch?v=abcdefghijk',title:'Kung pao chicken recipe',description:'Simple cooking tutorial'}],images:[],gaps:['image']}}]}};
  else if(p.endsWith('/apply')){assert.deepEqual(req.postDataJSON(),{recipe_id:13,links:[0],image:null});applied=true;}
  else if(p==='/api/auth/password-reset-code')data={retry_after:60};
  else if(p==='/api/auth/password-reset'){assert.equal(req.postDataJSON().code,'123456');reset=true;}
  else throw Error('Unexpected API '+p);
  await route.fulfill({contentType:'application/json',body:JSON.stringify({code:200,data})});
 });
 const base=process.env.ADMIN_UI_BASE||'http://127.0.0.1:8897';
 await page.goto(base+'/admin/index.html');await page.locator('#email').fill('admin@example.test');await page.locator('#password').fill('fixture-password');await page.locator('#loginForm button').click();
 await page.getByText('机器硬件',{exact:true}).waitFor();await page.getByText('应用流量',{exact:true}).waitFor();await page.getByText('数据库监控',{exact:true}).waitFor();await page.getByRole('button',{name:'菜谱内容',exact:true}).click();await page.getByRole('checkbox',{name:'选择宫保鸡丁'}).check();page.on('dialog',d=>d.accept());await page.locator('#create').click();await page.getByRole('button',{name:'查看候选'}).click();
 await page.getByText('完成 1/1 · 失败 0 · 搜索请求 2/120').waitFor();
 await page.locator('#review input[type=checkbox]').check();await page.getByRole('button',{name:'刷新候选（保留勾选）'}).click();await page.waitForFunction(()=>document.querySelector('#review input[type=checkbox]')?.checked);assert.ok(await page.locator('#review input[type=checkbox]').isChecked());await page.getByRole('button',{name:'确认添加所选内容'}).click();await page.waitForFunction(()=>document.getElementById('message').textContent.includes('已添加'));assert.ok(applied);
 await page.getByRole('button',{name:'服务监控',exact:true}).click();await page.getByText('JVM 内存',{exact:true}).waitFor();
 if(process.env.ADMIN_UI_SCREENSHOT)await page.screenshot({path:process.env.ADMIN_UI_SCREENSHOT,fullPage:true});
 await page.setViewportSize({width:390,height:844});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth));await page.locator('#autoRefresh').uncheck();await page.route('**/api/admin/metrics',r=>r.fulfill({status:503,contentType:'application/json',body:JSON.stringify({code:503,message:'fixture unavailable'})}));await page.locator('#refreshMonitor').click();await page.locator('#monitorError').waitFor();assert.ok((await page.locator('#hardwareMetrics').textContent()).includes('18.5'));await page.unroute('**/api/admin/metrics');
 await page.goto(base+'/reset-password.html');await page.locator('#email').fill('user@example.test');await page.locator('#send').click();await page.waitForFunction(()=>document.getElementById('message').textContent.includes('已发送'));
 await page.locator('#code').fill('123456');await page.locator('#password').fill('new-password');await page.locator('#confirm').fill('new-password');await page.locator('#submit').click();await page.waitForFunction(()=>document.getElementById('reset').hidden);assert.ok(reset);assert.deepEqual(errors,[]);
 console.log('PASS: login, selection, job creation, candidate application, monitoring, mobile width and password reset (mock API).');
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
