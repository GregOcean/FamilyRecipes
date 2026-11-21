import io, json, tempfile, unittest
from urllib.error import HTTPError
from unittest.mock import patch
from worker import Worker, BudgetExceeded, ProviderError, platform, terms_for, choose, pick_recipes, selection_group

class Tests(unittest.TestCase):
    def test_only_concrete_links(self):
        self.assertEqual('cn',platform('https://www.bilibili.com/video/BV1uY8vzkEmx/'))
        self.assertEqual('overseas',platform('https://www.youtube.com/watch?v=0u4KQ0s0d5U'))
        self.assertEqual('cn',platform('https://www.xiaohongshu.com/explore/abc123?xsec_token=test'))
        for url in ('https://www.youtube.com/','https://www.youtube.com/results?search_query=food','https://www.youtube.com.evil.test/watch?v=0u4KQ0s0d5U','https://evil@www.youtube.com/watch?v=0u4KQ0s0d5U','http://127.0.0.1','https://www.bilibili.com:9/video/BVabc','https://www.xiaohongshu.com/user/profile/abc'):
            self.assertIsNone(platform(url))
    def test_budget_persisted(self):
        with tempfile.TemporaryDirectory() as d:
            w=Worker(d,budget=0,delay=0)
            with self.assertRaises(BudgetExceeded):w.request('https://example.test',{},'secret')
    def test_cached_requests_do_not_charge(self):
        with tempfile.TemporaryDirectory() as d:
            w=Worker(d,delay=0)
            class Response:
                def __enter__(self):return self
                def __exit__(self,*args):pass
                def read(self,n):return b'{"search_result":[]}'
            with patch('worker.build_opener') as build:
                build.return_value.open.return_value=Response()
                w.request('https://example.test',{},'secret')
                w.request('https://example.test',{},'secret')
                self.assertEqual(1,build.return_value.open.call_count)
            self.assertEqual(1,w.db.execute('SELECT COUNT(*) FROM calls').fetchone()[0])
    def test_license_allowlist(self):
        with tempfile.TemporaryDirectory() as d:
            w=Worker(d)
            def page(license):return {'title':'Food','imageinfo':[{'width':1000,'height':750,'extmetadata':{'LicenseShortName':{'value':license}}}]}
            with patch.object(w,'request',return_value={'query':{'pages':{'1':page('CC BY-SA 4.0'),'2':page('CC BY-NC 4.0'),'3':page('Unknown')}}}):
                self.assertEqual(1,len(w.images('Food')))
    def test_zero_llm_and_fallback_only_when_needed(self):
        with tempfile.TemporaryDirectory() as d:
            w=Worker(d)
            def search(query,domain):
                return [{'url':'https://www.bilibili.com/video/BVabc','title':'番茄炒蛋做法','description':'','rank':1}] if domain=='www.bilibili.com' else [{'url':'https://www.youtube.com/watch?v=0u4KQ0s0d5U','title':'Tomato scrambled eggs recipe','rank':1}]
            with patch.object(w,'search',side_effect=search) as calls,patch.object(w,'images',side_effect=TimeoutError()),patch.object(w,'request',side_effect=AssertionError('unexpected network')):
                result=w.process({'id':1,'name':'西红柿炒鸡蛋'})
            self.assertEqual(2,calls.call_count);self.assertEqual(2,len(result['links']))
            self.assertEqual(0,result['llm_calls']);self.assertEqual(['image'],result['gaps'])
            self.assertFalse(hasattr(w,'llm'))
    def test_existing_media_skipped(self):
        with tempfile.TemporaryDirectory() as d:
            w=Worker(d)
            with patch.object(w,'search',side_effect=AssertionError()),patch.object(w,'images',side_effect=AssertionError()):
                result=w.process({'id':1,'name':'蛋炒饭','has_image':True,'existing_links':['https://www.bilibili.com/video/BVabc','https://www.youtube.com/watch?v=0u4KQ0s0d5U']})
            self.assertEqual([],result['gaps'])
    def test_missing_translation_does_not_call_llm(self):
        with tempfile.TemporaryDirectory() as d:
            w=Worker(d)
            with patch.object(w,'search',return_value=[]) as search,patch.object(w,'images',return_value=[]):
                result=w.process({'id':1,'name':'某地方小吃'})
            self.assertEqual(2,search.call_count);self.assertIn('en_name_missing',result['skipped'])
    def test_reuses_translated_name(self):
        terms=terms_for({'name':'Special Soup','name_zh':'某地方汤'})
        self.assertEqual('某地方汤',terms['zh']);self.assertEqual('Special Soup',terms['en'])
    def test_priority_and_duplicate_queries(self):
        rows=[{'id':1,'name':'Obscure cake'}, {'id':2,'name':'西红柿炒鸡蛋'}, {'id':3,'name':'番茄炒蛋'}, {'id':4,'name':'一种川菜','tags':['川菜']}, {'id':5,'name':'Home stew','tags':['家常菜']}]
        self.assertEqual({2,4,5},{r['id'] for r in pick_recipes(rows,30)})
    def test_balanced_selection_and_reallocation(self):
        names=['红烧肉','回锅肉','糖醋排骨','可乐鸡翅','黄焖鸡','白切鸡','葱油鸡','酸辣土豆丝','麻婆豆腐','清蒸鲈鱼','玉米排骨汤','蛋炒饭']
        rows=[{'id':i,'name':name} for i,name in enumerate(names)]
        first=pick_recipes(rows,6)
        self.assertEqual(6,len({r['selection_group'] for r in first}))
        self.assertEqual(len(rows),len(pick_recipes(rows,30)))
        self.assertEqual('汤羹',selection_group({'name':'玉米排骨汤'}))
        self.assertEqual('主食',selection_group({'name':'蛋炒饭'}))
        self.assertEqual('蛋豆',selection_group({'name':'西红柿炒鸡蛋'}))
        self.assertTrue(all(r['selection_reasons'] for r in first))
    def test_upgrade_preserves_completed_jobs_and_request_cache(self):
        import sqlite3
        with tempfile.TemporaryDirectory() as d:
            db=sqlite3.connect(d+'/state.sqlite')
            db.executescript("CREATE TABLE jobs(id INTEGER PRIMARY KEY,status TEXT,result TEXT,attempts INTEGER DEFAULT 0); INSERT INTO jobs VALUES(1,'done','{}',1); INSERT INTO jobs VALUES(2,'failed','{}',3);")
            db.commit();db.close()
            w=Worker(d)
            self.assertEqual(('done',1),w.db.execute('SELECT status,attempts FROM jobs WHERE id=1').fetchone())
            self.assertEqual(0,w.db.execute('SELECT attempts FROM jobs WHERE id=2').fetchone()[0])
            w.db.execute('UPDATE jobs SET attempts=2 WHERE id=2');w.db.commit();w.db.close()
            w=Worker(d)
            self.assertEqual(2,w.db.execute('SELECT attempts FROM jobs WHERE id=2').fetchone()[0])
    def test_noisy_results_rejected(self):
        terms=terms_for({'name':'红烧肉'})
        candidates=[{'url':'https://www.bilibili.com/video/BVa','title':'红烧肉探店做法','rank':1}, {'url':'https://www.bilibili.com/video/BVb','title':'红烧鱼做法','rank':2}, {'url':'https://www.bilibili.com/video/BVc','title':'红烧肉家常做法','rank':5}]
        self.assertEqual('https://www.bilibili.com/video/BVc',choose(candidates,terms,'cn')['url'])
        self.assertIsNone(choose(candidates[:2],terms,'cn'))
    def test_provider_error_does_not_expose_body_or_key(self):
        with tempfile.TemporaryDirectory() as d:
            w=Worker(d,delay=0)
            error=HTTPError('https://example.test',400,'Bad request',{},io.BytesIO(b'{"error":{"code":"1210","message":"thinking disabled invalid secret-key private-data"}}'))
            with patch('worker.build_opener') as build:
                build.return_value.open.side_effect=error
                with self.assertRaises(ProviderError) as caught:w.request('https://example.test',{},'secret-key')
            text=str(caught.exception)
            self.assertIn('thinking',text);self.assertIn('1210',text)
            self.assertNotIn('secret-key',text);self.assertNotIn('private-data',text)
    def test_export_preserves_status(self):
        with tempfile.TemporaryDirectory() as d:
            w=Worker(d);w.db.execute("INSERT INTO jobs VALUES(1,'failed','{}',1)");w.db.commit();w.export()
            self.assertEqual('failed',json.loads((w.directory/'results.json').read_text())['jobs'][0]['status'])
if __name__=='__main__':unittest.main()
