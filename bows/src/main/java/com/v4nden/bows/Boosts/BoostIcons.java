package com.v4nden.bows.Boosts;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import com.v4nden.bows.Bows;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.ShadowColor;

/**
 * Иконки бустов как символы шрифтов ресурспака (resourcepack/gen_icon_font.py):
 * bows:boost_icons — 16px для меню /info, bows:boost_icons_small — 8px для таба.
 */
public class BoostIcons {
    public static final Key LARGE_FONT = Key.key("bows", "boost_icons");
    public static final Key SMALL_FONT = Key.key("bows", "boost_icons_small");
    // ширина маленькой иконки в табе: 8px картинка + 1px после символа
    public static final int SMALL_WIDTH = 9;

    private static final Properties CODEPOINTS = load();

    public static Component icon(String boostId, Key font) {
        int codepoint = Integer.parseInt(CODEPOINTS.getProperty(boostId, "FFFD"), 16);
        return Component.text(new String(Character.toChars(codepoint)))
                .font(font)
                .color(NamedTextColor.WHITE)
                .shadowColor(ShadowColor.none());
    }

    private static Properties load() {
        Properties codepoints = new Properties();
        try (InputStream in = Bows.instance.getResource("boost_icons.properties")) {
            if (in != null) {
                codepoints.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            Bows.instance.getLogger().warning("Не удалось загрузить boost_icons.properties: " + e.getMessage());
        }
        return codepoints;
    }
}
