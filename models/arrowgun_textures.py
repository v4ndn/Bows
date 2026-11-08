"""Текстуры модели «Стреломёта» (models/arrowgun.bbmodel): жёлтая полупрозрачная
сфера и стрелы, которые бьются изнутри в её стенку.

arrowgun_sphere.png      — 32x32 на кадр, 32 кадра: шар с бликом и мерцанием
arrowgun_arrow_<a|b|c>.png — 16x16 на кадр, 32 кадра: стрела вдоль средней строки,
                             наконечник упирается в стенку и пробивает её наружу;
                             у a/b/c одна анимация со сдвигом фазы

Анимация — 2 тика на кадр. Запуск: python3 arrowgun_textures.py <папка>
"""
import math
import os
import sys

from PIL import Image

FRAMES = 32

# позиция наконечника по кадрам (столбец текстуры, стенка сферы — около 8)
TIP = [8, 8, 9, 8, 8, 10, 12, 11, 9, 8,
       8, 7, 8, 8, 9, 11, 14, 15, 15, 13,
       11, 9, 8, 8, 9, 8, 7, 8, 9, 8,
       8, 8]
PHASES = {'a': 0, 'b': 11, 'c': 21}

SHAFT = (160, 136, 103)
HEAD = (154, 160, 166)
HEAD_EDGE = (127, 133, 140)
FLETCH = (216, 208, 196)
FLETCH_DARK = (190, 182, 170)

SPHERE = (252, 214, 58)
SPHERE_RIM = (238, 176, 36)
SPHERE_LIGHT = (255, 242, 176)


def arrow_frame(tip):
    image = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    put = lambda x, y, c: 0 <= x < 16 and image.putpixel((x, y), c + (255,))
    tail = tip - 11
    for x in range(tail + 1, tip - 2):
        put(x, 7, SHAFT)
    put(tip, 7, HEAD)
    for y in (6, 7, 8):
        put(tip - 1, y, HEAD)
    for y in (5, 9):
        put(tip - 2, y, HEAD_EDGE)
    for y in (6, 7, 8):
        put(tip - 2, y, HEAD)
    for dx, (up, down) in enumerate(((5, 9), (6, 8))):
        put(tail + dx, up, FLETCH)
        put(tail + dx, down, FLETCH)
        put(tail + dx + 1, up, FLETCH_DARK)
        put(tail + dx + 1, down, FLETCH_DARK)
    return image


def arrow_sheet(phase):
    sheet = Image.new('RGBA', (16, 16 * FRAMES), (0, 0, 0, 0))
    for f in range(FRAMES):
        sheet.paste(arrow_frame(TIP[(f + phase) % FRAMES]), (0, f * 16))
    return sheet


def sphere_sheet():
    size = 32
    sheet = Image.new('RGBA', (size, size * FRAMES), (0, 0, 0, 0))
    c = (size - 1) / 2
    radius = 15.5
    for f in range(FRAMES):
        t = f / FRAMES * math.pi * 2
        pulse = 1 + 0.03 * math.sin(t)
        for y in range(size):
            for x in range(size):
                dx, dy = x - c, y - c
                d = math.hypot(dx, dy) / (radius * pulse)
                if d > 1:
                    continue
                angle = math.atan2(dy, dx)
                # прозрачная середина, плотный обод (как у стекла), лёгкие бегущие разводы
                alpha = 0.24 + 0.5 * d ** 3 + 0.025 * math.sin(3 * angle + 5 * d - t * 2)
                color = tuple(round(a + (b - a) * d ** 2) for a, b in zip(SPHERE, SPHERE_RIM))
                # блик сверху слева
                hl = math.hypot(dx / radius + 0.38, dy / radius + 0.38) / 0.28
                if hl < 1:
                    k = (1 - hl) ** 1.5 * (0.85 + 0.15 * math.sin(t))
                    color = tuple(round(a + (b - a) * k) for a, b in zip(color, SPHERE_LIGHT))
                    alpha += 0.3 * k
                sheet.putpixel((x, f * size + y), color + (round(min(alpha, 0.85) * 255),))
    return sheet


if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else '.'
    os.makedirs(out, exist_ok=True)
    sphere_sheet().save(os.path.join(out, 'arrowgun_sphere.png'))
    for name, phase in PHASES.items():
        arrow_sheet(phase).save(os.path.join(out, f'arrowgun_arrow_{name}.png'))
