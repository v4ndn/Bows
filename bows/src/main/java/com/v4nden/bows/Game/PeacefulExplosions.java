package com.v4nden.bows.Game;

import org.bukkit.Difficulty;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.util.Vector;

/**
 * Урон от TNT по игрокам на мирной сложности.
 *
 * На мирной игра обнуляет игрокам урон, зависящий от сложности, — взрывы такие.
 * Поэтому плагин считает урон взрыва TNT сам (ванильная формула: расстояние и
 * закрытость блоками) и наносит его типом, который мирная не обнуляет. Прямая
 * сущность урона — TNT, инициатор — поджёгший, поэтому фильтр игры урон
 * пропускает, а убийство засчитывается поджёгшему. На других сложностях не
 * работает — там урон наносит сама игра (и Game.halveExplosionDamage режет его).
 */
public class PeacefulExplosions implements Listener {
    private static final double DAMAGE_MULTIPLIER = .5;

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        if (!(e.getEntity() instanceof TNTPrimed tnt) || e.getLocation().getWorld().getDifficulty() != Difficulty.PEACEFUL) {
            return;
        }
        Location center = e.getLocation();
        double power = tnt.getYield();
        double reach = power * 2;

        for (Entity nearby : center.getWorld().getNearbyEntities(center, reach, reach, reach)) {
            if (!(nearby instanceof Player player) || !player.isValid() || player.isDead()) {
                continue;
            }
            double distance = player.getLocation().distance(center) / reach;
            if (distance > 1) {
                continue;
            }
            double impact = (1 - distance) * exposure(center, player);
            if (impact <= 0) {
                continue;
            }
            // ванильная формула урона взрыва
            double damage = Math.floor((impact * impact + impact) / 2 * 7 * reach + 1) * DAMAGE_MULTIPLIER;

            DamageSource.Builder source = DamageSource.builder(DamageType.GENERIC).withDirectEntity(tnt)
                    .withDamageLocation(center);
            if (tnt.getSource() != null) {
                source.withCausingEntity(tnt.getSource());
            }
            player.damage(damage, source.build());
        }
    }

    // доля лучей от центра взрыва до игрока (ноги, тело, глаза), не перекрытых блоками
    private static double exposure(Location center, Player player) {
        Location[] points = { player.getLocation(), player.getLocation().add(0, player.getHeight() / 2, 0),
                player.getEyeLocation() };
        int open = 0;
        for (Location point : points) {
            Vector direction = point.toVector().subtract(center.toVector());
            double length = direction.length();
            if (length < 1e-3 || center.getWorld().rayTraceBlocks(center, direction.normalize(), length,
                    FluidCollisionMode.NEVER, true) == null) {
                open++;
            }
        }
        return open / (double) points.length;
    }
}
