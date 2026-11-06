package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.util.Vector;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;

import net.md_5.bungee.api.ChatColor;

public class TntBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.TNT)
                .setRarity(2)
                .setId("tnt")
                .setName("Динамит")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Запускает заряженый динамит \n в сторону направления\n головы игрока")
                .setRunnable(consumer -> {
                    TNTPrimed tnt = consumer.getWorld().spawn(consumer.getLocation().add(new Vector(0, 1, 0)),
                            TNTPrimed.class);
                    tnt.setSource(consumer);
                    tnt.setVelocity(consumer.getLocation().getDirection().multiply((2)));

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_TNT_PRIMED, SoundCategory.MASTER,
                                1,
                                1);
                        p.playSound(consumer.getLocation(), Sound.ENTITY_SNOWBALL_THROW,
                                SoundCategory.MASTER,
                                1,
                                1);
                    }
                })
                .build(player);
    }
}
