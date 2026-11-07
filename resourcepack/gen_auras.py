"""Генерирует анимированные ауры редкостей для 3D-моделей бустов (см. скилл boost-model).

textures/item/aura/aura_rare<N>.png (+ .mcmeta) — 32 кадра 32x32, по 2 тика:
мягкий радиальный градиент цвета редкости с медленной пульсацией и молнии из
центра, затухающие к краю. Чем выше редкость — тем больше молний и плотнее
свечение. У редкости 1 ауры нет.

Запуск: python3 gen_auras.py
"""
import json
import math
import os

from PIL import Image

ROOT = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(ROOT, 'assets', 'bows', 'textures', 'item', 'aura')

SIZE = 32
FRAMES = 32
FRAMETIME = 2

# цвета — как Boost.RARITY_COLORS; редкость 1 без ауры
AURAS = {
    'aura_rare2': {'color': '#4aff65', 'bolts': 3, 'glow': 0.55},
    'aura_rare3': {'color': '#4a89ff', 'bolts': 5, 'glow': 0.62},
    'aura_rare4': {'color': '#834aff', 'bolts': 7, 'glow': 0.67},
    'aura_rare5': {'color': '#e97218', 'bolts': 9, 'glow': 0.72},
}


def rgb(hex_color):
    return tuple(int(hex_color[i:i + 2], 16) for i in (1, 3, 5))


def lighten(color, amount):
    return tuple(round(c + (255 - c) * amount) for c in color)


class Lcg:
    """Тот же генератор, что в скрипте Blockbench, — одинаковые молнии."""

    def __init__(self, seed):
        self.seed = seed

    def next(self):
        self.seed = (self.seed * 1664525 + 1013904223) % 4294967296
        return self.seed / 4294967296


def blend(image, x, y, color, alpha):
    """Накладывает пиксель поверх уже нарисованного (как fillRect в canvas)."""
    r0, g0, b0, a0 = image.getpixel((x, y))
    a0 /= 255
    a = alpha + a0 * (1 - alpha)
    if a <= 0:
        return
    mix = lambda c, c0: round((c * alpha + c0 * a0 * (1 - alpha)) / a)
    image.putpixel((x, y), (mix(color[0], r0), mix(color[1], g0), mix(color[2], b0), round(a * 255)))


def render(color, bolt_count, glow):
    rand = Lcg(1337)
    center = (SIZE - 1) / 2
    radius = SIZE / 2
    bolt_color = lighten(color, 0.6)

    bolts = []
    for i in range(bolt_count):
        start = math.floor(i * FRAMES / bolt_count + rand.next() * 2) % FRAMES
        angle = rand.next() * math.pi * 2
        points = []
        offset = 0
        r = 2.0
        while r < radius - 1:
            offset += (rand.next() - 0.5) * 1.6
            offset = max(-2.5, min(2.5, offset))
            points.append((r, offset))
            r += 0.5
        bolts.append((start, angle, points))

    image = Image.new('RGBA', (SIZE, SIZE * FRAMES), (0, 0, 0, 0))
    for frame in range(FRAMES):
        top = frame * SIZE
        pulse = 0.85 + 0.15 * math.sin(frame / FRAMES * math.pi * 2)
        for y in range(SIZE):
            for x in range(SIZE):
                d = math.hypot(x - center, y - center) / radius
                alpha = max(0.0, 1 - d / pulse) ** 1.25 * glow
                if alpha > 0.015:
                    blend(image, x, top + y, color, alpha)
        for start, angle, points in bolts:
            age = (frame - start + FRAMES) % FRAMES
            if age > 2:
                continue
            flash = (0.95, 0.6, 0.3)[age]
            cos, sin = math.cos(angle), math.sin(angle)
            drawn = set()
            for r, offset in points:
                x = round(center + cos * r - sin * offset)
                y = round(center + sin * r + cos * offset)
                if not (0 <= x < SIZE and 0 <= y < SIZE) or (x, y) in drawn:
                    continue
                drawn.add((x, y))
                fade = max(0.0, 1 - r / radius) ** 1.3
                blend(image, x, top + y, bolt_color, flash * fade)
    return image


if __name__ == '__main__':
    os.makedirs(OUT, exist_ok=True)
    for name, aura in AURAS.items():
        render(rgb(aura['color']), aura['bolts'], aura['glow']).save(os.path.join(OUT, f'{name}.png'))
        with open(os.path.join(OUT, f'{name}.png.mcmeta'), 'w', encoding='utf-8') as f:
            json.dump({'animation': {'frametime': FRAMETIME}}, f, indent=2)
            f.write('\n')
        print(name)
