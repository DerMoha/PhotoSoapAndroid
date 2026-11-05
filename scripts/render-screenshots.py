#!/usr/bin/env python3
"""Compose promotional artwork around unmodified Android screen captures.

Run with Python 3 and Pillow: python3 scripts/render-screenshots.py
Font overrides: PHOTOSOAP_FONT_REGULAR and PHOTOSOAP_FONT_BOLD.
"""
import os
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/screenshots'
FONT_DIR = Path('/System/Library/Fonts/Supplemental')
REGULAR = os.getenv('PHOTOSOAP_FONT_REGULAR', str(FONT_DIR / 'Arial.ttf'))
BOLD = os.getenv('PHOTOSOAP_FONT_BOLD', str(FONT_DIR / 'Arial Bold.ttf'))
SLIDES = [
    ('01-review', 'Swipe through\nyour photos.', '', False),
    ('06-swipe', 'Swipe right\nto keep.', '', False),
    ('02-delete-list', 'Check what\nyou delete.', '', False),
    ('03-statistics', 'See how much\nspace you freed.', '', False),
    ('04-achievements', 'Earn badges\nas you go.', '', False),
    ('05-appearance', 'Choose your\ncolors and theme.', '', False),
]


def font(size, bold=False):
    return ImageFont.truetype(BOLD if bold else REGULAR, size)


def phone(slug, width, dark=False, angle=0):
    src = OUT / 'source' / ('dark' if dark else '') / f'{slug}.png'
    screen = Image.open(src).convert('RGB')
    h = round(width * screen.height / screen.width)
    screen = screen.resize((width,h), Image.Resampling.LANCZOS)
    bezel = max(9, width//48)
    layer = Image.new('RGBA',(width+100,h+100))
    d = ImageDraw.Draw(layer)
    box=(40-bezel,40-bezel,40+width+bezel,40+h+bezel)
    shadow=Image.new('RGBA',layer.size)
    ImageDraw.Draw(shadow).rounded_rectangle(box, width//13, fill=(0,0,0,90))
    layer=Image.alpha_composite(layer,shadow.filter(ImageFilter.GaussianBlur(16)))
    d=ImageDraw.Draw(layer)
    d.rounded_rectangle(box,width//13,fill='#151C1B',outline='#74847E',width=2)
    mask=Image.new('L',screen.size)
    ImageDraw.Draw(mask).rounded_rectangle((0,0,width-1,h-1),width//16,fill=255)
    layer.paste(screen,(40,40),mask)
    d=ImageDraw.Draw(layer)
    cx=40+width//2;r=width//60
    d.ellipse((cx-r,40+width//40,cx+r,40+width//40+2*r),fill='#080C0B')
    d.rounded_rectangle((40+width+bezel,40+h//5,44+width+bezel,40+h//5+width//8),3,fill='#68766F')
    return layer.rotate(angle,Image.Resampling.BICUBIC,expand=True)


def render(slug, headline, subtitle, dark, index):
    dark = slug in ('03-statistics','05-appearance')
    palettes = {'01-review':('#E5F0D7','#F8F5E9'), '06-swipe':('#D4EEE5','#F2F7EF'),
                '02-delete-list':('#F6E8D8','#FFF9F1'), '03-statistics':('#132D2A','#091615'),
                '04-achievements':('#ECE3F7','#F9F5FC'), '05-appearance':('#253437','#0B181A')}
    from PIL import ImageColor
    top,bottom=map(ImageColor.getrgb,palettes[slug])
    canvas=Image.new('RGB',(1080,1920));d=ImageDraw.Draw(canvas)
    for y in range(1920):
        t=y/1919
        d.line((0,y,1080,y),fill=tuple(round(a+(b-a)*t) for a,b in zip(top,bottom)))
    fg='#EFF7EF' if dark else '#18352E'
    accent='#A9DFCA' if dark else '#357462'
    # Quiet editorial header and oversized typography.
    d.text((72,62),'PhotoSoap',font=font(30,True),fill=fg)
    d.text((955,66),f'{index:02d}',font=font(24),fill=accent)
    d.line((72,119,1008,119),fill=accent,width=1)
    d.multiline_text((68,175),headline,font=font(91,True),fill=fg,spacing=1)
    labels={'01-review':'PHOTO & VIDEO CLEANUP','06-swipe':'KEEP THE PHOTOS YOU LOVE',
            '02-delete-list':'YOU CONFIRM EVERY DELETION','03-statistics':'YOUR CLEANUP AT A GLANCE',
            '04-achievements':'A LITTLE PROGRESS EVERY DAY','05-appearance':'LIGHT, DARK & YOUR FAVORITE COLORS'}
    d.text((73,421),labels[slug],font=font(23,True),fill=accent)
    # Large architectural circle anchors the device without crossing the UI.
    d.ellipse((-220,790,1300,2310),fill=('#1D3D36' if dark else '#FFFFFF'))
    canvas=canvas.convert('RGBA')
    if slug=='05-appearance':
        canvas.alpha_composite(phone(slug,510,False,5),(-75,650))
        canvas.alpha_composite(phone(slug,610,True,-4),(455,545))
    elif slug=='06-swipe':
        canvas.alpha_composite(phone(slug,650,False,-7),(170,510))
    else:
        width=690 if slug=='01-review' else 650
        canvas.alpha_composite(phone(slug,width,dark),(int((1080-width)/2)-40,510))
    canvas=canvas.convert('RGB')
    canvas.save(OUT/'google-play'/f'{slug}.png',optimize=True)
    canvas.resize((360,640),Image.Resampling.LANCZOS).save(OUT/'github'/f'{slug}.png',optimize=True)
    return canvas


def main():
    for folder in ['google-play', 'github']:
        (OUT / folder).mkdir(parents=True, exist_ok=True)
    slides = [render(*s, i+1) for i,s in enumerate(SLIDES)]
    preview = Image.new('RGB', (300 * len(slides), 570), '#DFE9E1')
    for i, slide in enumerate(slides):
        preview.paste(slide.resize((288,512), Image.Resampling.LANCZOS), (6+i*300, 28))
    preview.save(OUT / 'github' / 'overview.png', optimize=True)
    print(f'Rendered {len(slides)} slides in {OUT}')


if __name__ == '__main__':
    main()
