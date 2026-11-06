package com.v4nden.bows.Boosts.Types;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Utils.TemporaryListener;

import net.md_5.bungee.api.ChatColor;

public class ArrowgunBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.BLAZE_POWDER)
                .setRarity(4)
                .setId("arrowgun")
                .setName("Стреломёт")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Выпускает шквал из стрел разной\n скорости с разбросом в направлении\n головы игрока, действует 3 секунды")
                .setRunnable(consumer -> {
                    List<Arrow> arrows = new ArrayList<Arrow>();
                    double now = System.currentTimeMillis();

                    int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {
                        consumer.getWorld().playSound(consumer.getLocation(), Sound.ENTITY_ARROW_SHOOT, 2, 1);
                        Location location = consumer.getLocation().add(0, 1.5, 0);
                        for (int i = 0; i < 2; i++) {
                            arrows.add(
                                    location.getWorld().spawnArrow(location, consumer.getLocation().getDirection(),
                                            Double.valueOf(3 * Math.random() + .5).floatValue(), 10));
                        }
                    }, 0, 2L);

                    Bukkit.getScheduler().runTaskLater(Bows.instance, () -> {
                        Bukkit.getScheduler().cancelTask(task);
                    }, 60L);

                    new TemporaryListener<>(ProjectileHitEvent.class, EventPriority.NORMAL, e -> {
                        ProjectileHitEvent event = (ProjectileHitEvent) e;
                        if (System.currentTimeMillis() - now < 15000) {
                            if (event.getHitBlock() != null && arrows.contains(event.getEntity())) {
                                event.getEntity().remove();
                            }
                            return false;
                        } else {
                            arrows.clear();
                            return true;
                        }
                    });
                })
                .setDelay(3)
                .build(player);
    }
}
