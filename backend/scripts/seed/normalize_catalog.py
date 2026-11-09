#!/usr/bin/env python3
"""Normalize local source snapshots; no semantic deduplication and no invented photo roles."""
import hashlib
import html
import json
import re
from pathlib import Path
from collect_open_sources import OUT, write_json


def clean(text, wiki=False):
    text=re.sub(r'<!--.*?-->', '', text, flags=re.S)
    text=re.sub(r'!\[[^\]]*\]\([^)]*\)', '', text)
    text=re.sub(r'\[\[(?:File|Image|Category):[^\n]+', '', text, flags=re.I)
    text=re.sub(r'\[\[([^\]|]+)\|([^\]]+)\]\]', lambda m:m[2], text)
    text=re.sub(r'\[\[([^\]]+)\]\]', lambda m:m[1].removeprefix('Cookbook:'), text)
    text=re.sub(r'\[([^\]]+)\]\((https?://[^)]+)\)', r'\1 (\2)',text)
    text=re.sub(r'\[https?://\S+\s+([^\]]+)\]',r'\1',text)
    text=re.sub(r'<br\s*/?>','\n',text,flags=re.I)
    text=re.sub(r'<[^>]+>','',text)
    text=html.unescape(text).replace("'''",'').replace("''",'')
    return text.strip()


def sections(text,wiki=False):
    pattern=r'^(={2,})\s*(.*?)\s*\1\s*$' if wiki else r'^(#{2,})\s+(.+?)\s*$'
    result={}; last=0; title='intro'
    for m in re.finditer(pattern,text,re.M):
        result[title]=text[last:m.start()]; title=m[2].strip().lower();last=m.end()
    result[title]=text[last:];return result


def normalize(r):
    source=r['source']; recipe={'name':r['title'].removeprefix('Cookbook:').removesuffix('的做法'),'visibility':'public'}
    ingredients=[]; steps=[]; tags=[]; gaps=[]; media=[]
    if source=='UniTools':
        row=r['original'];recipe.update(description=row['summary']['en'],servings=row.get('baseServings'),cooking_time=(row.get('prepMinutes') or 0)+(row.get('cookMinutes') or 0))
        ingredients=[dict(name=i['name']['en'],amount=str(i.get('quantity') or '')+' '+(i.get('unit') or '')+(' ('+i['note']['en']+')' if i.get('note') else ''),is_main=False) for i in row['ingredients']]
        # Preserve full notes in the description if they exceed the DB's short amount field.
        recipe['description']+='\n\nIngredients\n'+'\n'.join(i['name']+': '+i['amount'] for i in ingredients)
        for i in ingredients:
            if len(i['amount'])>50:i['amount']='See ingredient notes'
        steps=[dict(step_number=n+1,description=i['text']['en'],duration=i.get('minutes')) for n,i in enumerate(row['steps'])]
        tags=[dict(tag_type='cuisine',tag_value=r['country'])]
    else:
        raw=(OUT/r['raw_markdown' if source=='HowToCook' else 'raw_wikitext']).read_text()
        wiki=source=='Wikibooks'; parts=sections(raw,wiki)
        ingredients_text='\n'.join(v for k,v in parts.items() if k in ('ingredients','必备原料和工具','计算'))
        procedure='\n'.join(v for k,v in parts.items() if k in ('procedure','preparation','directions','instructions','method','操作'))
        if not ingredients_text.strip() or not procedure.strip():return None,'missing recognizable ingredients/procedure'
        if wiki and ('{{' in procedure or '{{' in ingredients_text):return None,'unresolved wiki templates; needs structured parsing/review'
        intro=clean(parts.get('intro',''),wiki)
        if wiki:intro=re.sub(r'\{\{.*?\}\}','',intro,flags=re.S).strip(' |\n')
        else:intro=re.sub(r'^# .*\n?','',intro).strip()
        recipe['description']=intro+'\n\n'+('Ingredients' if wiki else '食材与用量')+'\n'+clean(ingredients_text,wiki)
        # Keep quantities verbatim; do not guess ingredient IDs from free text.
        lines=[]
        for line in procedure.splitlines():
            value=clean(re.sub(r'^\s*(?:\d+[.)]|[-*#]+)\s*','',line),wiki)
            if value:lines.append(value)
        steps=[dict(step_number=n+1,description=line) for n,line in enumerate(lines)]
        if not steps:return None,'no usable steps'
        gaps.append('unstructured_ingredients_preserved_in_description')
        if source=='HowToCook':tags=[]
        else:
            cuisine=[c['title'].removeprefix('Category:') for c in r.get('categories',[]) if ' cuisine' in c['title'].lower()]
            tags=[dict(tag_type='cuisine',tag_value=c[:50]) for c in cuisine[:3]]
    for img in r.get('images',[]):
        if img.get('status')!='downloaded' or not img.get('local_path'):continue
        file=OUT/img['local_path']
        if not file.is_file():continue
        metadata=dict(img)
        if source=='HowToCook':
            metadata.update(author=r['author'],license=r['license'],license_url=r['license_url'],source_url=r['source_url'])
        if not metadata.get('license') or not metadata.get('author'):continue
        if source=='UniTools':
            lic=metadata['license'].lower().replace(' ','-')
            match=re.fullmatch(r'cc-(by(?:-sa)?)-(\d\.\d)',lic)
            if match:metadata['license_url']=f'https://creativecommons.org/licenses/{match[1]}/{match[2]}/'
            elif lic in ('cc0','cc0-1.0','public-domain'):metadata['license_url']='https://creativecommons.org/publicdomain/zero/1.0/'
            else:continue
        metadata['sha256']=hashlib.sha256(file.read_bytes()).hexdigest()
        media.append(metadata)
    if media:
        # A lead photo is a display cover; other photos are gallery images, not fabricated step images.
        recipe['cover_image']='local:'+media[0]['local_path']
        recipe['images']=[dict(url='local:'+i['local_path'],category='cover' if n==0 else 'screenshot',position=n) for n,i in enumerate(media[:30])]
    else:gaps.append('no_verified_cover')
    gaps.append('no_verified_step_photo')
    if len(recipe['name'])>100 or not recipe['name']:return None,'invalid title length'
    if len(steps)>100 or any(len(s['description'])>10000 for s in steps):return None,'step length/count exceeds application limits'
    if recipe.get('cooking_time')==0:recipe.pop('cooking_time')
    provenance={k:r[k] for k in ('source','source_id','source_url','author','license','license_url','revision','history_url','language','country') if k in r}
    provenance.update(images=media,modifications='Normalized to app fields; formatting changed. Translations, when present, are machine-generated adaptations.',quality_gaps=gaps,popularity=None)
    for kind in ('taste','cuisine'):
        if not any(t['tag_type']==kind for t in tags):
            tags.append(dict(tag_type=kind,tag_value='待确认'))
            gaps.append(kind+'_needs_classification')
    return dict(recipe=recipe,ingredients=ingredients,steps=steps,tags=tags,provenance=provenance),None


