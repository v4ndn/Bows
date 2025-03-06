package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.v4nden.bows.Bows;
import com.v4nden.bows.BowsUtils;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Utils.TemporaryListener;

import net.md_5.bungee.api.ChatColor;

public class RainBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.ORANGE_DYE)
                .setRarity(3)
                .setId("rain")
                .setName("Дождь из стрел")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:"
                        + ChatColor.RESET
                        + "\n Выпускает заряд в сторону\n направления головы игрока, \n на точке где заряд \n контактировал с блоком\n вызывает дождь из стрел\n в радиусе 15ти блоков")
                .setRunnable(consumer -> {

                    Snowball charge = BowsUtils.lauchCharge(consumer, Material.ORANGE_DYE);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,
                                SoundCategory.MASTER, 1,
                                1);
                    }
                    new TemporaryListener<>(ProjectileHitEvent.class,
                            EventPriority.NORMAL, e -> {
                                ProjectileHitEvent event = (ProjectileHitEvent) e;
                                if (event.getEntity().equals(charge)) {
                                    Location loc = event.getEntity().getLocation();

                                    for (Player p : Bukkit.getOnlinePlayers()) {
                                        p.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST,
                                                SoundCategory.MASTER, 5,
                                                1);
                                        p.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,
                                                SoundCategory.MASTER, 5,
                                                1);
                                        p.playSound(loc, Sound.ITEM_GOAT_HORN_SOUND_6,
                                                SoundCategory.MASTER, 5,
                                                1);
                                    }

                                    // for (int i = 0; i <= 10; i++) {
                                    // final int fi = Integer.valueOf(i);
                                    // Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                                    // BowsUtils.spawnParticleCircle(Particle.CLOUD, loc.clone().add(0, 15, 0),
                                    // 15 * Math.abs(Math.sin(fi * 3)));
                                    // }, fi * 20);
                                    // }

                                    // for (int i = 0; i <= 10; i++) {
                                    // final int fi = Integer.valueOf(i);
                                    // Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                                    // BowsUtils.spawnParticleCircle(Particle.CLOUD, loc.clone().add(0, 15, 0),
                                    // 15 * Math.abs(Math.sin(fi * 3)));
                                    // }, fi * 20);
                                    // }

                                    for (int i = 0; i <= 100; i += 2) {
                                        Bukkit.getScheduler().runTaskLater(Bows.instance, () -> {
                                            for (int c = 0; c < 10; c++) {

                                                double angle = Math.random() * 2 * Math.PI;
                                                double distance = Math.sqrt(Math.random()) * 15;
                                                Arrow arrow = loc.getWorld().spawnArrow(
                                                        loc.clone()
                                                                .add(distance * Math.cos(angle), 100,
                                                                        distance * Math.sin(angle)),
                                                        new Vector(0, -3, 0),
                                                        Double.valueOf(3f * Math.random()).floatValue(), .1f);
                                                Bukkit.getScheduler().runTaskLater(Bows.instance, () -> {
                                                    arrow.remove();
                                                    arrow.getWorld().playSound(arrow.getLocation(),
                                                            Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, .1f,
                                                            Double.valueOf(2 * Math.random()).floatValue());
                                                }, 20L * 15);
                                            }

                                        }, i);
                                    }
                                    return true;
                                }
                                return false;

                            });

                })
                .build(player);
    }
}
