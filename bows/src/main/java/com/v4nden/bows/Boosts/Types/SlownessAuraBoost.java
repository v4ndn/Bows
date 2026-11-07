package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;

import net.md_5.bungee.api.ChatColor;

public class SlownessAuraBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.GRAY_DYE)
                .setRarity(2)
                .setId("slownessaura")
                .setPreviewRadius(20)
                .setName("Аура замедления")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Создаёт вокруг владельца ауру\n эффекта, все игроки находящиеся в радиусе\n действия ауры получают эффект\n замедления. Эффект не действует\n на владельца буста")
                .setRunnable(consumer -> {
                    int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {
                        Location location = consumer.getLocation();
                        for (double i = 0; i <= Math.PI; i += Math.PI / 20) {
                            double radius = Math.sin(i) * 20;
                            double y = Math.cos(i) * 20;
                            for (double a = 0; a < Math.PI * 2; a += Math.PI / 20) {
                                double x = Math.cos(a) * radius;
                                double z = Math.sin(a) * radius;

                                Game.targetsFor(consumer).forEach((gamePlayer) -> {
                                    if (gamePlayer != consumer) {
                                        if (gamePlayer.getLocation().distance(consumer.getLocation()) < 20) {
                                            gamePlayer.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1));
                                        }
                                    }
                                });

                                consumer.getLocation().getWorld().spawnParticle(Particle.DUST,
                                        location.clone().add(x + Math.random() * 3,
                                                y + Math.random() * 3,
                                                z + Math.random() * 3),
                                        1,
                                        new Particle.DustOptions(
                                                Color.fromRGB(120, 120, 120), 5));
                            }
                        }
                    }, 0, 5L);

                    Bukkit.getScheduler().runTaskLater(Bows.instance, () -> {
                        Bukkit.getScheduler().cancelTask(task);
                    }, 120L);
                })
                .setDelay(3)
                .build(player);
    }
}
