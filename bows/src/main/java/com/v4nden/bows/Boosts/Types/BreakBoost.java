package com.v4nden.bows.Boosts.Types;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;
import com.v4nden.bows.Utils.TemporaryListener;

import net.md_5.bungee.api.ChatColor;

public class BreakBoost implements IBoostType {
    private static final int DEBRIS_RADIUS = 5;
    private static final int LANDING_TIMEOUT_TICKS = 100;
    private static final long DEBRIS_LIFETIME_TICKS = 60L;

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.FIREWORK_STAR)
                .setRarity(4)
                .setId("break")
                .setPreviewRadius(7)
                .setIcon("break")
                .setName("Удар")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Подбрасывает владельца\n в воздух, затем с силой\n ударяет о поверхность,\n что создаёт ударную\n волну отталкивающую и \n наносящую урон игрокам\n в радиусе 7 блоков")
                .setRunnable(consumer -> {
                    consumer.setVelocity(consumer.getVelocity().add(new Vector(0, 1.15, 0)));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,
                                SoundCategory.MASTER, 1,
                                1);
                    }
                    Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                        consumer.setVelocity(consumer.getVelocity().add(new Vector(0, -2, 0)));

                        // ждём реального приземления, а не фиксированное число тиков
                        new BukkitRunnable() {
                            int ticks = 0;

                            @Override
                            public void run() {
                                ticks++;
                                if (!consumer.isOnline() || ticks > LANDING_TIMEOUT_TICKS) {
                                    cancel();
                                    return;
                                }
                                if (ticks > 1 && isLanded(consumer)) {
                                    cancel();
                                    impact(consumer);
                                }
                            }
                        }.runTaskTimer(Bows.instance, 1L, 1L);
                    }, 15L);
                })
                .build(player);
    }

    private static boolean isLanded(Player player) {
        return player.isOnGround()
                || player.getLocation().clone().subtract(0, 0.1, 0).getBlock().getType().isSolid();
    }

    private static void impact(Player consumer) {
        consumer.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE,
                consumer.getLocation(),
                50, 3,
                0, 3, .01);
        Game.targetsFor(consumer).forEach((gamePlayer) -> {
            if (!gamePlayer.equals(consumer)) {
                if (gamePlayer.getWorld().equals(consumer.getWorld())
                        && gamePlayer.getLocation().distance(consumer.getLocation()) < 7) {
                    gamePlayer.setVelocity(gamePlayer.getVelocity()
                            .add(gamePlayer.getLocation().subtract(consumer.getLocation()).toVector()
                                    .divide(new Vector(2, 2, 2))
                                    .setY(1.5)));
                }
            }
        });
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(consumer.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK,
                    SoundCategory.MASTER,
                    1,
                    1);
        }
        spawnDebris(consumer.getLocation());
    }

    // Подбрасывает копии верхних блоков вокруг точки удара; сами блоки остаются на месте
    private static void spawnDebris(Location center) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<FallingBlock> debris = new ArrayList<>();
        Set<UUID> debrisIds = new HashSet<>();

        for (int x = -DEBRIS_RADIUS; x <= DEBRIS_RADIUS; x++) {
            for (int z = -DEBRIS_RADIUS; z <= DEBRIS_RADIUS; z++) {
                double distance = Math.sqrt(x * x + z * z);
                if (distance < 1 || distance > DEBRIS_RADIUS || random.nextDouble() > 0.6) {
                    continue;
                }

                Block surface = findSurface(center.clone().add(x, 0, z));
                if (surface == null) {
                    continue;
                }

                FallingBlock falling = center.getWorld().spawnFallingBlock(
                        surface.getLocation().add(0.5, 1, 0.5), surface.getBlockData());
                falling.setDropItem(false);
                falling.setHurtEntities(false);

                // ближние блоки летят выше, дальние — дальше в стороны
                double strength = 1 - distance / (DEBRIS_RADIUS + 1);
                Vector outward = new Vector(x, 0, z).normalize()
                        .multiply(0.15 + random.nextDouble() * 0.25);
                outward.setY(0.35 + strength * 0.6 + random.nextDouble() * 0.2);
                falling.setVelocity(outward);

                debris.add(falling);
                debrisIds.add(falling.getUniqueId());
            }
        }

        if (debris.isEmpty()) {
            return;
        }

        TemporaryListener<EntityChangeBlockEvent> landListener = new TemporaryListener<EntityChangeBlockEvent>(
                EntityChangeBlockEvent.class, EventPriority.NORMAL, e -> {
                    if (debrisIds.contains(e.getEntity().getUniqueId())) {
                        e.setCancelled(true);
                        e.getEntity().remove();
                    }
                });

        Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
            debris.forEach(FallingBlock::remove);
            landListener.unregister();
        }, DEBRIS_LIFETIME_TICKS);
    }

    // Ищет верхний твёрдый блок под/над точкой, над которым есть воздух
    private static Block findSurface(Location loc) {
        Block block = loc.getBlock();
        for (int dy = 1; dy >= -3; dy--) {
            Block candidate = block.getRelative(0, dy, 0);
            if (candidate.getType().isSolid() && candidate.getRelative(0, 1, 0).isPassable()) {
                return candidate;
            }
        }
        return null;
    }
}
