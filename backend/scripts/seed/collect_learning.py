#!/usr/bin/env python3
"""Pin and package HowToCook learning chapters for offline reading. No database writes."""
import html,json,re,shutil,urllib.parse
from pathlib import Path
from collect_open_sources import OUT,ROOT,fetch
base=OUT/'howtocook'
tree=json.loads((base/'tree.json').read_text()); rev=tree['sha']
assets=ROOT/'android/app/src/main/assets/learning';assets.mkdir(parents=True,exist_ok=True)
entries=[i['path'] for i in tree['tree'] if i['type']=='blob' and i['path'].startswith('tips/')]
for path in entries:
    error=fetch('https://raw.githubusercontent.com/Anduin2017/HowToCook/'+rev+'/'+urllib.parse.quote(path),base/'raw'/path)
    if error:raise RuntimeError(error)
readme=(base/'raw/README.md').read_text()
chapters=[]
for section,title in [('beginner','搭建环境'),('advanced','进阶知识学习')]:
    block=readme.split('## '+title,1)[1].split('\n## ',1)[0]
    for name,path in re.findall(r'\[([^]]+)\]\((tips/[^)]+\.md)\)',block):
        target=base/'raw'/path
        text=target.read_text()
        # Render escaped markdown; never run source HTML or JavaScript.
        body=[]
        for line in text.splitlines():
            escaped=html.escape(line)
            def image(m):
                relative=(target.parent/urllib.parse.unquote(html.unescape(m[2]))).resolve()
                if not relative.is_relative_to((base/'raw/tips').resolve()) or not relative.is_file():return m[1]
                out=assets/relative.relative_to(base/'raw');out.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(relative,out)
                return '<img alt="'+m[1]+'" src="'+urllib.parse.quote(str(relative.relative_to(base/'raw')))+'">'
            escaped=re.sub(r'!\[([^]]*)\]\(([^)]+)\)',image,escaped)
            escaped=re.sub(r'\*\*(.+?)\*\*',r'<strong>\1</strong>',escaped)
            if line.startswith('#'):
                level=min(4,len(line)-len(line.lstrip('#')));body.append(f'<h{level}>'+escaped.lstrip('# ').strip()+f'</h{level}>')
            elif line.strip():body.append('<p>'+escaped+'</p>')
        filename=str(len(chapters))+'.html'
        source='https://github.com/Anduin2017/HowToCook/blob/'+rev+'/'+urllib.parse.quote(path)
        page='<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><style>body{font:18px/1.75 sans-serif;margin:24px;background:#fffaf4;color:#302820}h1{font-size:26px}h2{font-size:23px}p{overflow-wrap:anywhere}img{max-width:100%}a{color:#a8492e}@media(prefers-color-scheme:dark){body{background:#211c19;color:#f6eee6}a{color:#ffba98}}</style></head><body>'+''.join(body)+'</body></html>'
        (assets/filename).write_text(page)
        chapters.append(dict(title=name,section=section,file=filename,source=source,revision=rev,license='Unlicense',language='zh'))
(assets/'index.json').write_text(json.dumps(chapters,ensure_ascii=False,indent=2))
print(f'Packaged {len(chapters)} chapters, pinned revision {rev}')
