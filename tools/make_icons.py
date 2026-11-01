"""Generates launcher icons (4 moods, legacy + adaptive), splash screens and the notification icon."""
import os, re, subprocess, tempfile
from PIL import Image
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, 'android/app/src/main/res')
MOODS = {'happy': ('#13A37F', '#0B6E57'), 'neutral': None, 'worried': None, 'sad': None}
DENS = {'mdpi': 1, 'hdpi': 1.5, 'xhdpi': 2, 'xxhdpi': 3, 'xxxhdpi': 4}
tmp = tempfile.mkdtemp()

def render(svg_text, w, h, out):
    p = os.path.join(tmp, 'x.svg'); open(p, 'w').write(svg_text)
    subprocess.run(['rsvg-convert', '-w', str(w), '-h', str(h), '-o', out, p], check=True)

for m in ['happy', 'neutral', 'worried', 'sad']:
    src = open(os.path.join(ROOT, f'src/icons/icon_{m}.svg')).read()
    src = re.sub(r'<title>.*?</title>', '', src)
    src = re.sub(r'\s(width|height)="\d+"', '', src, count=2)
    stops = re.findall(r'stop-color="(#[0-9A-Fa-f]{6})"', src)[:2]
    body = src.split('<rect width="512" height="512" rx="114" fill="url(#bg)"/>', 1)[1].rsplit('</svg>', 1)[0]
    fg = f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="-150 -150 812 812">{body}</svg>'
    bg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 108 108"><defs><linearGradient id="g" x1="0" y1="0" x2="0" y2="1">'
          f'<stop offset="0" stop-color="{stops[0]}"/><stop offset="1" stop-color="{stops[1]}"/></linearGradient></defs>'
          f'<rect width="108" height="108" fill="url(#g)"/></svg>')
    for d, k in DENS.items():
        folder = os.path.join(RES, f'mipmap-{d}'); os.makedirs(folder, exist_ok=True)
        render(src, int(48 * k), int(48 * k), os.path.join(folder, f'ic_fennouk_{m}.png'))
        render(fg, int(108 * k), int(108 * k), os.path.join(folder, f'ic_fennouk_{m}_fg.png'))
        render(bg, int(108 * k), int(108 * k), os.path.join(folder, f'ic_fennouk_{m}_bg.png'))
    os.makedirs(os.path.join(RES, 'mipmap-anydpi-v26'), exist_ok=True)
    open(os.path.join(RES, f'mipmap-anydpi-v26/ic_fennouk_{m}.xml'), 'w').write(
        '<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        f'    <background android:drawable="@mipmap/ic_fennouk_{m}_bg"/>\n    <foreground android:drawable="@mipmap/ic_fennouk_{m}_fg"/>\n</adaptive-icon>\n')

# default launcher = happy
for d in DENS:
    f = os.path.join(RES, f'mipmap-{d}')
    for name in ('ic_launcher.png', 'ic_launcher_round.png'):
        Image.open(os.path.join(f, 'ic_fennouk_happy.png')).save(os.path.join(f, name))
    Image.open(os.path.join(f, 'ic_fennouk_happy_fg.png')).save(os.path.join(f, 'ic_launcher_foreground.png'))
for name in ('ic_launcher.xml', 'ic_launcher_round.xml'):
    open(os.path.join(RES, 'mipmap-anydpi-v26', name), 'w').write(
        '<?xml version="1.0" encoding="utf-8"?>\n<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
        '    <background android:drawable="@mipmap/ic_fennouk_happy_bg"/>\n    <foreground android:drawable="@mipmap/ic_fennouk_happy_fg"/>\n</adaptive-icon>\n')

# splash: mascot on the app background + app name
mascot = re.sub(r'<title>.*?</title>', '', open(os.path.join(ROOT, 'src/mascot/happy.svg')).read())
mp = os.path.join(tmp, 'mascot.png')
for path in [p for p in os.popen(f'ls -d {RES}/drawable*/splash.png').read().split()]:
    w, h = Image.open(path).size
    s = int(min(w, h) * 0.42)
    render(re.sub(r'\s(width|height)="\d+"', '', mascot, count=2), s, s, mp)
    img = Image.new('RGB', (w, h), '#F3F5F4'); m = Image.open(mp).convert('RGBA')
    img.paste(m, ((w - s) // 2, (h - s) // 2 - int(s * 0.08)), m); img.save(path)

# notification small icon: white fox-head silhouette (vector)
os.makedirs(os.path.join(RES, 'drawable'), exist_ok=True)
open(os.path.join(RES, 'drawable/ic_stat_fennouk.xml'), 'w').write('''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FFFFFFFF" android:pathData="M3,2.5L9,8.2C10,7.9 11,7.8 12,7.8C13,7.8 14,7.9 15,8.2L21,2.5C21.6,6.5 20.9,10.2 19.4,12.6C19.8,13.4 20,14.3 20,15.2C20,18.6 16.4,21.5 12,21.5C7.6,21.5 4,18.6 4,15.2C4,14.3 4.2,13.4 4.6,12.6C3.1,10.2 2.4,6.5 3,2.5Z"/>
</vector>
''')
print('icons ok')
