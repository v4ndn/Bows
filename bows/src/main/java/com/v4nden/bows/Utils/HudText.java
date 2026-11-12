package com.v4nden.bows.Utils;

import java.time.Duration;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;

/**
 * Анимированный текст над прицелом. Отправляется тайтлом с цветами-метками,
 * которые распознаёт шейдер ресурспака (assets/minecraft/shaders/core/rendertype_text.vsh):
 * он переносит текст на ~200px выше прицела и анимирует появление и скрытие
 * по альфе тайтла. Без ресурспака текст покажется обычным тайтлом.
 *
 * Слово «@ник» онлайн-игрока заменяется головой его скина (объектный компонент 1.21.9+):
 * её рисует клиент, метки цвета на ней те же.
 */
public class HudText {
    // должны совпадать с HUD_TEXT_MARKER и HUD_SHADOW_MARKER в шейдере
    private static final TextColor TEXT_MARKER = TextColor.color(254, 1, 0);
    private static final ShadowColor SHADOW_MARKER = ShadowColor.shadowColor(254, 2, 0, 255);

    private static final int ANIMATION_TICKS = 10;

    public static void show(Player player, String text, double seconds) {
        Component title = withHeads(text).color(TEXT_MARKER).shadowColor(SHADOW_MARKER).build();
        Duration animation = Duration.ofMillis(ANIMATION_TICKS * 50L);
        // пустой подзаголовок: иначе старый показался бы вместе с текстом.
        // Через Paper, а не PacketEvents: PacketEvents не умеет головы в тексте
        player.showTitle(Title.title(title, Component.empty(),
                Title.Times.times(animation, Duration.ofMillis(Math.round(seconds * 1000)), animation)));
    }

    // текст по словам; «@ник» онлайн-игрока — голова его скина
    private static TextComponent.Builder withHeads(String text) {
        TextComponent.Builder builder = Component.text();
        StringBuilder plain = new StringBuilder();
        for (String word : text.split(" ", -1)) {
            Player head = word.length() > 1 && word.startsWith("@") ? Bukkit.getPlayerExact(word.substring(1)) : null;
            if (head == null) {
                plain.append(word).append(' ');
                continue;
            }
            builder.append(Component.text(plain.toString()));
            plain.setLength(0);
            builder.append(PlayerHeads.head(head));
            plain.append(' ');
        }
        builder.append(Component.text(plain.toString().stripTrailing()));
        return builder;
    }
}
