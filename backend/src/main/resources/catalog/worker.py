#!/usr/bin/env python3
"""Bounded server-side catalog discovery. Reads MySQL; writes only a local review queue."""
import argparse, datetime, fcntl, hashlib, html, json, os, re, sqlite3, subprocess, time
from pathlib import Path
from urllib.parse import urlparse, parse_qs, urlencode
from urllib.request import Request, build_opener, HTTPRedirectHandler
from urllib.error import HTTPError

BASE='https://open.bigmodel.cn/api/paas/v4/'
class ProviderError(Exception): pass
class BudgetExceeded(Exception): pass
class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, *args, **kwargs): return None

def now(): return datetime.datetime.now(datetime.timezone.utc).isoformat()
def plain(value): return html.unescape(re.sub('<[^>]*>', '', value or ''))
def platform(url):
    try:
        u=urlparse(url); host=(u.hostname or '').lower(); path=u.path
        if u.scheme!='https' or u.username or u.password or u.port not in (None,443): return None
        if host in ('www.youtube.com','youtube.com','m.youtube.com'):
            if path=='/watch' and re.fullmatch(r'[\w-]{11}',parse_qs(u.query).get('v',[''])[0]): return 'overseas'
            if re.fullmatch(r'/shorts/[\w-]{11}/?',path): return 'overseas'
        if host=='youtu.be' and re.fullmatch(r'/[\w-]{11}/?',path): return 'overseas'
        if host in ('www.bilibili.com','bilibili.com') and re.fullmatch(r'/video/(BV[\w]+|av\d+)/?',path): return 'cn'
        if host in ('www.xiaohongshu.com','xiaohongshu.com') and re.fullmatch(r'/(explore|discovery/item)/[a-fA-F0-9]+/?',path): return 'cn'
        if host in ('www.xiachufang.com','xiachufang.com') and re.fullmatch(r'/recipe/\d+/?',path): return 'cn'
    except (ValueError,TypeError): pass
    return None

# Curated common dishes supply stable search names without a translation request.
COMMON = [
('西红柿炒鸡蛋|番茄炒蛋','tomato scrambled eggs'),('宫保鸡丁|宫爆鸡丁','kung pao chicken'),
('鱼香肉丝','yu xiang shredded pork'),('麻婆豆腐','mapo tofu'),('红烧肉','red braised pork belly'),
('回锅肉','twice cooked pork'),('糖醋排骨','sweet and sour pork ribs'),('可乐鸡翅','cola chicken wings'),
('土豆炖牛肉','beef potato stew'),('黄焖鸡','Chinese braised chicken'),('葱油拌面','scallion oil noodles'),
('蛋炒饭','egg fried rice'),('扬州炒饭','Yangzhou fried rice'),('蒸蛋|水蒸蛋|鸡蛋羹','Chinese steamed eggs'),
('酸辣土豆丝','hot and sour shredded potatoes'),('醋溜土豆丝','vinegar shredded potatoes'),
('清炒土豆丝','stir fried shredded potatoes'),('地三鲜','di san xian'),('干煸四季豆','dry fried green beans'),
('蒜蓉西兰花','garlic broccoli'),('手撕包菜','stir fried cabbage'),('蚝油生菜','lettuce with oyster sauce'),
('拍黄瓜|凉拌黄瓜','smashed cucumber salad'),('凉拌木耳','wood ear mushroom salad'),
('清蒸鲈鱼','steamed sea bass'),('红烧鲫鱼','braised crucian carp'),('水煮鱼','Sichuan boiled fish'),
('酸菜鱼','Sichuan fish with pickled mustard greens'),('白灼虾','poached shrimp'),('油焖大虾','braised prawns'),
('清蒸生蚝','steamed oysters'),('虾仁炒蛋','shrimp scrambled eggs'),('香菇滑鸡','steamed chicken mushrooms'),
('白切鸡','white cut chicken'),('葱油鸡','scallion oil chicken'),('口水鸡','Sichuan mouthwatering chicken'),
('辣子鸡','Sichuan spicy chicken'),('照烧鸡腿','teriyaki chicken'),('咖喱鸡','chicken curry'),
('玉米排骨汤','corn pork rib soup'),('冬瓜排骨汤','winter melon pork rib soup'),
('番茄蛋汤|西红柿鸡蛋汤','tomato egg soup'),('紫菜蛋花汤','seaweed egg drop soup'),
('小米粥','millet porridge'),('皮蛋瘦肉粥','century egg pork congee'),('馄饨','wonton'),
('饺子','Chinese dumplings'),('葱油饼','scallion pancakes'),('鸡蛋饼','Chinese egg pancakes')]
CHINA_TAGS = {'region:china','region:china-eight','region:china-provinces','中国','CN','中餐','Chinese','川菜','粤菜','湘菜','鲁菜','苏菜','浙菜','闽菜','徽菜','东北菜','西北菜','京菜','沪菜','湖北菜','河南菜','江西菜','贵州菜','云南菜','广西菜','山西菜','陕西菜','新疆菜','内蒙古菜','西藏菜','海南菜','香港风味','澳门风味','台湾菜'}
HOME_TAGS={'家常菜','Home-style','中式家常','家常'}
def norm(text):
    return re.sub(r'[^a-z0-9\u4e00-\u9fff]+','',str(text).lower())
