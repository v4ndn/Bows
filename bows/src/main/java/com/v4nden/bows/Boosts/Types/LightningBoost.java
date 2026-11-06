package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;

import com.v4nden.bows.BowsUtils;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Utils.TemporaryListener;

import net.md_5.bungee.api.ChatColor;

public class LightningBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.PRISMARINE_CRYSTALS)
                .setRarity(2)
                .setId("lightning")
                .setName("Молния")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Бросает заряд в сторону\n направления головы игрока,\n в точку где заряд\n контактировал с блоком\n бьёт молния")
                .setRunnable(consumer -> {
                    Snowball charge = BowsUtils.lauchCharge(consumer, Material.PRISMARINE_CRYSTALS);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,
                                SoundCategory.MASTER, 1,
                                1);
                    }
                    new TemporaryListener<>(ProjectileHitEvent.class,
                            EventPriority.NORMAL, e -> {
                                ProjectileHitEvent event = (ProjectileHitEvent) e;
                                if (event.getEntity().equals(charge)) {
                                    charge.getWorld().strikeLightning(charge.getLocation()).setCausingPlayer(consumer);
                                    return true;
                                }
                                return false;
                            });
                })
                .build(player);
    }
}
