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

public class GlassPilarBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.GLASS)
                .setRarity(1)
                .setId("glasspilar")
                .setName("Столб")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Создаёт под владельцем\n столб из стекла 3х3х4\n и перемещает игрока на\n его верх")
                .setRunnable(consumer -> {
                    for (int y = 0; y < 4; y++) {
                        for (int x = -1; x <= 1; x++) {
                            for (int z = -1; z <= 1; z++) {
                                consumer.getLocation().add(new Vector(x, y, z)).getBlock()
                                        .setType(Material.GLASS);
                            }
                        }
                    }
                    consumer.teleport(consumer.getLocation().add(0, 4, 0));

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
