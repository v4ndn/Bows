"""Рисует иконки бустов 16x16 в assets/bows/textures/item. Запуск: python3 draw_icons.py

Стиль: пастельная геометрическая фигура (не на весь слот) + простой символ
внутри, нарисованный тёмным тоном того же оттенка.
"""
import os

from PIL import Image

ROOT = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(ROOT, 'assets', 'bows', 'textures', 'item')

# центр фигур; фигуры занимают пиксели 2..12, слот 16x16
C = 7


# ---- фигуры: функция (x, y) -> внутри ли пиксель ----
def circle(x, y):
    return (x - C) ** 2 + (y - C) ** 2 <= 5.6 ** 2


def diamond(x, y):
    # центр между пикселями: углы по 2 пикселя, без одиночных шипов
    return abs(x - C - .5) + abs(y - C - .5) <= 6


diamond.center = C + .5


def octagon(x, y):
    dx, dy = abs(x - C), abs(y - C)
    return dx <= 5 and dy <= 5 and dx + dy <= 7


# ---- палитры: пастельная заливка + тона того же оттенка ----
def rounded_square(x, y):
    dx, dy = abs(x - C), abs(y - C)
    return dx <= 5 and dy <= 5 and dx + dy <= 9


def palette(fill, outline, glyph, light, shade):
    return {'fill': fill, 'outline': outline, 'glyph': glyph, 'light': light, 'shade': shade}


BLUE = palette('#b5dcf5', '#6fa3c7', '#3d6c8f', '#e3f3fd', '#97c6e6')
LAVENDER = palette('#d4c4f5', '#9a85d1', '#5e4a9c', '#efe8ff', '#bba8ec')
MINT = palette('#b8ead2', '#73bd9b', '#3f8a68', '#e4f8ee', '#9bd9bc')
PEACH = palette('#f8cfae', '#d69a6e', '#9a5a33', '#fdeee2', '#edb88f')
BUTTER = palette('#f6e6a6', '#d1b65e', '#8a7228', '#fdf7dc', '#ead489')

# ---- символы: '#' = тёмный тон, 'o' = светлый тон ----
HOURGLASS = [
    "#####",
    ".#.#.",
    ".#o#.",
    "..#..",
    ".#.#.",
    ".###.",
    "#####",
]

QUESTION = [
    ".####.",
    "##..##",
    "...##.",
    "..##..",
    "......",
    "..##..",
]

TARGET = [
    ".####.",
    "#....#",
    "#.##.#",
    "#.##.#",
    "#....#",
    ".####.",
]

SLAM = [
    "...#...",
    "...#...",
    ".#####.",
    "..###..",
    "...#...",
    "#######",
]

DICE = [
    "#...#",
    ".....",
    "..#..",
    ".....",
    "#...#",
]

ICONS = {
    'timestop': (circle, BLUE, HOURGLASS),
    'teleportrandom': (diamond, LAVENDER, QUESTION),
    'teleportclosest': (diamond, MINT, TARGET),
    'break': (octagon, PEACH, SLAM),
    # не буст: кнопка случайного стартового буста в окне выбора
    'random': (rounded_square, BUTTER, DICE),
}


def rgba(hex_color):
    return tuple(int(hex_color[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def render(shape, pal, glyph):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    inside = lambda x, y: 0 <= x < 16 and 0 <= y < 16 and shape(x, y)
    is_edge = lambda x, y: inside(x, y) and not all(
        inside(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))

    for y in range(16):
        for x in range(16):
            if not inside(x, y):
                continue
            if is_edge(x, y):
                color = pal['outline']
            elif is_edge(x, y - 1) or is_edge(x - 1, y):
                color = pal['light']  # блик сверху-слева
            elif is_edge(x, y + 1) or is_edge(x + 1, y):
                color = pal['shade']  # тень снизу-справа
            else:
                color = pal['fill']
            img.putpixel((x, y), rgba(color))

    h, w = len(glyph), len(glyph[0])
    center = getattr(shape, 'center', C)
    ox, oy = int(center + .5 - w / 2), int(center + .5 - h / 2)
    for gy, row in enumerate(glyph):
        assert len(row) == w, row
        for gx, ch in enumerate(row):
            if ch == '#':
                img.putpixel((ox + gx, oy + gy), rgba(pal['glyph']))
            elif ch == 'o':
                img.putpixel((ox + gx, oy + gy), rgba(pal['light']))
    return img


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    images = {}
    for name, (shape, pal, glyph) in ICONS.items():
        images[name] = render(shape, pal, glyph)
        images[name].save(os.path.join(OUT, f'{name}.png'))

    # превью: иконки x16 в слотах как в инвентаре
    S, PAD = 16, 32
    prev = Image.new('RGBA', (len(images) * (16 * S + PAD) + PAD, 16 * S + 2 * PAD), (198, 198, 198, 255))
    for i, im in enumerate(images.values()):
        x = PAD + i * (16 * S + PAD)
        prev.paste((139, 139, 139, 255), (x, PAD, x + 16 * S, PAD + 16 * S))
        prev.alpha_composite(im.resize((16 * S, 16 * S), Image.NEAREST), (x, PAD))
    prev.save(os.path.join(ROOT, 'preview.png'))
