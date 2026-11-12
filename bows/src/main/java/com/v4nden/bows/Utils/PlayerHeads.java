package com.v4nden.bows.Utils;

import org.bukkit.entity.Player;

import com.destroystokyo.paper.profile.ProfileProperty;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.object.ObjectContents;
import net.kyori.adventure.text.object.PlayerHeadObjectContents;

/**
 * Голова скина игрока как символ в тексте (объектный компонент, клиент 1.21.9+).
 *
 * Сервер офлайн: по UUID клиент искал бы скин у Mojang и не нашёл бы. Поэтому
 * в компонент кладётся готовое свойство textures из профиля игрока — его ставит
 * SkinsRestorer, — и клиент качает скин прямо по ссылке из него.
 */
public class PlayerHeads {

    public static Component head(Player player) {
        PlayerHeadObjectContents.Builder head = ObjectContents.playerHead()
                .id(player.getUniqueId()).name(player.getName());
        for (ProfileProperty property : player.getPlayerProfile().getProperties()) {
            if (property.getName().equals("textures")) {
                head.profileProperty(PlayerHeadObjectContents.property(property.getName(), property.getValue(),
                        property.getSignature()));
            }
        }
        return Component.object(head.build());
    }
}
