"""Builds www/index.html (the offline app bundled into the Android project) from src/."""
import json, re, shutil, os
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
S = lambda *p: os.path.join(ROOT, *p)
WWW = S('www')
os.makedirs(S('www', 'fonts'), exist_ok=True)

d = json.load(open(S('src', 'data', 'bac_data.json')))
keep = {'bac_date': {k: d['bac_date'].get(k) for k in ('estimated_start', 'official', 'note')}, 'tips': d['tips'], 'streams': []}
for s in d['streams']:
    subs = []
    for x in s['subjects']:
        y = {k: x[k] for k in ('id', 'name_ar', 'coefficient', 'duration_hours') if k in x}
        y['chapters'] = x.get('chapters', []); y['optional'] = x.get('optional', False)
        if x.get('chapters_by_option'): y['chapters_by_option'] = x['chapters_by_option']
        subs.append(y)
    keep['streams'].append({'id': s['id'], 'name_ar': s['name_ar'], 'options': s.get('options') or [], 'subjects': subs})
msg = json.load(open(S('src', 'data', 'messages_ar.json')))

def svg(p):
    t = open(p).read()
    t = re.sub(r'<\?xml[^>]*>', '', t); t = re.sub(r'<title>.*?</title>', '', t)
    return re.sub(r'\s(width|height)="\d+"', '', t, count=2).strip()
moods = ['happy', 'neutral', 'worried', 'sad']
mas = {k: svg(S('src', 'mascot', f'{k}.svg')) for k in moods}
ico = {k: svg(S('src', 'icons', f'icon_{k}.svg')).replace('id="bg"', f'id="bg-{k}"').replace('url(#bg)', f'url(#bg-{k})') for k in moods}

ex = {}
for f in ('exercises_math_physics.json', 'exercises_svt_philo.json'):
    fp = S('src', 'data', f)
    if os.path.exists(fp):
        for k, v in json.load(open(fp)).items():
            if isinstance(v, list): ex.setdefault(k, []).extend(v)
lk = json.load(open(S('src', 'data', 'links.json'))) if os.path.exists(S('src', 'data', 'links.json')) else {}
lk = {'sites': lk.get('sites', []), 'links': lk.get('links', {})}
h = open(S('src', 'app.html')).read()
h = h.replace('/*BUILD*/', str(int(os.environ.get('BUILD_NUMBER', '0') or 0)))
for k, v in [('/*HD*/', json.load(open(S('src', 'data', 'history_dates.json')))), ('/*TERMS*/', (lambda t: {k: t.get(k) for k in ('history', 'geography', 'units')})(json.load(open(S('src', 'data', 'terms.json'))) if os.path.exists(S('src', 'data', 'terms.json')) else {})), ('/*EX*/', ex), ('/*LINKS*/', lk), ('/*DATA*/', keep), ('/*MSGF*/', json.load(open(S('src', 'data', 'messages_ar_f.json')))), ('/*MSG*/', msg), ('/*MASCOT*/', mas), ('/*ICONS*/', ico)]:
    h = h.replace(k, json.dumps(v, ensure_ascii=False).replace('</', '<\\/'))

# Offline fonts instead of Google Fonts
faces = []
for fam, pkg, weights in [('IBM Plex Sans Arabic', 'ibm-plex-sans-arabic', (400, 500, 600, 700)), ('Readex Pro', 'readex-pro', (300, 500, 600, 700))]:
    for w in weights:
        for sub in ('arabic', 'latin'):
            fn = f'{pkg}-{sub}-{w}-normal.woff2'
            shutil.copy(S('node_modules', '@fontsource', pkg, 'files', fn), S('www', 'fonts', fn))
            faces.append(f'@font-face{{font-family:"{fam}";font-style:normal;font-weight:{w};font-display:swap;src:url(fonts/{fn}) format("woff2")}}')
h = re.sub(r'<link rel="preconnect"[^>]*>\s*<link rel="stylesheet" href="https://fonts.googleapis.com[^>]*>', '<style>' + ''.join(faces) + '</style>', h)
assert 'fonts.googleapis' not in h

shutil.copy(S('node_modules', '@capacitor', 'core', 'dist', 'capacitor.js'), S('www', 'capacitor-core.js'))
shutil.copy(S('node_modules', '@capacitor', 'local-notifications', 'dist', 'plugin.js'), S('www', 'local-notifications.js'))
head = ('<!doctype html><html lang="ar" dir="rtl"><head><meta charset="utf-8">'
        '<meta name="viewport" content="width=device-width,initial-scale=1,viewport-fit=cover">'
        '<meta name="theme-color" content="#0A7458">'
        '<style>:root{padding-top:env(safe-area-inset-top,0px);padding-bottom:env(safe-area-inset-bottom,0px)}body{margin:0}[hidden]{display:none!important}</style>'
        '<script src="capacitor-core.js"></script><script src="local-notifications.js"></script></head><body>')
open(S('www', 'index.html'), 'w').write(head + h + '</body></html>')
print('www/index.html', len(head + h), 'bytes')
