#!/usr/bin/env python3
"""Collect pinned HowToCook and UniTools candidates locally. No DB writes."""
import concurrent.futures as cf
import datetime as dt
import hashlib
import json
import re
import time
import urllib.parse
import urllib.request
import urllib.error
from pathlib import Path
import argparse

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / "data/seed-candidates"
HEADERS = {"User-Agent": "FamilyRecipes-source-audit/1.0", "Accept": "*/*"}

def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2))

def fetch(url, path):
    if path.exists() and path.stat().st_size:
        return None
    error = None
    for attempt in range(2):
        try:
            request=urllib.request.Request(url,headers=HEADERS)
            with urllib.request.urlopen(request,timeout=25) as response:
                body=response.read()
            path.parent.mkdir(parents=True,exist_ok=True)
            temp=path.with_suffix(path.suffix+".part")
            temp.write_bytes(body);temp.replace(path)
            return None
        except urllib.error.HTTPError as exc:
            error=f"HTTPError: HTTP Error {exc.code}: {exc.reason}"
            if exc.code == 429:
                return {"url":url,"error":error,"status":429,"retry_after":exc.headers.get("Retry-After"),
                        "path":str(path.relative_to(OUT))}
            if attempt==0: time.sleep(1)
        except Exception as exc:
            error=f"{type(exc).__name__}: {exc}"
            if attempt==0: time.sleep(1)
    return {"url":url,"error":error,"path":str(path.relative_to(OUT))}

def batch(jobs):
    failures=[]
    with cf.ThreadPoolExecutor(max_workers=4) as pool:
        futures=[pool.submit(fetch,url,path) for url,path in jobs]
        for index,future in enumerate(cf.as_completed(futures),1):
            error=future.result()
            if error: failures.append(error)
            if index%100==0 or index==len(futures):
                print(f"Downloaded/checked {index}/{len(futures)}, failures {len(failures)}",flush=True)
    return failures

def howtocook():
    base=OUT/"howtocook"
    tree_path=base/"tree.json"
    error=fetch("https://api.github.com/repos/Anduin2017/HowToCook/git/trees/master?recursive=1",tree_path)
    if error: raise RuntimeError(error)
    tree=json.loads(tree_path.read_text())
    if tree.get("truncated"): raise RuntimeError("Truncated repository tree")
    revision=tree["sha"]
    entries=[x for x in tree["tree"] if x["type"]=="blob" and (x["path"].startswith("dishes/") or x["path"] in ("LICENSE","README.md"))]
    jobs=[]
    for item in entries:
        path=item["path"]
        if ".." in Path(path).parts or Path(path).is_absolute(): raise ValueError(path)
        url=f"https://raw.githubusercontent.com/Anduin2017/HowToCook/{revision}/"+urllib.parse.quote(path)
        jobs.append((url,base/"raw"/path))
    failures=batch(jobs)
    records=[]
    for item in entries:
        path=item["path"]
        if not(path.startswith("dishes/") and path.endswith(".md")): continue
        file=base/"raw"/path
        if not file.exists(): continue
        text=file.read_text()
        title=re.search(r"^#\s+(.+)",text,re.M)
        # Keep original markdown and image links. Do not fabricate step-image associations.
        images=[]
        for ref in re.findall(r"!\[[^\]]*\]\(((?:[^()\n]|\([^()]*\))+)\)",text):
            ref=ref.strip().split(' "')[0].strip("<>")
            if urllib.parse.urlparse(ref).scheme:
                images.append({"source_url":ref,"status":"external_reference_not_downloaded","role":"unclassified"})
            else:
                local=(file.parent/urllib.parse.unquote(ref)).resolve()
                if not local.is_relative_to((base/"raw").resolve()): continue
                exists=local.is_file()
                images.append({"local_path":str(local.relative_to(OUT.resolve())),"status":"downloaded" if exists else "missing",
                    "sha256":hashlib.sha256(local.read_bytes()).hexdigest() if exists else None,"role":"unclassified"})
        records.append({"source":"HowToCook","source_id":path,"revision":revision,
            "source_url":f"https://github.com/Anduin2017/HowToCook/blob/{revision}/"+urllib.parse.quote(path),
            "title":title.group(1) if title else file.stem,"language":"zh","raw_markdown":str(file.relative_to(OUT)),
            "author":"HowToCook contributors; per-file history retained via source URL",
            "license":"Unlicense","license_scope":"repository declaration; third-party exceptions require review",
            "license_url":f"https://github.com/Anduin2017/HowToCook/blob/{revision}/LICENSE",
            "images":images,"cuisine":None,"cuisine_family":None,"popularity":None,
            "status":"candidate","rights_verified":False})
    return records,failures

def unitools():
    base=OUT/"unitools"
    source="https://theunitools.com/data/unitools-recipes-v1.json"
    error=fetch(source,base/"raw.json")
    if error: raise RuntimeError(error)
    data=json.loads((base/"raw.json").read_text())
    records=[];jobs=[]
    for row in data["recipes"]:
        slug=row["slug"]
        if not re.fullmatch(r"[a-z0-9-]+",slug): raise ValueError(slug)
        photo=row.get("photo")
        images=[]
        if photo and photo.get("url"):
            url=photo["url"];parsed=urllib.parse.urlparse(url)
            if parsed.scheme=="https" and parsed.hostname in ("theunitools.com","www.theunitools.com","upload.wikimedia.org"):
                local=base/"images"/(slug+".jpg")
                jobs.append((url,local))
                images=[{"source_url":url,"author":photo.get("author"),"license":photo.get("license"),
                    "local_path":str(local.relative_to(OUT)),"role":"cover_candidate","status":"pending"}]
        records.append({"source":"UniTools","source_id":slug,"title":row["name"]["en"],"language":["en","ru"],
            "source_url":source,"landing_page":data.get("landingPage"),"dataset_version":data.get("version"),
            "author":data.get("attribution"),"license":data.get("license"),"license_url":data.get("licenseUrl"),
            "country":row["country"],"cuisine":None,"cuisine_family":None,"original":row,"images":images,
            "popularity":None,"status":"candidate","rights_verified":False})
    failures=batch(jobs)
    for record in records:
        for image in record["images"]:
            path=OUT/image["local_path"]
            image["status"]="downloaded" if path.exists() else "download_failed"
            image["sha256"]=hashlib.sha256(path.read_bytes()).hexdigest() if path.exists() else None
    return records,failures

if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--source",choices=["howtocook","unitools","all"],default="all")
    args=parser.parse_args()
    OUT.mkdir(parents=True,exist_ok=True)
    summaries={}
    for source in ["howtocook","unitools"]:
        if args.source not in (source,"all"):continue
        print(f"Collecting {source}",flush=True)
        records,failures=globals()[source]()
        target=OUT/source/"candidates.jsonl"
        target.write_text("".join(json.dumps(r,ensure_ascii=False)+"\n" for r in records))
        summary={"collected_at":dt.datetime.now(dt.timezone.utc).isoformat(),"candidates":len(records),
            "recipes_with_downloaded_images":sum(any(i["status"]=="downloaded" for i in r["images"]) for r in records),
            "referenced_images":sum(len(r["images"]) for r in records),
            "failures":failures,"ready_for_publication":0,
            "gaps":["Individual source/image review","Verified step photo","Cuisine classification","Popularity evidence"]}
        write_json(OUT/source/"report.json",summary);summaries[source]=summary
        print(json.dumps({k:v for k,v in summary.items() if k!="failures"},ensure_ascii=False),flush=True)
    write_json(OUT/"latest-run.json",summaries)
