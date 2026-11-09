#!/usr/bin/env python3
"""Resumable, serial Wikibooks collector, including licensed image originals."""
import argparse
import hashlib
import json
import re
import time
import signal
import fcntl
import urllib.parse
from collect_open_sources import OUT, fetch, write_json


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--delay', type=float, default=15)
    parser.add_argument('--max-batches', type=int, default=100)
    parser.add_argument('--stop-after-failures', type=int, default=3, help='Stop this run after repeated unavailable requests')
    args = parser.parse_args()
    base = OUT / 'wikibooks'
    base.mkdir(parents=True, exist_ok=True)
    run_lock = (base / '.collector.lock').open('w')
    try:
        fcntl.flock(run_lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    except BlockingIOError:
        raise SystemExit('A Wikibooks collection is already running; no concurrent writer started.')
    failures = []
    last_request = 0
    checkpoint = lambda: None

    def paced(url, path):
        nonlocal last_request
        if path.exists() and path.stat().st_size:
            return None
        for attempt in range(3):
            time.sleep(max(0, max(5, args.delay) - (time.monotonic() - last_request)))
            error = fetch(url, path)
            last_request = time.monotonic()
            if not error:
                return None
            if error.get('status') != 429:
                break
            try:
                wait = float(error.get('retry_after') or 60)
            except ValueError:
                wait = 60
            print(f'Rate limited; waiting {max(wait, 60 * (attempt + 1))} seconds', flush=True)
            time.sleep(max(wait, 60 * (attempt + 1)))
        failures.append(error)
        if len(failures) >= args.stop_after_failures:
            checkpoint()
            print("Remote source unavailable; stopped with checkpoint. No further retry in this run.", flush=True)
            raise SystemExit(2)
        return error

    def query(host, params, path):
        url = f'https://{host}/w/api.php?' + urllib.parse.urlencode(dict(action='query', format='json', **params))
        if paced(url, path):
            return {}
        data = json.loads(path.read_text())
        if 'error' in data:
            failures.append({'url': url, 'error': data['error']})
            path.unlink()  # Do not cache API errors as successful responses.
            return {}
        return data

    members = []; continuation = {}; page = 0
    while True:
        data = query('en.wikibooks.org', dict(list='categorymembers', cmtitle='Category:Recipes', cmtype='page', cmlimit=500, **continuation), base/'index'/f'{page}.json')
        members.extend(data.get('query', {}).get('categorymembers', []))
        continuation = data.get('continue', {})
        if not continuation:
            break
        page += 1
    members = list({m['pageid']: m for m in members if m['ns'] == 102}.values())
    print(f'Index: {len(members)} recipe pages', flush=True)
    uncached = 0
    for start in range(0, len(members), 50):
        path = base/'batches'/f'{start}.json'
        if path.exists():
            continue
        if uncached >= args.max_batches:
            break
        data = query('en.wikibooks.org', dict(pageids='|'.join(str(m['pageid']) for m in members[start:start+50]), prop='revisions|categories', rvprop='ids|timestamp|content', rvslots='main', cllimit='max'), path)
        uncached += 1
        print(f'Text batch {start}: {len(data.get("query", {}).get("pages", {}))} pages', flush=True)
        if not data:
            break

    records = []
    for path in sorted((base/'batches').glob('*.json')):
        data = json.loads(path.read_text())
        for value in data.get('query', {}).get('pages', {}).values():
            if 'revisions' not in value:
                continue
            revision = value['revisions'][0]; text = revision['slots']['main']['*']; rid = revision['revid']
            raw = base/'raw'/f"{value['pageid']}-{rid}.wiki"
            raw.parent.mkdir(parents=True, exist_ok=True); raw.write_text(text)
            refs = re.findall(r'\[\[(?:File|Image):([^|\]]+)', text, re.I)
            refs += re.findall(r'^\s*\|\s*(?:image|image[0-9]+)\s*=\s*([^\n|]+)', text, re.I | re.M)
            refs = list(dict.fromkeys(x.strip().removeprefix('File:').removeprefix('Image:') for x in refs if re.search(r'\.(?:jpg|jpeg|png|webp|gif)\s*$', x, re.I)))
            records.append(dict(source='Wikibooks', source_id=str(value['pageid']), title=value['title'], revision=rid,
                source_url=f'https://en.wikibooks.org/w/index.php?oldid={rid}', author='Wikibooks contributors',
                history_url=f"https://en.wikibooks.org/w/index.php?curid={value['pageid']}&action=history",
                license='CC BY-SA 4.0', license_url='https://en.wikibooks.org/wiki/Wikibooks:Copyrights',
                raw_wikitext=str(raw.relative_to(OUT)), language='en', categories=value.get('categories', []),
                categories_complete='continue' not in data, images=[dict(title=x, role='unclassified', status='pending') for x in refs],
                cuisine=None, popularity=None, rights_verified=False, status='candidate'))

    def save():
        (base/'candidates.jsonl').write_text(''.join(json.dumps(r, ensure_ascii=False)+'\n' for r in records))
        write_json(base/'report.json', dict(index_pages=len(members), candidates=len(records), failures=failures,
            downloaded_image_references=sum(i.get('status') == 'downloaded' for r in records for i in r['images']), ready_for_publication=0))
    checkpoint = save
    signal.signal(signal.SIGTERM, lambda *_: (save(), exit(0)))
    save()  # Text survives interruption during media downloads.
    for count, record in enumerate(records):
        for img in record['images']:
            digest = hashlib.sha256(img['title'].encode()).hexdigest()
            data = query('en.wikibooks.org', dict(titles='File:'+img['title'], prop='imageinfo', iiprop='url|extmetadata', redirects=1), base/'imageinfo'/f'{digest}.json')
            info = next((p['imageinfo'][0] for p in data.get('query', {}).get('pages', {}).values() if p.get('imageinfo')), None)
            if not info:
                img['status'] = 'metadata_unavailable'; continue
            meta = info.get('extmetadata', {})
            def field(key): return meta.get(key, {}).get('value', '')
            img.update(source_url=info.get('url'), attribution_url=info.get('descriptionurl'), author=field('Artist'),
                license=field('LicenseShortName'), license_url=field('LicenseUrl'), attribution=field('Attribution'), metadata=meta)
            # No fair-use / NC / ND images. Retain metadata for manual review instead.
            license_name = img['license'].lower()
            allowed = bool(re.fullmatch(r'cc(?:0| by(?:-sa)?) [0-9.]+', license_name) or license_name in ('public domain', 'cc0'))
            url = img.get('source_url') or ''
            parsed = urllib.parse.urlparse(url)
            if not allowed or not img['author'] or parsed.scheme != 'https' or parsed.hostname != 'upload.wikimedia.org':
                img['status'] = 'license_review_required'; continue
            ext = parsed.path.rsplit('.', 1)[-1].lower()
            if ext not in ('jpg', 'jpeg', 'png', 'gif', 'webp'):
                img['status'] = 'unsupported_format'; continue
            dest = base/'images'/f'{digest}.{ext}'
            error = paced(url, dest)
            img.update(status='download_failed' if error else 'downloaded', local_path=str(dest.relative_to(OUT)))
            if not error:
                img['sha256'] = hashlib.sha256(dest.read_bytes()).hexdigest()
            save()  # Checkpoint every attempted image, including failed downloads.
            print(f"Image {img['title']}: {img['status']}", flush=True)
        if count % 20 == 0:
            save(); print(f'Images checked: {count+1}/{len(records)} recipes', flush=True)
    save()
    print(f'Saved {len(records)} recipes', flush=True)

if __name__ == '__main__':
    main()
