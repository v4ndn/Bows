package com.v4nden.bows.Boosts.Types;

import java.util.Comparator;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;

import net.md_5.bungee.api.ChatColor;

public class GlowClosestBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.GLOWSTONE_DUST)
                .setRarity(2)
                .setId("glowclosest")
                .setName("Подсветить ближайшего игрока")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Спустя задержку перед применением\n выдаёт ближайшему к владельцу\n игроку эффект свечения, звук\n задержки перед эффектом\n слышан как владельцу, так и\n ближайшему игроку")
                .setRunnable(consumer -> {
                    LivingEntity closest = Game.targetsExcept(consumer).stream()
                            .min(Comparator.comparingDouble(
                                    (gamePlayer) -> gamePlayer.getLocation().distance(consumer.getLocation())))
                            .orElse(null);
                    if (closest == null) {
                        return;
                    }

                    closest.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 5, 1));
                })
                .setDelay(1)
                .build(player);
    }
}
