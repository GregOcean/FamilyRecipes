#!/usr/bin/env python3
"""Audit by default; --apply queues additive imported-recipe refinement on the server."""
import argparse
import getpass
import json
import os
import urllib.error
import urllib.parse
import urllib.request


def endpoint(value):
    url=value.rstrip('/')
    parsed=urllib.parse.urlparse(url)
    if (parsed.scheme!='https' and not(parsed.scheme=='http' and parsed.hostname in ('127.0.0.1','localhost'))) or parsed.username or parsed.password or parsed.query or parsed.fragment:
        raise argparse.ArgumentTypeError('Use an HTTPS server URL or local HTTP URL without credentials/query')
    return url


def request(url,path,token=None,body=None):
    headers={'Content-Type':'application/json','X-Content-Original':'true'}
    if token:headers['Authorization']='Bearer '+token
    req=urllib.request.Request(url+path,data=json.dumps(body).encode() if body is not None else None,headers=headers)
    try:
        with urllib.request.urlopen(req,timeout=120) as response:result=json.load(response)
    except urllib.error.HTTPError as e:
        messages={401:'登录失败或已过期：使用 App 账号密码，不是 SSH/MySQL 密码。',403:'仅 CATALOG_ADMIN_USER_ID 对应的数据管理员可以操作。',404:'接口不存在：先部署 1.0.10 后端。'}
        raise SystemExit(messages.get(e.code,'接口请求失败，HTTP '+str(e.code))) from None
    except (urllib.error.URLError,TimeoutError):
        raise SystemExit('连接失败或超时。已入队任务保留；可重跑同一命令，不会重复入队。') from None
    if result.get('code')!=200:raise SystemExit('服务器未确认成功，请检查后端日志。')
    return result.get('data')


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--url',type=endpoint,default='https://api.familyrecipes.live')
    p.add_argument('--login-email');p.add_argument('--apply',action='store_true')
    p.add_argument('--limit',type=int,default=5);p.add_argument('--all',action='store_true')
    args=p.parse_args()
    if not 1<=args.limit<=100:p.error('--limit must be between 1 and 100')
    if args.all and not args.apply:p.error('--all requires --apply')
    token=os.environ.get('CATALOG_ADMIN_TOKEN')
    if args.login_email:
        session=request(args.url,'/api/auth/login',body={'email':args.login_email.strip(),'password':getpass.getpass('App 管理员账号密码（输入不显示）: ')})
        token=session['token']
    if not token:token=getpass.getpass('管理员登录 token（输入不显示）: ')
    before=request(args.url,'/api/catalog/cuisine-refinement',token)
    print(json.dumps(before,ensure_ascii=False,indent=2))
    if not args.apply:
        print('仅审计：未入队，也未触发模型调用。');return
    total=0
    while True:
        result=request(args.url,'/api/catalog/cuisine-refinement?limit='+str(100 if args.all else args.limit),token,{})
        count=result['queued'];total+=count
        print('本批入队：%s；累计入队：%s'%(count,total),flush=True)
        if not args.all or count==0:break
    print('入队完成；后台逐条处理，原标签保留。重复运行不会重复处理同版本任务。')
    print(json.dumps(request(args.url,'/api/catalog/cuisine-refinement',token),ensure_ascii=False,indent=2))


if __name__=='__main__':main()
