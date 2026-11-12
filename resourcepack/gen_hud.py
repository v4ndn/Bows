"""Генерирует шрифт bows:hud — элемент статуса на месте полоски опыта (Game/StatusBar.java).

Элемент шириной 182px (как полоска опыта) и высотой 7px:
  круг бустов (сколько из 10) | круг времени до следующего буста |
  круг «насколько смотришь на центр» | полоса барьера с узором.

Узор полосы — диагональные полосы с периодом PATTERN по x. Заливка собирается
из кусков 128, 64, …, 1 по убыванию, поэтому кусок от PATTERN px и шире всегда
начинается на кратной PATTERN позиции и узор на стыках совпадает; для куска 1px
есть второй вариант со сдвигом фазы на 2 (после куска 2px).
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
DIRECTION_STATES = 16
RINGS = 3
BAR_WIDTH = 182 - RINGS * (RING + 2)
PATTERN = 4
FILL_PIECES = [1, 2, 4, 8, 16, 32, 64, 128]
SPACES = [-BAR_WIDTH, -1, 1, 2, 4, 8, 16, 32, 64, 128]

# коды символов (должны совпадать с StatusBar.java)
BOOST_FIRST = 0xE300
TIMER_FIRST = 0xE310
FRAME = 0xE320
FILL_FIRST = 0xE321
FILL_1_SHIFTED = 0xE329
SPACE_FIRST = 0xE330
DIRECTION_FIRST = 0xE340

BACKGROUND = (0, 0, 0, 130)
BORDER = (0, 0, 0, 200)
BOOST_COLOR = (233, 114, 24, 255)
TIMER_COLOR = (244, 235, 229, 255)
DIRECTION_COLOR = (90, 200, 255, 255)
BAR_LIGHT = (240, 150, 82, 255)
BAR_COLOR = (226, 128, 56, 255)
BAR_DARK = (196, 104, 44, 255)
BACKGROUND_STRIPE = (0, 0, 0, 145)
STRIPE_LIGHTEN = 0.12
DONUT_EMPTY = (40, 40, 40, 170)
DONUT_OUTER = 3.5
DONUT_INNER = 1.5


def shade(color, amount):
    """amount > 0 — светлее, < 0 — темнее."""
    if amount >= 0:
        return lighten(color, amount)
    return tuple(round(c * (1 + amount)) for c in color[:3]) + (color[3],)


def ring(fraction, color):
    """Пончик 7×7: кольцо с дыркой в центре, заполняется сектором по часовой от верха.
    Свет сверху слева: на верхне-левой стороне кольца светлее, на нижне-правой темнее,
    плюс объём трубки — середина кольца светлее краёв."""
    image = Image.new('RGBA', (RING, RING))
    center = (RING - 1) / 2
    for y in range(RING):
        for x in range(RING):
            dx, dy = x - center, y - center
            distance = math.hypot(dx, dy)
            if distance > DONUT_OUTER or distance < DONUT_INNER:
                continue
            angle = (math.degrees(math.atan2(dx, -dy)) + 360) % 360
            filled = fraction >= 1 or (fraction > 0 and angle <= fraction * 360)
            light = (-dx - dy) / (distance * math.sqrt(2))
            tube = 1 - abs(distance - (DONUT_OUTER + DONUT_INNER) / 2) / ((DONUT_OUTER - DONUT_INNER) / 2)
            amount = light * 0.22 + (tube - 0.5) * 0.2
            image.putpixel((x, y), shade(color if filled else DONUT_EMPTY, amount))
    return image


def lighten(color, amount):
    return tuple(round(c + (255 - c) * amount) for c in color[:3]) + (color[3],)


def stripe(x, y):
    # x — от начала заливки; диагональ шириной 2 из каждых PATTERN
    return (x + y) % PATTERN < 2


def frame():
    image = Image.new('RGBA', (BAR_WIDTH, HEIGHT))
    for y in range(1, HEIGHT - 1):
        for x in range(BAR_WIDTH):
            left, right = x == 0, x == BAR_WIDTH - 1
            top, bottom = y == 1, y == HEIGHT - 2
            # скруглённые углы: угловых пикселей рамки нет
            if (left or right) and (top or bottom):
                continue
            if left or right or top or bottom:
                image.putpixel((x, y), BORDER)
            else:
                image.putpixel((x, y), BACKGROUND_STRIPE if stripe(x - 1, y) else BACKGROUND)
    return image


def fill(width, phase=0):
    # заливка внутри рамки, строки 2..4: светлый верх, тёмный низ, поверх — светлые диагонали
    image = Image.new('RGBA', (width, HEIGHT))
    for y in range(2, HEIGHT - 2):
        for x in range(width):
            shade = {2: BAR_LIGHT, 3: BAR_COLOR}.get(y, BAR_DARK)
            image.putpixel((x, y), lighten(shade, STRIPE_LIGHTEN) if stripe(x + phase, y) else shade)
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
for i in range(DIRECTION_STATES):
    providers.append(bitmap(f'direction_{i}.png', ring(i / (DIRECTION_STATES - 1), DIRECTION_COLOR),
                            DIRECTION_FIRST + i))
providers.append(bitmap('frame.png', frame(), FRAME))
for i, width in enumerate(FILL_PIECES):
    providers.append(bitmap(f'fill_{width}.png', fill(width), FILL_FIRST + i))
providers.append(bitmap('fill_1_shifted.png', fill(1, 2), FILL_1_SHIFTED))
providers.append({'type': 'space',
                  'advances': {chr(SPACE_FIRST + i): advance for i, advance in enumerate(SPACES)}})

with open(FONT, 'w', encoding='utf-8') as f:
    json.dump({'providers': providers}, f, ensure_ascii=False, indent=2)
print(f'{len(providers)} providers -> {FONT}')
