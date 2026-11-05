"""Рисует иконки бустов 16x16 в assets/bows/textures/item. Запуск: python3 draw_icons.py"""
import os

from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'assets', 'bows', 'textures', 'item')

PAL = {
    '.': None,
    'O': '#2a1a0e',   # outline
    'G': '#e8b33a', 'g': '#a8701c', 'Y': '#fff2a8',  # gold
    'W': '#cfeefc', 'w': '#8cc6df',   # glass
    'S': '#f0cf72', 's': '#c29a42',   # sand
    'C': '#8ff6ff', 'c': '#2fb3d6',   # frost sparkle
    # teleport
    'K': '#1a0b2a', 'P': '#7a2fc4', 'p': '#4b1782', 'M': '#c77dff', 'L': '#f1d9ff', 'E': '#2e8f7a', 'e': '#7fe0c4',
    'Q': '#ffffff', 'R': '#ff4a4a', 'r': '#a31616',
    # break
    'D': '#1e1e1e', 'T': '#8a8a8a', 't': '#5e5e5e', 'B': '#7a5636', 'b': '#4f361f', 'A': '#5c9e3a', 'a': '#3d6e24',
    'F': '#ffffff', 'f': '#d6d6d6', 'X': '#ffd23a', 'x': '#ff8a1f',
}

def render(name, rows):
    assert len(rows) == 16, (name, len(rows))
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        assert len(row) == 16, (name, y, row, len(row))
        for x, ch in enumerate(row):
            c = PAL[ch]
            if c:
                img.putpixel((x, y), tuple(int(c[i:i+2], 16) for i in (1, 3, 5)) + (255,))
    img.save(os.path.join(OUT, f'{name}.png'))
    return img

ICONS = {
'timestop': [
"................",
"..OOOOOOOOOOOO..",
"..OYGGGGGGGGgO..",
"..OOOOOOOOOOOO..",
"...OWSSSSSSwO...",
"...OWsSSSSswO...",
"....OWsSSswO..C.",
".....OWSSwO..CcC",
"......OSwO....C.",
".....OWWCwO.....",
"....OWWWWWwO....",
".C..OWWSSWwO....",
"CcC.OWSSSSwO....",
".C.OOOOOOOOOO...",
"..OYGGGGGGGGgO..",
"..OOOOOOOOOOOO..",
],
'teleportrandom': [
"................",
".....KKKKKK.....",
"...KKppppppKK...",
"..KppPPPPPPppK..",
".KpPPMQQQQMPPpK.",
".KpPMQQMMQQMPpK.",
"KpPMLMMMMQQMLPpK",
"KpPMLMMMQQMMLPpK",
"KpPMLMMQQMMMLPpK",
"KpPMMMMQQMMMMPpK",
".KpPMMMMMMMMPpK.",
".KpPPMMQQMMPPpK.",
"..KppPPPPPPppK..",
"...KKppppppKK...",
".....KKKKKK.....",
"................",
],
'teleportclosest': [
"................",
".....KKKKKK.....",
"...KKppppppKK...",
"..KppPPPPPPppK..",
".KpPPMMRRMMPPpK.",
".KpPMMLRRLMMPpK.",
"KpPMMLLLLLLMMPpK",
"KpPRRLLrrLLRRPpK",
"KpPRRLLrrLLRRPpK",
"KpPMMLLLLLLMMPpK",
".KpPMMLRRLMMPpK.",
".KpPPMMRRMMPPpK.",
"..KppPPPPPPppK..",
"...KKppppppKK...",
".....KKKKKK.....",
"................",
],
'break': [
".....DDDDDD.....",
".Tt..DXXXxD..Bb.",
".tT..DXXXxD..bB.",
".....DXXXxD.....",
"..DDDDXXXxDDDD..",
"...DXXXXXXXxD...",
"B...DXXXXXxD...T",
".....DXXXxD.....",
"......DXxD......",
".T.F...DD...F.t.",
"ff.FF.xXXx.FF.ff",
"AAaAAAADDAAAaAAA",
"BbBBbBDBBDBbBBbB",
"bBBBDBBbbBBDBBbB",
"BBbDBBbBBbBBDbBB",
"BbBBBbBBBBbBBBbB",
],
}

if __name__ == '__main__':
    for name, rows in ICONS.items():
        render(name, rows)
    # превью: все иконки рядом, x16, на сером фоне как в инвентаре
    S = 16
    prev = Image.new('RGBA', (len(ICONS) * (16 * S + 32) + 32, 16 * S + 64), (139, 139, 139, 255))
    for i, name in enumerate(ICONS):
        im = Image.open(os.path.join(OUT, f'{name}.png')).resize((16 * S, 16 * S), Image.NEAREST)
        prev.alpha_composite(im, (32 + i * (16 * S + 32), 32))
    prev.save(os.path.join(os.path.dirname(OUT), '..', '..', '..', 'preview.png'))
