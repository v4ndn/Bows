package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;

import net.md_5.bungee.api.ChatColor;

public class DashBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.FEATHER)
                .setRarity(1)
                .setId("dash")
                .setName("Рывок")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Запускает владельца\n относительно поворота головы\n вперёд ~30 блоков")
                .setRunnable(consumer -> {
                    consumer.setVelocity(consumer.getLocation().getDirection().multiply((2)));

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP,
                                SoundCategory.MASTER, 1,
                                1);
                    }
                })
                .build(player);
    }
}
