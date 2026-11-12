"""Генерирует шрифт bows:hud — элемент статуса на месте полоски опыта (Game/StatusBar.java).

Элемент шириной 182px (как полоска опыта) и высотой 7px:
  круг бустов (сколько из 10) | круг времени до следующего буста | полоса барьера.
Плагин шлёт его строкой actionbar с цветом-меткой, шейдер rendertype_text.vsh
опускает её на место полоски опыта. Ширины ниже должны совпадать с StatusBar.java.

Глифы bitmap получают ширину по последнему непрозрачному столбцу и шаг на 1px
больше, поэтому после кусков заливки идёт пробел −1 (символы шрифта space).

Запуск: python3 gen_hud.py
"""
import json
import math
import os

from PIL import Image

ROOT = os.path.dirname(os.path.abspath(__file__))
TEXTURES = os.path.join(ROOT, 'assets', 'bows', 'textures', 'font', 'hud')
FONT = os.path.join(ROOT, 'assets', 'bows', 'font', 'hud.json')

HEIGHT = 7
RING = 7
BOOST_STATES = 11  # 0..10 бустов
TIMER_STATES = 16
BAR_WIDTH = 164
FILL_PIECES = [1, 2, 4, 8, 16, 32, 64, 128]
SPACES = [-BAR_WIDTH, -1, 1, 2, 4, 8, 16, 32, 64, 128]

# коды символов (должны совпадать с StatusBar.java)
BOOST_FIRST = 0xE300
TIMER_FIRST = 0xE310
FRAME = 0xE320
FILL_FIRST = 0xE321
SPACE_FIRST = 0xE330

BACKGROUND = (0, 0, 0, 130)
BORDER = (0, 0, 0, 200)
BOOST_COLOR = (233, 114, 24, 255)
TIMER_COLOR = (244, 235, 229, 255)
BAR_COLOR = (224, 91, 75, 255)


def ring(fraction, color):
    """Круг 7×7: тёмный диск, заполненный сектором по часовой от верха."""
    image = Image.new('RGBA', (RING, RING))
    center = (RING - 1) / 2
    for y in range(RING):
        for x in range(RING):
            dx, dy = x - center, y - center
            if dx * dx + dy * dy > (RING / 2) ** 2:
                continue
            angle = (math.degrees(math.atan2(dx, -dy)) + 360) % 360
            filled = fraction >= 1 or (fraction > 0 and angle <= fraction * 360)
            image.putpixel((x, y), color if filled else BACKGROUND)
    return image


def frame():
    image = Image.new('RGBA', (BAR_WIDTH, HEIGHT))
    for y in range(1, HEIGHT - 1):
        for x in range(BAR_WIDTH):
            border = x == 0 or x == BAR_WIDTH - 1 or y == 1 or y == HEIGHT - 2
            image.putpixel((x, y), BORDER if border else BACKGROUND)
    return image


def fill(width):
    # заливка внутри рамки: строки 2..4 из 7
    image = Image.new('RGBA', (width, HEIGHT))
    for y in range(2, HEIGHT - 2):
        for x in range(width):
            image.putpixel((x, y), BAR_COLOR)
    return image


def bitmap(path, image, char):
    image.save(os.path.join(TEXTURES, path))
    return {'type': 'bitmap', 'file': f'bows:font/hud/{path}', 'ascent': HEIGHT, 'height': HEIGHT,
            'chars': [chr(char)]}


os.makedirs(TEXTURES, exist_ok=True)
providers = []
for i in range(BOOST_STATES):
    providers.append(bitmap(f'boosts_{i}.png', ring(i / (BOOST_STATES - 1), BOOST_COLOR), BOOST_FIRST + i))
for i in range(TIMER_STATES):
    providers.append(bitmap(f'timer_{i}.png', ring(i / (TIMER_STATES - 1), TIMER_COLOR), TIMER_FIRST + i))
providers.append(bitmap('frame.png', frame(), FRAME))
for i, width in enumerate(FILL_PIECES):
    providers.append(bitmap(f'fill_{width}.png', fill(width), FILL_FIRST + i))
providers.append({'type': 'space',
                  'advances': {chr(SPACE_FIRST + i): advance for i, advance in enumerate(SPACES)}})

with open(FONT, 'w', encoding='utf-8') as f:
    json.dump({'providers': providers}, f, ensure_ascii=False, indent=2)
print(f'{len(providers)} providers -> {FONT}')
