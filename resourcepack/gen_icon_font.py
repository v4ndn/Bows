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
TABLE = os.path.join(ROOT, '..', 'bows', 'src', 'main', 'resources', 'boost_icons.properties')

FIRST_CODEPOINT = 0xE100
ICON_HEIGHT = 16
ICON_ASCENT = 12
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

providers = [{'type': 'space', 'advances': {' ': GAP_ADVANCE}}]
table = []
for index, (boost, texture) in enumerate(sorted(boosts.items())):
    char = chr(FIRST_CODEPOINT + index)
    providers.append({'type': 'bitmap', 'file': texture, 'ascent': ICON_ASCENT, 'height': ICON_HEIGHT,
                      'chars': [char]})
    table.append(f'{boost}={FIRST_CODEPOINT + index:04X}')

os.makedirs(os.path.dirname(FONT), exist_ok=True)
with open(FONT, 'w', encoding='utf-8') as f:
    json.dump({'providers': providers}, f, indent=2, ensure_ascii=False)
    f.write('\n')
with open(TABLE, 'w', encoding='utf-8') as f:
    f.write('# сгенерировано resourcepack/gen_icon_font.py\n' + '\n'.join(table) + '\n')
print(f'{len(table)} icons')
