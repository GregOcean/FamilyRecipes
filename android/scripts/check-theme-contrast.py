#!/usr/bin/env python3
"""Check text/background palette pairs in both Android themes (WCAG 4.5:1)."""
from pathlib import Path
import xml.etree.ElementTree as ET
res = Path(__file__).resolve().parents[1] / 'app/src/main/res'
def colors(folder):
    return {e.attrib['name']: e.text for e in ET.parse(res / folder / 'colors.xml').getroot()}
def lum(value):
    value = value.removeprefix('#')[-6:]
    v = [int(value[i:i+2], 16) / 255 for i in (0, 2, 4)]
    v = [x / 12.92 if x <= .04045 else ((x + .055) / 1.055) ** 2.4 for x in v]
    return sum(x * w for x, w in zip(v, (.2126, .7152, .0722)))
base = colors('values')
for mode, palette in [('light', base), ('dark', base | colors('values-night'))]:
    for fg, bg in [('text_primary','background'),('text_primary','surface'),('text_hint','surface'),('text_secondary','background'),('text_primary','primary_light'),('primary','primary_light'),('on_primary','primary'),('white','toolbar_background')]:
        a,b=sorted((lum(palette[fg]),lum(palette[bg])))
        ratio=(b+.05)/(a+.05)
        print(f'{mode}: {fg}/{bg} = {ratio:.2f}:1')
        assert ratio >= 4.5, (mode, fg, bg, ratio)
