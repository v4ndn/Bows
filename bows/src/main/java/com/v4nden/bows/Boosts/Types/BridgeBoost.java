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

public class BridgeBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.LIME_DYE)
                .setRarity(2)
                .setId("bridge")
                .setName("Мостик")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Выпускает заряд в сторону\n направления головы игрока, \n по траектории пути заряда\n в воздухе оставляет платформы\n 3x3 из блоков, которые\n пропадают спустя 10 секунд")
                .setRunnable(consumer -> {
                    Snowball charge = BowsUtils.lauchCharge(consumer, Material.LIME_DYE);

                    int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {
                        Location loc = charge.getLocation().clone().add(0, -3, 0);
                        for (int x = -1; x <= 1; x++) {
                            for (int z = -1; z <= 1; z++) {
                                loc.clone().add(x, 0, z).getBlock().setType(Material.LIME_CONCRETE);
                            }
                        }

                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.MASTER,
                                    3,
                                    1);
                        }
                        Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                            for (int x = -1; x <= 1; x++) {
                                for (int z = -1; z <= 1; z++) {
                                    loc.clone().add(x, 0, z).getBlock().setType(Material.AIR);
                                }
                            }
                            for (Player p : Bukkit.getOnlinePlayers()) {
                                p.playSound(loc, Sound.BLOCK_NOTE_BLOCK_BIT, SoundCategory.MASTER,
                                        3,
                                        1);
                            }
                        }, 200L);
                    }, 0, 1L);

                    new TemporaryListener<>(ProjectileHitEvent.class,
                            EventPriority.NORMAL, e -> {
                                ProjectileHitEvent event = (ProjectileHitEvent) e;
                                if (event.getEntity().equals(charge)) {
                                    Bukkit.getScheduler().cancelTask(task);
                                    return true;
                                }
                                return false;
                            });

                }

                )
                .build(player);
    }
}
