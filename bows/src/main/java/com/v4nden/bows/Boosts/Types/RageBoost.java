package com.v4nden.bows.Boosts.Types;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;
import com.v4nden.bows.Utils.TemporaryListener;

import net.md_5.bungee.api.ChatColor;

public class RageBoost implements IBoostType {
    // в 4/3 раза сильнее рывка (DashBoost)
    private static final double PUSH = 4 / 1.5;
    private static final int BREAK_TICKS = 10; // 0.5 секунды
    // насколько вперёд по направлению рывка расчищается путь, в блоках
    private static final double LOOKAHEAD = 3;
    // на сколько блоков вокруг хитбокса ломается коридор
    private static final double BREAK_RADIUS = 1;
    private static final double SHOVE_RADIUS = 2.5;
    private static final double SHOVE_SIDE = 1.5;
    private static final double SHOVE_UP = .5;
    // разлёт обломков: доля сломанных блоков, предел за тик и время жизни
    private static final double DEBRIS_CHANCE = .15;
    private static final int DEBRIS_PER_TICK = 12;
    private static final long DEBRIS_LIFETIME_TICKS = 60L;
    // защита от падения после окончания буста
    private static final long NO_FALL_AFTER_TICKS = 40L;

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.NETHERITE_INGOT)
                .setRarity(4)
                .setId("rage")
                .setPreviewRadius(SHOVE_RADIUS)
                .setName("Рейдж")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n 0.5 секунды несёт владельца\n туда, куда он смотрит,\n ломая все блоки на пути\n и расталкивая игроков\n в стороны")
                .setRunnable(consumer -> {

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_RAVAGER_ROAR, SoundCategory.MASTER, 1, 1);
                    }

                    // пока летишь и ещё 2 секунды после — без урона от падения
                    TemporaryListener<EntityDamageEvent> fallListener = new TemporaryListener<EntityDamageEvent>(
                            EntityDamageEvent.class, EventPriority.LOWEST, e -> {
                                if (e.getEntity().equals(consumer) && e.getCause() == DamageCause.FALL) {
                                    e.setCancelled(true);
                                }
                            });
                    Bukkit.getScheduler().runTaskLater(Bows.instance, () -> {
                        fallListener.unregister();
                        // накопленная за полёт высота не должна ударить сразу после защиты
                        consumer.setFallDistance(0);
                    }, BREAK_TICKS + NO_FALL_AFTER_TICKS);

                    Set<UUID> shoved = new HashSet<>();
                    Set<UUID> debris = new HashSet<>();
                    // обломки падают, но не ставятся блоками
                    TemporaryListener<EntityChangeBlockEvent> debrisListener = new TemporaryListener<EntityChangeBlockEvent>(
                            EntityChangeBlockEvent.class, EventPriority.NORMAL, e -> {
                                if (debris.contains(e.getEntity().getUniqueId())) {
                                    e.setCancelled(true);
                                    e.getEntity().remove();
                                }
                            });
                    new BukkitRunnable() {
                        int ticks = 0;

                        @Override
                        public void run() {
                            if (++ticks > BREAK_TICKS || !consumer.isOnline()) {
                                cancel();
                                Bukkit.getScheduler().runTaskLater(Bows.instance, () -> {
                                    debris.forEach(uuid -> {
                                        Entity entity = Bukkit.getEntity(uuid);
                                        if (entity != null) {
                                            entity.remove();
                                        }
                                    });
                                    debrisListener.unregister();
                                }, DEBRIS_LIFETIME_TICKS);
                                return;
                            }
                            // не инерция: каждый тик ровно туда, куда смотрит игрок
                            Vector direction = consumer.getLocation().getDirection().normalize();
                            consumer.setVelocity(direction.clone().multiply(PUSH));
                            clearPath(consumer, direction, debris);
                            shoveAside(consumer, direction, shoved);
                        }
                    }.runTaskTimer(Bows.instance, 0L, 1L);
                })
                .build(player);
    }

    // копия блока вылетает в сторону от линии полёта и вверх
    private static void launchDebris(Block block, Player player, Vector direction, Set<UUID> debris) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vector relative = block.getLocation().add(.5, .5, .5).toVector().subtract(player.getLocation().toVector());
        Vector side = relative.subtract(direction.clone().multiply(relative.dot(direction))).setY(0);
        if (side.lengthSquared() < 1e-4) {
            side = new Vector(random.nextDouble(-1, 1), 0, random.nextDouble(-1, 1));
        }
        FallingBlock falling = block.getWorld().spawnFallingBlock(block.getLocation().add(.5, 0, .5),
                block.getBlockData());
        falling.setDropItem(false);
        falling.setHurtEntities(false);
        falling.setVelocity(side.normalize().multiply(.3 + random.nextDouble() * .4)
                .setY(.4 + random.nextDouble() * .4));
        debris.add(falling.getUniqueId());
    }

    // отталкивает каждую цель рядом в сторону от линии полёта, по разу за рывок
    private static void shoveAside(Player player, Vector direction, Set<UUID> shoved) {
        for (LivingEntity target : Game.targetsExcept(player)) {
            if (shoved.contains(target.getUniqueId()) || !target.getWorld().equals(player.getWorld())
                    || target.getLocation().distance(player.getLocation()) > SHOVE_RADIUS) {
                continue;
            }
            Vector relative = target.getLocation().toVector().subtract(player.getLocation().toVector());
            // убираем составляющую вдоль полёта — остаётся направление «в сторону»
            Vector side = relative.subtract(direction.clone().multiply(relative.dot(direction))).setY(0);
            if (side.lengthSquared() < 1e-4) {
                // цель ровно на линии полёта: толкаем вбок от направления
                side = new Vector(-direction.getZ(), 0, direction.getX());
            }
            target.setVelocity(side.normalize().multiply(SHOVE_SIDE).setY(SHOVE_UP));
            shoved.add(target.getUniqueId());
        }
    }

    // ломает блоки, которые пересекает хитбокс игрока, сдвинутый вперёд по рывку
    private static void clearPath(Player player, Vector direction, Set<UUID> debris) {
        World world = player.getWorld();
        BoundingBox box = player.getBoundingBox();
        boolean broke = false;
        int debrisThisTick = 0;
        for (double d = 0; d <= LOOKAHEAD; d += .5) {
            BoundingBox ahead = box.clone().shift(direction.clone().multiply(d)).expand(BREAK_RADIUS);
            for (int x = (int) Math.floor(ahead.getMinX()); x <= (int) Math.floor(ahead.getMaxX()); x++) {
                for (int y = (int) Math.floor(ahead.getMinY()); y <= (int) Math.floor(ahead.getMaxY()); y++) {
                    for (int z = (int) Math.floor(ahead.getMinZ()); z <= (int) Math.floor(ahead.getMaxZ()); z++) {
                        Block block = world.getBlockAt(x, y, z);
                        // бедрок, барьеры и т.п. (твёрдость < 0) не ломаются
                        if (block.getType().isAir() || block.getType().getHardness() < 0) {
                            continue;
                        }
                        world.spawnParticle(Particle.BLOCK, block.getLocation().add(.5, .5, .5), 8,
                                .3, .3, .3, block.getBlockData());
                        if (debrisThisTick < DEBRIS_PER_TICK && ThreadLocalRandom.current().nextDouble() < DEBRIS_CHANCE) {
                            launchDebris(block, player, direction, debris);
                            debrisThisTick++;
                        }
                        block.setType(Material.AIR);
                        broke = true;
                    }
                }
            }
        }
        if (broke) {
            world.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, .4f, 1.4f);
        }
    }
}
