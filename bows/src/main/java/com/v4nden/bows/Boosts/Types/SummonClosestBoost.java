package com.v4nden.bows.Boosts.Types;

import java.util.Comparator;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;

import net.md_5.bungee.api.ChatColor;

public class SummonClosestBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.ECHO_SHARD)
                .setRarity(3)
                .setId("summonclosest")
                .setName("Призыв ближайшего игрока")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Спустя 3 секунды\n телепортирует ближайшего\n игрока к владельцу, отсчёт\n видят и слышат как владелец,\n так и призываемый игрок")
                .setRunnable(consumer -> {
                    LivingEntity closest = Game.selectableTargetsExcept(consumer).stream()
                            .filter(target -> target.getWorld().equals(consumer.getWorld()))
                            .min(Comparator.comparingDouble(
                                    target -> target.getLocation().distanceSquared(consumer.getLocation())))
                            .orElse(null);
                    if (closest == null) {
                        return;
                    }
                    Summon.summon(consumer, closest);
                })
                .build(player);
    }
}
