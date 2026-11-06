package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Utils.TemporaryListener;

import net.md_5.bungee.api.ChatColor;

public class FireArrowsBoost implements IBoostType {
    private static final long DURATION_TICKS = 20 * 15L;

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.MAGMA_CREAM)
                .setRarity(3)
                .setId("firearrows")
                .setName("Огненные стрелы")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n На 15 секунд все стрелы,\n выпущенные владельцем\n буста, становятся\n огненными и поджигают\n цель при попадании")
                .setRunnable(consumer -> {
                    consumer.getWorld().spawnParticle(Particle.FLAME, consumer.getLocation().add(0, 1, 0), 60,
                            .6, .8, .6, .03);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ITEM_FIRECHARGE_USE,
                                SoundCategory.MASTER, 1,
                                1);
                    }

                    TemporaryListener<EntityShootBowEvent> shootListener = new TemporaryListener<EntityShootBowEvent>(
                            EntityShootBowEvent.class, EventPriority.NORMAL, e -> {
                                if (e.getEntity().equals(consumer) && e.getProjectile() instanceof AbstractArrow) {
                                    // горит весь полёт, как стрела с зачарованием "Пламя"
                                    e.getProjectile().setFireTicks(20 * 100);
                                }
                            });

                    // пока буст активен, вокруг игрока раз в секунду горит пламя
                    int particleTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {
                        if (consumer.isOnline()) {
                            consumer.getWorld().spawnParticle(Particle.FLAME, consumer.getLocation().add(0, 1, 0), 15,
                                    .4, .6, .4, .01);
                        }
                    }, 20L, 20L);

                    Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                        Bukkit.getScheduler().cancelTask(particleTask);
                        shootListener.unregister();
                    }, DURATION_TICKS);
                })
                .setDelay(1)
                .build(player);
    }
}
