package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;
import com.v4nden.bows.Game.Teams;
import com.v4nden.bows.Utils.TemporaryListener;

import net.md_5.bungee.api.ChatColor;

public class ImpactBoost implements IBoostType {
    // у «Удара» (BreakBoost): подъём 1.15, удар вниз через 15 тиков, радиус 7
    // высота растёт как квадрат скорости: ×√2 к скорости — вдвое выше (≈26 блоков)
    private static final double LAUNCH_UP = 2.26;
    private static final long SLAM_DELAY_TICKS = 42L;
    private static final double SLAM_DOWN = 2.5;
    private static final int LANDING_TIMEOUT_TICKS = 160;
    private static final double PUSH_RADIUS = 9;
    // горизонтальная сила: от PUSH_SIDE на краю до PUSH_SIDE + PUSH_SIDE_NEAR вплотную
    private static final double PUSH_SIDE = 4.4;
    private static final double PUSH_SIDE_NEAR = 2.4;
    private static final double PUSH_UP = 3.2;
    private static final float EXPLOSION_POWER = 3;
    // защита от падения после приземления
    private static final long NO_FALL_AFTER_TICKS = 40L;
    // сколько живут слушатели взрыва после его создания
    private static final long EXPLOSION_LISTENERS_TICKS = 10L;

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.GUNPOWDER)
                .setRarity(5)
                .setId("impact")
                .setPreviewRadius(PUSH_RADIUS)
                .setName("Импакт")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Подбрасывает владельца\n высоко в воздух, затем\n обрушивает его вниз, где\n под ним происходит взрыв,\n с силой отталкивающий\n игроков в радиусе 9 блоков.\n Владелец не получает урона\n от падения и своего взрыва")
                .setRunnable(consumer -> {
                    long game = Game.gameStart;
                    consumer.setVelocity(consumer.getVelocity().setY(LAUNCH_UP));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,
                                SoundCategory.MASTER, 1, .7f);
                    }

                    TemporaryListener<EntityDamageEvent> fallListener = new TemporaryListener<EntityDamageEvent>(
                            EntityDamageEvent.class, EventPriority.LOWEST, e -> {
                                if (e.getEntity().equals(consumer) && e.getCause() == DamageCause.FALL) {
                                    e.setCancelled(true);
                                }
                            });
                    Runnable endFallProtection = () -> Bukkit.getScheduler().runTaskLater(Bows.instance, () -> {
                        fallListener.unregister();
                        consumer.setFallDistance(0);
                    }, NO_FALL_AFTER_TICKS);

                    Bukkit.getScheduler().runTaskLater(Bows.instance, () -> {
                        if (!isActual(consumer, game)) {
                            endFallProtection.run();
                            return;
                        }
                        consumer.setVelocity(consumer.getVelocity().setY(-SLAM_DOWN));

                        new BukkitRunnable() {
                            int ticks = 0;

                            @Override
                            public void run() {
                                ticks++;
                                if (!isActual(consumer, game) || ticks > LANDING_TIMEOUT_TICKS) {
                                    cancel();
                                    endFallProtection.run();
                                    return;
                                }
                                if (ticks > 1 && BreakBoost.isLanded(consumer)) {
                                    cancel();
                                    consumer.setFallDistance(0);
                                    impact(consumer);
                                    endFallProtection.run();
                                }
                            }
                        }.runTaskTimer(Bows.instance, 1L, 1L);
                    }, SLAM_DELAY_TICKS);
                })
                .build(player);
    }

    private static boolean isActual(Player owner, long game) {
        return Game.isRunning && Game.gameStart == game && owner.isOnline() && !owner.isDead()
                && Game.gamePlayers.contains(owner);
    }

    private static void impact(Player consumer) {
        explode(consumer);

        consumer.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, consumer.getLocation(),
                70, 4, 0, 4, .01);
        for (LivingEntity target : Game.targetsExcept(consumer)) {
            if (!target.getWorld().equals(consumer.getWorld())) {
                continue;
            }
            Vector relative = target.getLocation().toVector().subtract(consumer.getLocation().toVector());
            double distance = relative.length();
            if (distance > PUSH_RADIUS) {
                continue;
            }
            Vector side = relative.setY(0);
            if (side.lengthSquared() < 1e-4) {
                side = consumer.getLocation().getDirection().setY(0);
            }
            double strength = PUSH_SIDE + (1 - distance / PUSH_RADIUS) * PUSH_SIDE_NEAR;
            target.setVelocity(side.normalize().multiply(strength).setY(PUSH_UP));
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(consumer.getLocation(), Sound.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.MASTER, 1, .8f);
        }
        BreakBoost.spawnDebris(consumer.getLocation());
    }

    // взрыв TNT под ногами: урон по игрокам наносит PeacefulExplosions, убийство — на владельце.
    // Блоки не ломаются, владельца и сокомандников не ранит и не отбрасывает
    private static void explode(Player consumer) {
        TNTPrimed tnt = consumer.getWorld().spawn(consumer.getLocation(), TNTPrimed.class, primed -> {
            primed.setPersistent(false);
            primed.setSource(consumer);
            primed.setYield(EXPLOSION_POWER);
            primed.setFuseTicks(0);
        });

        TemporaryListener<EntityExplodeEvent> blocksListener = new TemporaryListener<EntityExplodeEvent>(
                EntityExplodeEvent.class, EventPriority.LOWEST, e -> {
                    if (e.getEntity().equals(tnt)) {
                        e.blockList().clear();
                    }
                });
        TemporaryListener<EntityDamageEvent> damageListener = new TemporaryListener<EntityDamageEvent>(
                EntityDamageEvent.class, EventPriority.LOWEST, e -> {
                    if (tnt.equals(e.getDamageSource().getDirectEntity()) && isProtected(consumer, e.getEntity())) {
                        e.setCancelled(true);
                    }
                });
        TemporaryListener<EntityKnockbackEvent> knockbackListener = new TemporaryListener<EntityKnockbackEvent>(
                EntityKnockbackEvent.class, EventPriority.LOWEST, e -> {
                    if (e.getCause() == EntityKnockbackEvent.KnockbackCause.EXPLOSION
                            && isProtected(consumer, e.getEntity())) {
                        e.setCancelled(true);
                    }
                });
        Bukkit.getScheduler().runTaskLater(Bows.instance, () -> {
            blocksListener.unregister();
            damageListener.unregister();
            knockbackListener.unregister();
            tnt.remove();
        }, EXPLOSION_LISTENERS_TICKS);
    }

    private static boolean isProtected(Player owner, Entity entity) {
        return entity.equals(owner) || Teams.areTeammates(owner, entity);
    }
}
