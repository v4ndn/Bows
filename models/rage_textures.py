"""Текстура модели «Рейджа» (models/rage.bbmodel): тёмный незеритовый молот
с фиолетовым раскалённым ядром, кожаной обмоткой и рогами.

32x32 атлас: у каждой грани свой участок. Палитра низкоконтрастная, без
чистого чёрного и белого. Запуск: python3 rage_textures.py <папка>
"""
import os
import random
import sys

from PIL import Image, ImageDraw

SIZE = 32
random.seed(4187)
image = Image.new('RGBA', (SIZE, SIZE), (0, 0, 0, 0))
pixels = image.load()


def rect(x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            pixels[x, y] = color + (255,)


def noise(x0, y0, x1, y1, base, spread, alpha=255):
    for y in range(y0, y1):
        for x in range(x0, x1):
            d = random.randint(-spread, spread)
            pixels[x, y] = tuple(max(0, min(255, c + d)) for c in base) + (alpha,)


def line(draw, points, color):
    draw.line(points, fill=color + (255,), width=1)


# --- 0..8,0..12: тёмный металл ---
noise(0, 0, 8, 12, (59, 53, 68), 4)
for x in (0, 7):
    for y in range(12):
        pixels[x, y] = (48, 43, 56, 255)
for y in (0, 11):
    for x in range(8):
        pixels[x, y] = (48, 43, 56, 255)
for x, y in ((2, 3), (5, 8)):
    pixels[x, y] = (82, 74, 94, 255)
    pixels[x + 1, y] = (70, 63, 81, 255)

# --- 8..16,0..8: тёмный верх ---
noise(8, 0, 16, 8, (66, 59, 75), 3)
for x in range(8, 16):
    pixels[x, 0] = (52, 46, 60, 255)
    pixels[x, 7] = (52, 46, 60, 255)
for y in range(8):
    pixels[8, y] = (52, 46, 60, 255)
    pixels[15, y] = (52, 46, 60, 255)
for x, y in ((10, 2), (13, 5), (11, 6)):
    pixels[x, y] = (86, 77, 98, 255)

# --- 16..24,0..12: светлый металл с трещинами ---
noise(16, 0, 24, 12, (91, 84, 105), 4)
for y in range(12):
    pixels[16, y] = (73, 67, 85, 255)
    pixels[23, y] = (73, 67, 85, 255)
for y in (0, 11):
    for x in range(16, 24):
        pixels[x, y] = (73, 67, 85, 255)
draw = ImageDraw.Draw(image)
line(draw, [(19, 0), (19, 2), (18, 3), (18, 5), (20, 6), (20, 8), (19, 9), (19, 11)], (139, 86, 184))
line(draw, [(18, 4), (16, 4)], (116, 69, 157))
line(draw, [(20, 7), (22, 7), (23, 6)], (116, 69, 157))
for x, y in ((21, 2), (17, 8)):
    pixels[x, y] = (111, 103, 126, 255)

# --- 24..32,0..12: раскалённое ядро ---
noise(24, 0, 32, 12, (108, 56, 145), 5)
for y in range(12):
    pixels[24, y] = (74, 39, 104, 255)
    pixels[31, y] = (74, 39, 104, 255)
for x in range(24, 32):
    pixels[x, 0] = (74, 39, 104, 255)
    pixels[x, 11] = (74, 39, 104, 255)
for x, y, color in (
    (26, 2, (176, 103, 219)), (27, 2, (198, 134, 235)), (28, 3, (176, 103, 219)),
    (27, 4, (224, 174, 248)), (28, 5, (198, 134, 235)), (29, 6, (176, 103, 219)),
    (28, 7, (224, 174, 248)), (27, 8, (198, 134, 235)), (26, 9, (176, 103, 219)),
    (29, 2, (139, 78, 179)), (25, 5, (139, 78, 179)), (30, 8, (139, 78, 179)),
):
    pixels[x, y] = color + (255,)

# --- 0..8,12..24: кожаная обмотка ---
noise(0, 12, 8, 24, (73, 58, 49), 4)
for y in (12, 15, 18, 21, 23):
    for x in range(8):
        pixels[x, y] = (55, 43, 37, 255)
for x in (2, 5):
    for y in range(12, 24):
        if y % 3:
            pixels[x, y] = (92, 73, 61, 255)

# --- 8..16,12..24: рог ---
noise(8, 12, 16, 24, (88, 78, 72), 4)
for y in (12, 15, 18, 21, 23):
    for x in range(8, 16):
        pixels[x, y] = (69, 61, 56, 255)
for x in range(8, 16):
    pixels[x, 13] = (104, 93, 85, 255)

# --- 16..24,12..24: грань/фаска ---
noise(16, 12, 24, 24, (74, 67, 86), 4)
for y in range(12, 24):
    pixels[16, y] = (57, 52, 67, 255)
    pixels[23, y] = (57, 52, 67, 255)
for x in range(16, 24):
    pixels[x, 12] = (100, 91, 116, 255)
    pixels[x, 23] = (57, 52, 67, 255)

# --- 24..32,12..20: тёмное дно ---
noise(24, 12, 32, 20, (50, 45, 57), 3)
for x in range(24, 32):
    pixels[x, 12] = (41, 37, 47, 255)
    pixels[x, 19] = (41, 37, 47, 255)

# --- 24..32,20..28: светлый верх ---
noise(24, 20, 32, 28, (82, 75, 95), 3)
for x in range(24, 32):
    pixels[x, 20] = (104, 95, 118, 255)
    pixels[x, 27] = (65, 59, 75, 255)
for x, y in ((26, 23), (29, 25)):
    pixels[x, y] = (113, 103, 128, 255)

# --- 8..16,24..32: кончик рога ---
noise(8, 24, 16, 32, (103, 92, 84), 4)
for y in (24, 27, 30, 31):
    for x in range(8, 16):
        pixels[x, y] = (82, 73, 67, 255)
for x in range(8, 16):
    pixels[x, 25] = (120, 108, 98, 255)

# --- 0..8,24..32: тёмный акцент ---
noise(0, 24, 8, 32, (43, 39, 50), 3)
for x in range(8):
    pixels[x, 24] = (55, 49, 63, 255)
    pixels[x, 31] = (35, 32, 41, 255)

# --- 16..24,24..32: тёмная кожаная полоса ---
noise(16, 24, 24, 32, (58, 46, 39), 3)
for y in (24, 27, 30, 31):
    for x in range(16, 24):
        pixels[x, y] = (45, 35, 30, 255)
for x in (18, 21):
    for y in range(24, 32):
        if y % 3:
            pixels[x, y] = (77, 61, 51, 255)

if __name__ == '__main__':
    out = sys.argv[1] if len(sys.argv) > 1 else '.'
    os.makedirs(out, exist_ok=True)
    image.save(os.path.join(out, 'rage_model.png'))
