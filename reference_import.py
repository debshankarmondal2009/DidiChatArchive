#!/usr/bin/env python3
"""Reference implementation of the import rules (used to verify the Kotlin importer's counts).
Usage: python3 -I reference_import.py <chat.txt> [out.jsonl]"""
import re, sys, json, collections
OWNER, OTHER = "Didi 🤔", "𝔻ᴇʙsʜᴀɴᴋᴀʀ 𝕄ᴏɴᴅᴀʟ"   # device owner (right) / contact (left)
HDR = re.compile(r'^[‎‏]?(\d{1,2})/(\d{1,2})/(\d{2,4}), (\d{1,2}):(\d{2})[\s ]*([ap]m) - (.*)$', re.I)
EDIT = re.compile(r'\s*<This message was edited>\s*$')
def parse(path):
    raw = open(path, encoding='utf-8').read().replace('\r\n', '\n').split('\n')
    entries = []
    for line in raw:
        m = HDR.match(line)
        if m: entries.append([m.groups(), m.group(7)])
        elif entries: entries[-1][1] += '\n' + line
    out = []; stats = collections.Counter()
    for g, body in entries:
        d, mo, y = int(g[0]), int(g[1]), int(g[2]); y += 2000 if y < 100 else 0
        h = int(g[3]) % 12 + (12 if g[5].lower() == 'pm' else 0)
        sender = None; text = body
        for name in (OWNER, OTHER):
            if body.startswith(name + ': '): sender = name; text = body[len(name) + 2:]; break
        rec = dict(y=y, mo=mo, d=d, h=h, mi=int(g[4]))
        if sender is None:
            rec.update(kind='system', text=body.strip()); stats['system'] += 1
        else:
            if text.strip() in ('You deleted this message', 'This message was deleted'):
                stats['deleted_dropped'] += 1; continue
            edited = bool(EDIT.search(text)); text = EDIT.sub('', text).rstrip()
            kind = 'text'
            if text == '<Media omitted>': kind = 'media_omitted'
            elif text.endswith(' (file attached)') and '\n' not in text: kind = 'attachment'
            elif text.startswith('location: '): kind = 'location'
            elif text.startswith('POLL:\n'): kind = 'poll'
            elif text == '': kind = 'empty'
            rec.update(kind=kind, out=(sender == OWNER), text=text, edited=edited)
            stats[kind] += 1; stats['edited'] += edited
            stats['right' if sender == OWNER else 'left'] += 1
        out.append(rec)
    return out, stats, len(entries)
if __name__ == '__main__':
    recs, stats, n = parse(sys.argv[1])
    print("entries in file:", n, "| kept:", len(recs)); print(dict(stats))
    if len(sys.argv) > 2:
        with open(sys.argv[2], 'w', encoding='utf-8') as f:
            for r in recs: f.write(json.dumps(r, ensure_ascii=False) + '\n')
    for r in recs[:12]:
        print(f"{r['d']:02d}/{r['mo']:02d} {r['h']:02d}:{r['mi']:02d}", 'RIGHT' if r.get('out') else ('SYS  ' if r['kind']=='system' else 'LEFT '), r['kind'], '|', r['text'][:60].replace('\n',' ⏎ '))
    emp = [r for r in recs if r['kind']=='empty']
    print("empty examples:", [(r['d'],r['mo'],r['h'],r['mi'],r['out']) for r in emp[:5]])
