package com.v4nden.bows.Utils;

import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleSubtitle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleText;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetTitleTimes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * Анимированный текст над прицелом. Отправляется тайтлом с цветами-метками,
 * которые распознаёт шейдер ресурспака (assets/minecraft/shaders/core/rendertype_text.vsh):
 * он переносит текст на ~200px выше прицела и анимирует появление и скрытие
 * по альфе тайтла. Без ресурспака текст покажется обычным тайтлом.
 */
public class HudText {
    // должны совпадать с HUD_TEXT_MARKER и HUD_SHADOW_MARKER в шейдере
    private static final TextColor TEXT_MARKER = TextColor.color(254, 1, 0);
    private static final ShadowColor SHADOW_MARKER = ShadowColor.shadowColor(254, 2, 0, 255);

    private static final int ANIMATION_TICKS = 10;

    public static void show(Player player, String text, double seconds) {
        Component title = Component.text(text).color(TEXT_MARKER).shadowColor(SHADOW_MARKER);
        int stay = (int) Math.round(seconds * 20);

        PlayerManager players = PacketEvents.getAPI().getPlayerManager();
        players.sendPacket(player, new WrapperPlayServerSetTitleTimes(ANIMATION_TICKS, stay, ANIMATION_TICKS));
        // старый подзаголовок иначе показался бы вместе с текстом
        players.sendPacket(player, new WrapperPlayServerSetTitleSubtitle(Component.empty()));
        players.sendPacket(player, new WrapperPlayServerSetTitleText(title));
    }
}
