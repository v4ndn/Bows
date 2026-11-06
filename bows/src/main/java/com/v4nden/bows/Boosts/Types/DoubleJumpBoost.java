package com.v4nden.bows.Boosts.Types;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Boosts.StarterBoosts;
import com.v4nden.bows.Game.Game;

import net.md_5.bungee.api.ChatColor;

/**
 * Двойной прыжок: владельцу разрешён полёт, и второй прыжок в воздухе (который
 * включил бы полёт) превращается в рывок вверх. Перезаряжается при приземлении.
 * Урон от падения у владельца всегда есть — от высшей точки в воздухе.
 */
public class DoubleJumpBoost implements IBoostType {
    public static final String ID = "doublejump";
    private static final double JUMP_UP = .9;
    private static final double JUMP_FORWARD = .5;

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.SLIME_BALL)
                .setRarity(5)
                .setId(ID)
                .setName("Двойной прыжок")
                .setDescription(ChatColor.of("#a6a6a6") + "Пассивный стартовый буст:" + ChatColor.RESET
                        + "\n На всю игру даёт\n владельцу двойной прыжок:\n нажми прыжок ещё раз\n в воздухе. Урон от падения\n считается от высшей точки\n и проходит даже при\n приземлении в воду")
                .setPassive(true)
                .setRunnable(consumer -> {
                })
                .build(player);
    }

    public static void apply(Player player) {
        player.setAllowFlight(true);
    }

    private static boolean canDoubleJump(Player player) {
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            return false;
        }
        if (!Game.isRunning || !Game.gamePlayers.contains(player)) {
            return false;
        }
        Boost starter = StarterBoosts.current(player);
        return starter != null && starter.getId().equals(ID);
    }

    public static class Handler implements Listener {
        @EventHandler(ignoreCancelled = true)
        public void onToggleFlight(PlayerToggleFlightEvent e) {
            Player player = e.getPlayer();
            if (!e.isFlying() || !canDoubleJump(player)) {
                return;
            }
            e.setCancelled(true);
            player.setFlying(false);
            // до приземления второго прыжка нет
            player.setAllowFlight(false);

            Vector forward = player.getLocation().getDirection().setY(0);
            if (forward.lengthSquared() > 1e-4) {
                forward.normalize().multiply(JUMP_FORWARD);
            }
            player.setVelocity(forward.setY(JUMP_UP));

            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 15, .3, .05, .3, .02);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_JUMP, SoundCategory.MASTER, 1, 1.2f);
        }

        // урон от падения считаем сами: игре нельзя бить падением того, кому
        // разрешён полёт, а он разрешён, чтобы ловить второй прыжок
        private final Map<UUID, Double> peakY = new HashMap<>();
        private final Set<UUID> applyingFall = new HashSet<>();

        @EventHandler(ignoreCancelled = true)
        public void onMove(PlayerMoveEvent e) {
            Player player = e.getPlayer();
            if (!canDoubleJump(player)) {
                peakY.remove(player.getUniqueId());
                return;
            }
            double y = e.getTo().getY();
            if (player.isOnGround()) {
                Double peak = peakY.remove(player.getUniqueId());
                if (peak != null) {
                    applyFallDamage(player, peak - y);
                }
                if (!player.getAllowFlight()) {
                    player.setAllowFlight(true);
                }
            } else if (player.isInWater() || player.isClimbing() || player.isGliding()) {
                // лестницы и элитры гасят падение; вода — только если игрок в ней уже
                // плывёт: приземление в воду всё равно бьёт (так задумано, есть в описании)
                peakY.put(player.getUniqueId(), y);
            } else {
                peakY.merge(player.getUniqueId(), y, Math::max);
            }
        }

        private void applyFallDamage(Player player, double height) {
            double safe = player.getAttribute(Attribute.SAFE_FALL_DISTANCE).getValue();
            double multiplier = player.getAttribute(Attribute.FALL_DAMAGE_MULTIPLIER).getValue();
            double damage = Math.ceil((height - safe) * multiplier);
            if (damage <= 0) {
                return;
            }
            applyingFall.add(player.getUniqueId());
            try {
                player.damage(damage, DamageSource.builder(DamageType.FALL).build());
            } finally {
                applyingFall.remove(player.getUniqueId());
            }
        }

        // ванильный урон от падения (когда полёт уже выключен после второго прыжка)
        // не должен прийти вдобавок к нашему
        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onFall(EntityDamageEvent e) {
            if (e.getCause() == DamageCause.FALL && e.getEntity() instanceof Player player
                    && !applyingFall.contains(player.getUniqueId()) && canDoubleJump(player)) {
                e.setCancelled(true);
            }
        }
    }
}