def dish_key(text):
    text=re.sub(r'(的做法|做法)$','',str(text).strip())
    return norm(text)
def terms_for(recipe):
    names=[recipe.get(k,'') for k in ('name','name_zh','name_en')]
    for aliases,en in COMMON:
        if any(dish_key(n) in {dish_key(x) for x in aliases.split('|')}|{dish_key(en)} for n in names if n):
            return {'zh':aliases.split('|')[0],'en':en,'aliases':aliases.split('|')+[en],'source':'common_dictionary'}
    zh=recipe.get('name_zh') or next((n for n in names if re.search(r'[\u4e00-\u9fff]',n)), '')
    en=recipe.get('name_en') or next((n for n in names if n and not re.search(r'[\u4e00-\u9fff]',n)), '')
    return {'zh':zh,'en':en,'aliases':[n for n in names if n]+[n for n in (zh,en) if n],'source':'existing_names'}
def priority(recipe):
    tags=set(recipe.get('tags',[]));terms=terms_for(recipe)
    common=terms['source']=='common_dictionary';chinese=bool(tags & CHINA_TAGS);home=bool(tags & HOME_TAGS)
    return (100 if common else 0)+(40 if chinese else 0)+(20 if home else 0)
GROUPS=('蔬菜','蛋豆','肉类','鱼虾','汤羹','主食')
def selection_group(recipe):
    terms=terms_for(recipe);name=' '.join((terms['zh'],terms['en'])).lower()
    # Selection buckets only, never written as recipe tags. Check soup/staples before ingredients.
    for group,pattern in (
        ('汤羹',r'汤|soup|broth'),
        ('主食',r'饭|面|粥|饼|饺|馄饨|包子|馒头|rice|noodle|congee|porridge|pancake|dumpling|wonton|bread'),
        ('鱼虾',r'鱼|虾|蟹|蚝|贝|鱿|sea bass|fish|shrimp|prawn|crab|oyster|squid'),
        ('肉类',r'肉|鸡(?!蛋)|鸭|鹅|排骨|牛|羊|chicken|pork|beef|duck|lamb|rib'),
        ('蛋豆',r'蛋|豆腐|豆干|\beggs?\b|tofu'),
        ('蔬菜',r'菜|土豆|黄瓜|木耳|茄子|四季豆|豇豆|冬瓜|南瓜|potato|cabbage|broccoli|lettuce|cucumber|mushroom|bean|eggplant|vegetable')):
        if re.search(pattern,name):return group
    return '其他'
def selection_reasons(row):
    reasons=[]
    if terms_for(row)['source']=='common_dictionary':reasons.append('人工常见菜清单')
    if set(row.get('tags',[])) & CHINA_TAGS:reasons.append('已有中餐菜系标签')
    if set(row.get('tags',[])) & HOME_TAGS:reasons.append('已有家常菜标签')
    return reasons

