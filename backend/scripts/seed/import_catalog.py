#!/usr/bin/env python3
"""Dry-run by default. Import through authenticated APIs; never directly alters production DB."""
import argparse
import getpass
import hashlib
import json
import os
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid
from pathlib import Path
from collect_open_sources import OUT, write_json
from catalog_images import prepare_image


def api_json(request, timeout, login=False):
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return json.load(response)
    except urllib.error.HTTPError as error:
        if error.code == 401:
            message = ('登录失败（401）：请核对线上 App 注册邮箱和账号密码；这里不是 SSH、MySQL 密码或 API key。'
                       if login else '登录凭证失效（401）：请重新使用 --login-email 登录后重试。')
        elif error.code == 403:
            message = '权限不足（403）：请确认登录账号的用户 ID 与服务器 CATALOG_ADMIN_USER_ID 一致，并已重启后端。'
        else:
            message = f'请求失败（HTTP {error.code}，接口 {urllib.parse.urlparse(request.full_url).path}）'
            allowed = {'请上传JPEG或PNG图片', '图片不能超过2000万像素', '图片大小须为1字节至10MB', '上传文件过大', '食材过多', '食材名称无效', '菜谱缺少步骤', '最多30张图片', '请填写每一步的做法，最多100步', '不能引用其他用户上传的图片', '本地图片地址不合法'}
            try:
                detail = json.loads(error.read(4096)).get('message')
                if detail in allowed: message += '：' + detail
            except (ValueError, AttributeError, TypeError): pass
        # Do not echo request headers, credentials or an untrusted response body.
        raise SystemExit(message) from None
    except urllib.error.URLError:
        raise SystemExit('无法连接服务器，请检查网络和 --url 地址后重试。') from None


def main():
    p=argparse.ArgumentParser();p.add_argument('--url',default='https://api.familyrecipes.live');p.add_argument('--input',type=Path,default=OUT/'normalized/recipes.jsonl');p.add_argument('--apply',action='store_true');p.add_argument('--limit',type=int,default=0)
    p.add_argument('--login-email', help='Log in with administrator email; password is prompted invisibly')
    args=p.parse_args();rows=[json.loads(l) for l in args.input.read_text().splitlines()]
    if args.limit:rows=rows[:args.limit]
    print(f'{len(rows)} recipes, no semantic deduplication; apply={args.apply}')
    if not args.apply:return
    url=args.url.rstrip('/');parsed=urllib.parse.urlparse(url)
    if parsed.scheme!='https' and not(parsed.scheme=='http' and parsed.hostname in ('127.0.0.1','localhost')):raise ValueError('Use HTTPS or localhost')
    token=os.environ.get('CATALOG_ADMIN_TOKEN')
    if args.login_email:
        print(f'登录服务器：{url}；账号邮箱：{args.login_email.strip()}')
        login=urllib.request.Request(url+'/api/auth/login',data=json.dumps(dict(email=args.login_email.strip(),password=getpass.getpass('App 注册账号密码（不是 SSH/MySQL 密码，输入不显示）: '))).encode(),headers={'Content-Type':'application/json'})
        session=api_json(login,30,login=True)
        if session.get('code')!=200: raise RuntimeError('Administrator login failed')
        token=session['data']['token']
    if not token: token=getpass.getpass('Data administrator login token (hidden): ')
    def request(path,body=None,content_type='application/json'):
        payload=body if isinstance(body,bytes) else json.dumps(body).encode() if body is not None else None
        req=urllib.request.Request(url+path,data=payload,headers={'Authorization':'Bearer '+token,'Content-Type':content_type,'X-Content-Original':'true'})
        data=api_json(req,120)
        if data.get('code')!=200:raise RuntimeError(data.get('message','API failure'))
        return data.get('data')
    existing={r['source_identity'] for r in request('/api/catalog/imported')}
    owner=request('/api/auth/me')['id']
    cachefile=OUT/'normalized'/('uploads-'+hashlib.sha256((url+':'+str(owner)).encode()).hexdigest()[:12]+'.json')
    cache=json.loads(cachefile.read_text()) if cachefile.exists() else {}
    def upload(ref):
        if not ref.startswith('local:'):return ref
        file=(OUT/ref[6:]).resolve()
        if not file.is_relative_to(OUT.resolve()) or not file.is_file():raise ValueError('Invalid local image path')
        digest=hashlib.sha256(file.read_bytes()).hexdigest()
        if digest in cache:return cache[digest]
        prepared,mime=prepare_image(file,OUT/'normalized'/'upload-images')
        if prepared != file: print(f'转换图片：{file.name} → PNG',flush=True)
        extension=".png" if mime == "image/png" else ".jpg"
        boundary=uuid.uuid4().hex
        content=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="image{extension}"\r\nContent-Type: {mime}\r\n\r\n').encode()+prepared.read_bytes()+f'\r\n--{boundary}--\r\n'.encode()
        result=request('/api/upload/image',content,'multipart/form-data; boundary='+boundary)
        cache[digest]=result;write_json(cachefile,cache);return result
    succeeded=0
    for i,row in enumerate(rows):
        provenance=row['provenance'];identity=hashlib.sha256((provenance['source']+'\n'+provenance['source_id']).encode()).hexdigest()
        if identity in existing:continue
        print(f'{i+1}/{len(rows)} preparing: {row["recipe"]["name"]}',flush=True)
        try:
            recipe=row['recipe']
            if recipe.get('cover_image'):recipe['cover_image']=upload(recipe['cover_image'])
            for image in recipe.get('images',[]):image['url']=upload(image['url'])
            for step in row['steps']:
                if step.get('image'):step['image']=upload(step['image'])
            provenance['upload_image_policy']='Upload copies are converted to PNG or resized when required by server limits; original files and hashes are preserved.'
            recipe_id=request('/api/catalog/import',row)
        except (SystemExit, Exception) as error:
            raise SystemExit(f'第 {i+1} 条「{row["recipe"]["name"]}」未完成：{error}。已成功导入的记录保留，修复后重跑自动跳过。') from None
        existing.add(identity);succeeded+=1
        print(f'{i+1}/{len(rows)} imported recipe {recipe_id}: {recipe["name"]}',flush=True)
        time.sleep(.2)
    print(f'Imported {succeeded}; existing source records skipped safely.')
if __name__=='__main__':main()
