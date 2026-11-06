package com.v4nden.bows.Boosts.Types;

import java.util.Comparator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;

import net.md_5.bungee.api.ChatColor;

public class LocatorBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.ECHO_SHARD)
                .setRarity(1)
                .setId("locator")
                .setName("Локатор")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Воспроизводит звук эха на позиции\n ближайшего от владельца игрока,\n действует 10 секунд")
                .setRunnable(consumer -> {
                    LivingEntity closest = Game.selectableTargetsExcept(consumer).stream()
                            .min(Comparator.comparingDouble(
                                    (gamePlayer) -> gamePlayer.getLocation().distance(consumer.getLocation())))
                            .orElse(null);
                    if (closest == null) {
                        return;
                    }

                    for (int i = 0; i <= 20; i++) {
                        final Integer fi = Integer.valueOf(i);
                        Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                            consumer.getWorld().playSound(closest.getLocation(), Sound.ENTITY_ALLAY_ITEM_THROWN,
                                    5,
                                    Double.valueOf((2.1 - (((fi + 1) % 5) / 2.0))).floatValue());
                        }, i * 10);
                    }
                })
                .build(player);
    }
}