def pick_recipes(rows,limit):
    # Round robin gives each available food group a slot; underfilled groups yield slots naturally.
    buckets={g:[] for g in (*GROUPS,'其他')};seen=set();result=[]
    for row in sorted(rows,key=lambda r:(-priority(r),r['id'])):
        if priority(row)==0:continue
        key=dish_key(terms_for(row)['zh'] or row['name'])
        if key in seen:continue
        existing={platform(u) for u in row.get('existing_links',[])}
        if row.get('has_image') and {'cn','overseas'}<=existing:continue
        seen.add(key);group=selection_group(row)
        buckets[group].append(dict(row,priority=priority(row),selection_group=group,selection_reasons=selection_reasons(row)))
    while len(result)<limit and any(buckets[g] for g in GROUPS):
        for group in GROUPS:
            if buckets[group] and len(result)<limit:result.append(buckets[group].pop(0))
    result.extend(buckets['其他'][:max(0,limit-len(result))])
    return result

def score_candidate(candidate,terms):
    title=candidate.get('title','');description=candidate.get('description','');text=title+' '+description
    if re.search(r'吃播|探店|测评|广告|挑战|mukbang|restaurant review|food tour|eating challenge',title,re.I):return 0
    aliases=[a for a in terms['aliases'] if len(norm(a))>=2]
    title_match=any(norm(a) in norm(title) for a in aliases)
    desc_match=any(norm(a) in norm(description) for a in aliases)
    tutorial=bool(re.search(r'做法|教程|怎么做|制作|烹饪|食谱|recipe|how to|cook|tutorial',text,re.I))
    if not tutorial or not (title_match or desc_match):return 0
    return (80 if title_match else 60)+max(0,6-int(candidate.get('rank',6)))

def choose(candidates,terms,region):
    scored=[dict(c,score=score_candidate(c,terms)) for c in candidates if platform(c['url'])==region]
    scored=[c for c in scored if c['score']>=60]
    if not scored:return None
    return dict(max(scored,key=lambda c:c['score']),region=region,note='相关做法；根据搜索标题、摘要和排名匹配，具体配方可能不同',verification='search_metadata_only',selection='rules-v2')

