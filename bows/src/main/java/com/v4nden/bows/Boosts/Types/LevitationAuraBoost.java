package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;

import net.md_5.bungee.api.ChatColor;

public class LevitationAuraBoost implements IBoostType {
    private static final double RADIUS = 10;
    private static final long DURATION_TICKS = 20 * 6L;

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.SHULKER_SHELL)
                .setRarity(3)
                .setId("levitationaura")
                .setPreviewRadius(RADIUS)
                .setName("Аура левитации")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Создаёт вокруг владельца\n ауру на 6 секунд, все\n игроки в радиусе 10 блоков\n получают эффект левитации.\n Эффект не действует\n на владельца буста")
                .setRunnable(consumer -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_SHULKER_SHOOT,
                                SoundCategory.MASTER, 1,
                                1);
                    }

                    int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {
                        Location center = consumer.getLocation();

                        Game.targets().forEach((gamePlayer) -> {
                            if (gamePlayer != consumer && gamePlayer.getWorld().equals(center.getWorld())
                                    && gamePlayer.getLocation().distance(center) < RADIUS) {
                                gamePlayer.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 0));
                            }
                        });

                        // кольцо частиц по границе ауры
                        for (double a = 0; a < Math.PI * 2; a += Math.PI / 24) {
                            center.getWorld().spawnParticle(Particle.END_ROD,
                                    center.clone().add(Math.cos(a) * RADIUS, .2, Math.sin(a) * RADIUS),
                                    1, 0, .3, 0, .01);
                        }
                    }, 0, 5L);

                    Bukkit.getScheduler().runTaskLater(Bows.instance, () -> {
                        Bukkit.getScheduler().cancelTask(task);
                    }, DURATION_TICKS);
                })
                .setDelay(3)
                .build(player);
    }
}
