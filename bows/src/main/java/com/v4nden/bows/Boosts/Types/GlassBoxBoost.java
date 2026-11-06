package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;

import net.md_5.bungee.api.ChatColor;

public class GlassBoxBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.GLASS)
                .setRarity(1)
                .setId("glassbox")
                .setName("Коробка")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Создаёт вокруг владельца\n коробку из стекла 5х5х3")
                .setRunnable(consumer -> {
                    for (int y = 0; y < 3; y++) {
                        for (int i = -1; i <= 1; i++) {
                            consumer.getLocation().add(new Vector(i, y, 2)).getBlock().setType(Material.GLASS);
                            consumer.getLocation().add(new Vector(i, y, -2)).getBlock().setType(Material.GLASS);
                            consumer.getLocation().add(new Vector(2, y, i)).getBlock().setType(Material.GLASS);
                            consumer.getLocation().add(new Vector(-2, y, i)).getBlock().setType(Material.GLASS);
                        }
                    }

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.BLOCK_GLASS_PLACE, SoundCategory.MASTER,
                                1,
                                1);
                    }
                })
                .setDelay(.5f)
                .build(player);
    }
}