class Worker:
    def __init__(self, directory, budget=120, delay=2):
        self.directory=Path(directory); self.directory.mkdir(parents=True,exist_ok=True)
        self.db=sqlite3.connect(self.directory/'state.sqlite')
        self.db.executescript('CREATE TABLE IF NOT EXISTS cache(k TEXT PRIMARY KEY,v TEXT); CREATE TABLE IF NOT EXISTS calls(at TEXT,kind TEXT); CREATE TABLE IF NOT EXISTS jobs(id INTEGER PRIMARY KEY,status TEXT,result TEXT,attempts INTEGER DEFAULT 0);')
        self.db.execute('CREATE TABLE IF NOT EXISTS metadata(k TEXT PRIMARY KEY,v TEXT)')
        if not self.db.execute("SELECT 1 FROM metadata WHERE k='rules-v2'").fetchone():
            # Give old failures a fresh bounded retry allowance; retain completed results and caches.
            self.db.execute("UPDATE jobs SET attempts=0 WHERE status IN ('failed','running')")
            self.db.execute("INSERT INTO metadata VALUES('rules-v2',?)",(now(),));self.db.commit()
        self.budget=budget; self.delay=delay
        self.key=os.environ.get('CATALOG_SEARCH_KEY') or os.environ.get('ZHIPU_API_KEY') or os.environ.get('TRANSLATION_LLM_KEY','')
    def activity(self, recipe_id, stage):
        self.db.execute("INSERT OR REPLACE INTO metadata VALUES('activity',?)",(json.dumps({'recipe_id':recipe_id,'stage':stage,'updated_at':now()}),));self.db.commit();self.export()
    def request(self, url, payload=None, key=None):
        fingerprint=hashlib.sha256(json.dumps([url,payload],sort_keys=True).encode()).hexdigest()
        cached=self.db.execute('SELECT v FROM cache WHERE k=?',(fingerprint,)).fetchone()
        if cached:return json.loads(cached[0])
        paid=key is not None
        if paid:
            if self.db.execute("SELECT COUNT(*) FROM calls WHERE kind='search'").fetchone()[0]>=self.budget:raise BudgetExceeded('search_request_limit')
            self.db.execute('INSERT INTO calls VALUES(?,?)',(now(),'search' if url.endswith('web_search') else 'llm'));self.db.commit()
        headers={'User-Agent':'FamilyRecipesCatalogPilot/1.0 (candidate research)','Accept':'application/json'}
        if key: headers['Authorization']='Bearer '+key
        if payload is not None:headers['Content-Type']='application/json'
        time.sleep(self.delay)
        try:
            with build_opener(NoRedirect).open(Request(url,data=json.dumps(payload).encode() if payload is not None else None,headers=headers),timeout=120) as response:
                raw=response.read(4_000_001)
            if len(raw)>4_000_000:raise ValueError('response_too_large')
            result=json.loads(raw)
            if 'error' in result:raise ProviderError('provider_error')
        except HTTPError as e:
            # Extract only a bounded code and recognized parameter names, never the raw message.
            if paid:
                code='unknown'; fields=[]
                try:
                    error=json.loads(e.read(16384)).get('error',{})
                    raw_code=str(error.get('code',''))
                    if re.fullmatch(r'[A-Za-z0-9_-]{1,40}',raw_code):code=raw_code
                    message=str(error.get('message','')).lower()
                    fields=[f for f in ('thinking','reasoning_effort','temperature','max_tokens','model','search_engine','count') if f in message]
                except Exception:pass
                stage='search' if url.endswith('web_search') else 'llm'
                raise ProviderError(f'{stage}_http_{e.code} code={code} parameters={fields}') from None
            raise RuntimeError('commons_http_'+str(e.code)) from None
        self.db.execute('INSERT OR REPLACE INTO cache VALUES(?,?)',(fingerprint,json.dumps(result)));self.db.commit()
        return result
    def search(self, query, domain):
        data=self.request(BASE+'web_search',{'search_engine':os.environ.get('CATALOG_SEARCH_ENGINE','search_std'),'search_intent':False,'search_query':query[:70],'search_domain_filter':domain,'search_recency_filter':'noLimit','count':5},self.key)
        if not isinstance(data.get('search_result'),list):raise ValueError('invalid_search_response')
        return [dict(url=r.get('link',''),title=r.get('title','')[:300],description=r.get('content','')[:500],rank=i+1,query=query,domain=domain) for i,r in enumerate(data['search_result']) if platform(r.get('link',''))]
    def images(self, name):
        params={'action':'query','format':'json','generator':'search','gsrsearch':name,'gsrnamespace':6,'gsrlimit':5,'prop':'imageinfo','iiprop':'url|extmetadata|size'}
        data=self.request('https://commons.wikimedia.org/w/api.php?'+urlencode(params))
        candidates=[]
        for page in data.get('query',{}).get('pages',{}).values():
            info=(page.get('imageinfo') or [{}])[0]; meta=info.get('extmetadata',{})
            get=lambda k:plain(meta.get(k,{}).get('value',''))
            license=get('LicenseShortName')
            if license not in ('CC0','Public domain','CC BY 2.0','CC BY 3.0','CC BY 4.0','CC BY-SA 2.0','CC BY-SA 3.0','CC BY-SA 4.0'):continue
            if info.get('width',0)<600 or info.get('height',0)<400:continue
            candidates.append({'page':info.get('descriptionurl'),'url':info.get('url'),'title':page['title'],'description':get('ImageDescription')[:1200],'license':license,'license_url':get('LicenseUrl'),'author':get('Artist'),'width':info['width'],'height':info['height'],'status':'license_metadata_available_visual_review_pending'})
        return candidates
    def process(self, recipe):
        terms=terms_for(recipe)
        existing={platform(u) for u in recipe.get('existing_links',[])}
        selected=[];candidates=[];skipped=[]
        if 'cn' not in existing and terms['zh']:
            # Bilibili first; only pay for a second Chinese search if no suitable result exists.
            for domain in ('www.bilibili.com','www.xiaohongshu.com'):
                self.activity(recipe['id'],'搜索B站教程' if domain=='www.bilibili.com' else '搜索小红书教程')
                candidates+=self.search(terms['zh']+' 做法 教程',domain)
                best=choose(candidates,terms,'cn')
                if best:selected.append(best);break
        elif 'cn' in existing:skipped.append('cn_existing')
        else:skipped.append('cn_name_missing')
        if 'overseas' not in existing and terms['en']:
            self.activity(recipe['id'],'搜索YouTube教程')
            candidates+=self.search(terms['en']+' recipe tutorial','www.youtube.com')
            best=choose(candidates,terms,'overseas')
            if best:selected.append(best)
        elif 'overseas' in existing:skipped.append('overseas_existing')
        else:skipped.append('en_name_missing')
        images=[];image_error=None
        if recipe.get('has_image'):skipped.append('image_existing')
        else:
            try:
                self.activity(recipe['id'],'查找可复用图片')
                images=self.images(terms['en'] or terms['zh'])
            except Exception as e:image_error=type(e).__name__
        return {'recipe':recipe,'queries':terms,'searched_at':now(),'strategy':'rules-v2','llm_calls':0,
                'links':selected,'link_candidates':candidates,'images':images,'image_error':image_error,'skipped':skipped,
                'gaps':[r for r in ('cn','overseas') if r not in existing and not any(x['region']==r for x in selected)]+
                       (['image'] if not images and not recipe.get('has_image') else []),'imported':False}
    def export(self):
        rows=[dict(id=r[0],status=r[1],result=json.loads(r[2]) if r[2] else None,attempts=r[3]) for r in self.db.execute('SELECT * FROM jobs ORDER BY id')]
        activity=self.db.execute("SELECT v FROM metadata WHERE k='activity'").fetchone()
        dest=self.directory/'results.json';tmp=dest.with_suffix('.tmp')
        tmp.write_text(json.dumps({'updated_at':now(),'activity':json.loads(activity[0]) if activity else None,'strategy':'rules-v2','llm_enabled':False,'requests_by_kind':dict(self.db.execute('SELECT kind,COUNT(*) FROM calls GROUP BY kind')),'jobs':rows},ensure_ascii=False,indent=2));tmp.replace(dest)