def main():
    target=OUT/'normalized';target.mkdir(exist_ok=True)
    previous={}
    catalog=target/'recipes.jsonl'
    if catalog.exists():
        for line in catalog.read_text().splitlines():
            old=json.loads(line);prov=old['provenance']
            if prov.get('classification_reviewed'):
                previous[(prov['source'],prov['source_id'])]=old
    records=[];skipped=[]
    for source in ('howtocook','unitools','wikibooks'):
        path=OUT/source/'candidates.jsonl'
        if not path.exists():continue
        for line in path.read_text().splitlines():
            r=json.loads(line)
            try:value,error=normalize(r)
            except (KeyError,ValueError,TypeError) as e:value=None;error=str(e)
            if value:
                old=previous.get((r['source'],r['source_id']))
                if old and old['recipe']['name']==value['recipe']['name'] and old['recipe'].get('description')==value['recipe'].get('description') and old['steps']==value['steps']:
                    value['tags']=old['tags']
                    value['provenance'].update({k:v for k,v in old['provenance'].items() if k.startswith('classification_')})
                records.append(value)
            else:skipped.append(dict(source=r['source'],id=r['source_id'],reason=error))
    (target/'recipes.jsonl').write_text(''.join(json.dumps(r,ensure_ascii=False)+'\n' for r in records))
    report=dict(records=len(records),sources={s:sum(r['provenance']['source']==s for r in records) for s in ('HowToCook','UniTools','Wikibooks')},
        with_cover=sum(bool(r['recipe'].get('cover_image')) for r in records),with_verified_step_photo=0,skipped=skipped,
        note='No popularity ranking or deduplication. Incomplete media explicitly retained as gaps; no invented steps/photos.')
    write_json(target/'report.json',report);print(json.dumps({k:v for k,v in report.items() if k!='skipped'},ensure_ascii=False))

if __name__=='__main__':main()
