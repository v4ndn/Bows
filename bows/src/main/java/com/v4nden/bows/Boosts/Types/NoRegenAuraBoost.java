package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;
import com.v4nden.bows.Utils.TemporaryListener;

import net.md_5.bungee.api.ChatColor;

public class NoRegenAuraBoost implements IBoostType {
    private static final double RADIUS = 15;
    private static final long DURATION_TICKS = 20 * 10L;
    private static final Particle.DustOptions ASH = new Particle.DustOptions(Color.fromRGB(60, 50, 50), 1.5f);

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.COAL)
                .setRarity(2)
                .setId("noregenaura")
                .setPreviewRadius(RADIUS)
                .setName("Аура без регенерации")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Создаёт вокруг владельца\n ауру на 10 секунд, у всех\n игроков в радиусе 15 блоков\n отключается естественная\n регенерация здоровья.\n Эффект не действует\n на владельца буста")
                .setRunnable(consumer -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_WITHER_AMBIENT,
                                SoundCategory.MASTER, .6f,
                                1.5f);
                    }

                    // только естественная регенерация от сытости; зелья и вампиризм
                    // (он меняет здоровье напрямую, без события) продолжают работать
                    TemporaryListener<EntityRegainHealthEvent> regenListener = new TemporaryListener<EntityRegainHealthEvent>(
                            EntityRegainHealthEvent.class, EventPriority.NORMAL, e -> {
                                if (e.getRegainReason() != RegainReason.SATIATED
                                        && e.getRegainReason() != RegainReason.REGEN) {
                                    return;
                                }
                                if (e.getEntity().equals(consumer) || !Game.targets().contains(e.getEntity())) {
                                    return;
                                }
                                Location center = consumer.getLocation();
                                if (e.getEntity().getWorld().equals(center.getWorld())
                                        && e.getEntity().getLocation().distance(center) < RADIUS) {
                                    e.setCancelled(true);
                                }
                            });

                    int particleTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {
                        Location center = consumer.getLocation();
                        for (double a = 0; a < Math.PI * 2; a += Math.PI / 32) {
                            center.getWorld().spawnParticle(Particle.DUST,
                                    center.clone().add(Math.cos(a) * RADIUS, .2, Math.sin(a) * RADIUS),
                                    1, 0, .2, 0, ASH);
                        }
                    }, 0L, 5L);

                    Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                        Bukkit.getScheduler().cancelTask(particleTask);
                        regenListener.unregister();
                    }, DURATION_TICKS);
                })
                .setDelay(3)
                .build(player);
    }
}
