package com.v4nden.bows.Boosts.Types;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;

import net.md_5.bungee.api.ChatColor;

public class GlowAllBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.GLOWSTONE_DUST)
                .setRarity(5)
                .setId("glowall")
                .setName("Подсветить всех игроков")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Спустя задержку перед применением\n выдаёт всем игрокам эффект\n свечения, звук задержки перед\n эффектом слышан как владельцу,\n так и всем игрокам")
                .setRunnable(consumer -> {
                    Game.gamePlayers.forEach((gamePlayer) -> {
                        gamePlayer.playSound(consumer.getLocation(), Sound.ENTITY_WITHER_SPAWN, 10, 0);
                    });
                    Game.targets().forEach((gamePlayer) -> {
                        gamePlayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 15, 1));
                        gamePlayer.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20, 1));
                    });
                })
                .setDelay(1.5f)
                .build(player);
    }
}
