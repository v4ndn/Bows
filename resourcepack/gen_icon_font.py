"""Генерирует шрифт иконок бустов для меню /info.

Каждому бусту — свой символ шрифта bows:boost_icons: битмап из текстуры его
предмета высотой 16px (в тексте обычные иконки 8px). Пробел этого шрифта —
отступ между иконками. Таблица «id буста -> символ» пишется в ресурсы плагина.

Запуск после добавления бустов: python3 gen_icon_font.py
"""
import json
import os
import re

ROOT = os.path.dirname(os.path.abspath(__file__))
TYPES = os.path.join(ROOT, '..', 'bows', 'src', 'main', 'java', 'com', 'v4nden', 'bows', 'Boosts', 'Types')
FONT = os.path.join(ROOT, 'assets', 'bows', 'font', 'boost_icons.json')
# те же символы размером со строку текста — для таба
SMALL_FONT = os.path.join(ROOT, 'assets', 'bows', 'font', 'boost_icons_small.json')
TABLE = os.path.join(ROOT, '..', 'bows', 'src', 'main', 'resources', 'boost_icons.properties')

FIRST_CODEPOINT = 0xE100
ICON_HEIGHT = 16
ICON_ASCENT = 12
SMALL_ICON_HEIGHT = 8
SMALL_ICON_ASCENT = 7
# видимый зазор = advance пробела + 1px, который шрифт добавляет после символа
GAP_ADVANCE = 10

# предметы-блоки: текстуры предмета нет, берём текстуру блока
BLOCK_TEXTURES = {'glass': 'minecraft:block/glass.png', 'tnt': 'minecraft:block/tnt_side.png'}


def texture_of(source):
    icon = re.search(r'\.setIcon\("([a-z0-9_]+)"\)', source)
    if icon:
        return f'bows:item/{icon.group(1)}.png'
    material = re.search(r'\.setMaterial\(Material\.([A-Z0-9_]+)\)', source).group(1).lower()
    return BLOCK_TEXTURES.get(material, f'minecraft:item/{material}.png')


def boost_id(source):
    literal = re.search(r'\.setId\("([a-z0-9_]+)"\)', source)
    if literal:
        return literal.group(1)
    constant = re.search(r'\.setId\((\w+)\)', source).group(1)
    return re.search(constant + r'\s*=\s*"([a-z0-9_]+)"', source).group(1)


boosts = {}
for name in sorted(os.listdir(TYPES)):
    source = open(os.path.join(TYPES, name), encoding='utf-8').read()
    if '.setMaterial(' not in source:
        continue
    boosts[boost_id(source)] = texture_of(source)

# иконки не бустов (кнопки в диалогах)
EXTRA_ICONS = {'random': 'bows:item/random.png'}

# коды символов только добавляются, а не пересчитываются: у игроков может быть
# старый пак (сервер раздаёт тот, что был при его запуске), и сдвиг кодов
# показал бы чужие иконки. Новые иконки получают следующий свободный код.
codepoints = {}
if os.path.exists(TABLE):
    for line in open(TABLE, encoding='utf-8'):
        if '=' in line and not line.startswith('#'):
            key, value = line.strip().split('=')
            codepoints[key] = int(value, 16)
icons = dict(sorted(boosts.items()))
icons.update(EXTRA_ICONS)
next_codepoint = max(codepoints.values(), default=FIRST_CODEPOINT - 1) + 1
for icon in icons:
    if icon not in codepoints:
        codepoints[icon] = next_codepoint
        next_codepoint += 1

providers = [{'type': 'space', 'advances': {' ': GAP_ADVANCE}}]
small_providers = []
table = []
for icon, codepoint in sorted(codepoints.items(), key=lambda item: item[1]):
    if icon not in icons:
        continue  # буст удалён — код остаётся занятым, но символ больше не нужен
    char = chr(codepoint)
    texture = icons[icon]
    providers.append({'type': 'bitmap', 'file': texture, 'ascent': ICON_ASCENT, 'height': ICON_HEIGHT,
                      'chars': [char]})
    small_providers.append({'type': 'bitmap', 'file': texture, 'ascent': SMALL_ICON_ASCENT,
                            'height': SMALL_ICON_HEIGHT, 'chars': [char]})
    table.append(f'{icon}={codepoint:04X}')

os.makedirs(os.path.dirname(FONT), exist_ok=True)
with open(FONT, 'w', encoding='utf-8') as f:
    json.dump({'providers': providers}, f, indent=2, ensure_ascii=False)
    f.write('\n')
with open(SMALL_FONT, 'w', encoding='utf-8') as f:
    json.dump({'providers': small_providers}, f, indent=2, ensure_ascii=False)
    f.write('\n')
with open(TABLE, 'w', encoding='utf-8') as f:
    f.write('# сгенерировано resourcepack/gen_icon_font.py\n' + '\n'.join(table) + '\n')
print(f'{len(table)} icons')
