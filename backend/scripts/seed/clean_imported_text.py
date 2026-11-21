#!/usr/bin/env python3
"""Scan imported recipe text. Default dry-run; --apply writes with optimistic checks.
Run on the DB host with local mysql socket access. Never deletes recipes.
Local snapshots: --jsonl data/seed-candidates/normalized/recipes.jsonl (scan only).
"""
import argparse, json, os, subprocess, datetime, hashlib
from pathlib import Path
from recipe_text import clean, SUSPECT

def mysql(sql, database):
    return subprocess.check_output(['mysql','--default-character-set=utf8mb4','--batch','--raw','--skip-column-names',database],input=('SET SESSION MAX_EXECUTION_TIME=30000;\n'+sql).encode(),timeout=120).decode()
def literal(text):
    return 'CONVERT(0x'+text.encode().hex()+' USING utf8mb4)' if text else "''"
def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--database',default='family_recipes')
    p.add_argument('--apply',action='store_true')
    p.add_argument('--jsonl',type=Path)
    p.add_argument('--report',type=Path)
    args=p.parse_args()
    if args.jsonl and args.apply:p.error('--jsonl only supports scanning; regenerate normalized data to apply')
    rows=[]
    if args.jsonl:
        for line in args.jsonl.read_text().splitlines():
            row=json.loads(line)
            recipe=row.get('recipe',{})
            rows.append(dict(table='snapshot',id=recipe.get('name'),column='description',text=recipe.get('description') or ''))
            rows.extend(dict(table='snapshot',id=recipe.get('name'),column='step',text=s.get('description') or '') for s in row.get('steps',[]))
    else:
        # Scope source cleanup to imported recipes; match translations to those source hashes.
        queries=["""SELECT JSON_OBJECT('table','recipe','id',r.id,'recipe_id',r.id,'column','description','text',r.description)
            FROM recipe r JOIN recipe_provenance p ON p.recipe_id=r.id WHERE r.description IS NOT NULL""",
            """SELECT JSON_OBJECT('table','cooking_step','id',s.id,'recipe_id',s.recipe_id,'column','description','text',s.description)
            FROM cooking_step s JOIN recipe_provenance p ON p.recipe_id=s.recipe_id WHERE s.description IS NOT NULL"""]
        for label,q in zip(('recipe descriptions','cooking steps'),queries):
            print(f'Reading {label}...',flush=True)
            rows.extend(json.loads(line) for line in mysql(q,args.database).splitlines() if line)
        # Hash source text once, then use the existing source_hash index in bounded batches.
        hashes=sorted({hashlib.sha256(row['text'].encode('utf-8')).hexdigest() for row in rows})
        for start in range(0,len(hashes),200):
            batch=hashes[start:start+200]
            values=','.join("'"+h+"'" for h in batch)
            print(f'Reading translations: {min(start+200,len(hashes))}/{len(hashes)} source hashes...',flush=True)
            q=f"""SELECT JSON_OBJECT('table','content_translations','id',t.id,'column','translated_text','text',t.translated_text)
                FROM content_translations t WHERE t.kind='content' AND t.translated_text IS NOT NULL
                AND t.source_hash IN ({values})"""
            rows.extend(json.loads(line) for line in mysql(q,args.database).splitlines() if line)
    changes=[];review=[]
    for row in rows:
        old=row['text'];new=clean(old)
        if old!=new:changes.append(dict(row,before=old,after=new))
        suspect=SUSPECT.findall(new)
        if suspect:review.append(dict(table=row['table'],id=row['id'],column=row['column'],markers=sorted(set(suspect))))
    report=args.report or Path('text-cleanup-'+datetime.datetime.now().strftime('%Y%m%d-%H%M%S')+'.json')
    # Exclusive creation: don't overwrite a previous audit or follow an existing symlink.
    with os.fdopen(os.open(str(report),os.O_WRONLY|os.O_CREAT|os.O_EXCL,0o600),'w') as f:
        json.dump(dict(apply=args.apply,scanned=len(rows),changes=changes,manual_review=review),f,ensure_ascii=False,indent=2)
    print(f'scanned={len(rows)}, safe_changes={len(changes)}, manual_review={len(review)}, report={report}',flush=True)
    if not args.apply:return
    statements=['START TRANSACTION;'];number=0
    for row in changes:
        table,col=row['table'],row['column'];identifier=int(row['id'])
        assert (table,col) in [('recipe','description'),('cooking_step','description'),('content_translations','translated_text')]
        statements.append(f"UPDATE `{table}` SET `{col}`={literal(row['after'])} WHERE id={identifier} AND BINARY `{col}`=BINARY {literal(row['before'])};")
        statements.append('SET @changed=ROW_COUNT(); SELECT @changed;')
        if 'recipe_id' in row:
            statements.append(f"DELETE FROM recipe_enrichment WHERE recipe_id={int(row['recipe_id'])} AND @changed=1;")
    statements.append('COMMIT;')
    print(f'Applying {len(changes)} changes in one transaction...',flush=True)
    results=mysql('\n'.join(statements),args.database)
    applied=sum(int(x) for x in results.splitlines() if x.strip().isdigit())
    print(f'applied={applied}, skipped_concurrent_changes={len(changes)-applied}; original text preserved in report')
if __name__=='__main__':main()