def mysql_rows(sql):
    output=subprocess.check_output(['mysql','--default-character-set=utf8mb4','-N','-B','--raw','family_recipes'],input=('SET SESSION MAX_EXECUTION_TIME=30000;'+sql).encode(),timeout=45).decode()
    return [json.loads(line) for line in output.splitlines() if line]

def snapshot(path, limit):
    if path.exists():return json.loads(path.read_text())
    # Imported public content only. Read narrow fields, not descriptions or personal records.
    rows=mysql_rows("SELECT JSON_OBJECT('id',r.id,'name',r.name,'has_image',IF(COALESCE(r.cover_image,'')<>'',1,0)) FROM recipe r JOIN recipe_provenance p ON p.recipe_id=r.id WHERE r.visibility='public'")
    by_id={r['id']:r for r in rows}
    for table,field,key in [('recipe_tag','tag_value','tags'),('external_recipe','url','existing_links')]:
        for row in mysql_rows(f"SELECT JSON_OBJECT('id',x.recipe_id,'value',x.{field}) FROM {table} x JOIN recipe_provenance p ON p.recipe_id=x.recipe_id"):
            if row['id'] in by_id:by_id[row['id']].setdefault(key,[]).append(row['value'])
    for row in mysql_rows("SELECT DISTINCT JSON_OBJECT('id',x.recipe_id) FROM recipe_image x JOIN recipe_provenance p ON p.recipe_id=x.recipe_id WHERE x.url<>''"):
        if row['id'] in by_id:by_id[row['id']]['has_image']=True
    # Indexed source-hash lookup, limited to names. Prefer the most recently completed translation.
    translations=mysql_rows("SELECT JSON_OBJECT('id',r.id,'language',t.target_language,'text',t.translated_text) FROM recipe r JOIN recipe_provenance p ON p.recipe_id=r.id JOIN content_translations t ON t.source_hash=SHA2(r.name,256) WHERE t.kind='content' AND t.status='ready' AND t.target_language IN ('zh','en') ORDER BY t.updated_at,t.id")
    for row in translations:
        if row['id'] in by_id and row['text']:by_id[row['id']]['name_'+row['language']]=row['text']
    selected=pick_recipes(rows,limit)
    tmp=path.with_suffix('.tmp');tmp.write_text(json.dumps(selected,ensure_ascii=False));tmp.replace(path)
    print('Selection groups: '+json.dumps({g:sum(r['selection_group']==g for r in selected) for g in (*GROUPS,'其他')},ensure_ascii=False),flush=True)
    print(f'Prioritized {len(selected)} common/home-style/Chinese dishes from {len(rows)} imported public recipes',flush=True)
    return selected

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--state-dir',default='/var/lib/familyrecipes-catalog-pilot');p.add_argument('--status',action='store_true');p.add_argument('--recipes-file');args=p.parse_args()
    os.umask(0o077)
    directory=Path(args.state_dir);directory.mkdir(parents=True,exist_ok=True)
    with (directory/'worker.lock').open('w') as lock:
        try:fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
        except BlockingIOError:raise SystemExit('Worker already running; view progress with journalctl.')
        w=Worker(directory)
        if args.status:w.export();print((directory/'results.json').read_text());return
        if not w.key:raise SystemExit('Missing ZHIPU_API_KEY or TRANSLATION_LLM_KEY in ai.env')
        print('rules-v2: no LLM requests; search API only, 30 recipes, 120 cumulative search attempts max',flush=True)
        recipes=json.loads(Path(args.recipes_file).read_text()) if args.recipes_file else snapshot(directory/'recipes-balanced-v2.json',30)
        if not isinstance(recipes,list) or not 1<=len(recipes)<=30:raise SystemExit('Select 1 to 30 recipes')
        for n,recipe in enumerate(recipes):
            rid=recipe['id'];old=w.db.execute('SELECT status,attempts FROM jobs WHERE id=?',(rid,)).fetchone()
            if old and (old[0]=='done' or old[1]>=3):continue
            w.db.execute("INSERT INTO jobs VALUES(?,'running',NULL,1) ON CONFLICT(id) DO UPDATE SET status='running',attempts=attempts+1",(rid,));w.db.commit();w.export()
            print(f'[{n+1}/{len(recipes)}] recipe {rid}: researching',flush=True)
            try:
                result=w.process(recipe);status='done'
            except Exception as e:
                status='failed';result={'error':str(e) if isinstance(e,(ProviderError,BudgetExceeded)) else type(e).__name__,'at':now()}
                w.db.execute('UPDATE jobs SET status=?,result=? WHERE id=?',(status,json.dumps(result),rid));w.db.commit();w.export()
                print(f'recipe {rid}: {result["error"]}',flush=True)
                if isinstance(e,(ProviderError,BudgetExceeded)):raise SystemExit('Stopped: provider access/rate/budget issue; cached progress retained.')
                continue
            w.db.execute('UPDATE jobs SET status=?,result=? WHERE id=?',(status,json.dumps(result,ensure_ascii=False),rid));w.db.commit();w.export()
            print(f'recipe {rid}: {len(result["links"])} links, {len(result["images"])} image candidates, gaps={result["gaps"]}',flush=True)
        w.export();print('Pilot finished; review results.json. No production content changed.',flush=True)
if __name__=='__main__':main()
