package com.v4nden.bows.Game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

/**
 * Статистика игрока (победы, убийства, смерти) в PersistentDataContainer игрока.
 */
public class Stats {
    public static final NamespacedKey WINS = new NamespacedKey("bows", "wins");
    public static final NamespacedKey KILLS = new NamespacedKey("bows", "kills");
    public static final NamespacedKey DEATHS = new NamespacedKey("bows", "deaths");

    // убийство засчитывается последнему игроку, нанёсшему урон за это время
    private static final long KILL_CREDIT_MS = 15_000;

    private static final Map<UUID, UUID> lastDamager = new HashMap<>();
    private static final Map<UUID, Long> lastDamageTime = new HashMap<>();

    public static int get(Player player, NamespacedKey key) {
        return player.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    public static void increment(Player player, NamespacedKey key) {
        player.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, get(player, key) + 1);
    }

    public static void recordDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = findAttacker(e);
        if (attacker != null && !attacker.equals(victim)) {
            lastDamager.put(victim.getUniqueId(), attacker.getUniqueId());
            lastDamageTime.put(victim.getUniqueId(), System.currentTimeMillis());
        }
    }

    // убийца — тот, кто нанёс смертельный урон, иначе последний недавно ранивший
    public static Player findKiller(Player victim, EntityDamageEvent lethal) {
        Player attacker = findAttacker(lethal);
        if (attacker != null && !attacker.equals(victim)) {
            return attacker;
        }
        UUID uuid = lastDamager.get(victim.getUniqueId());
        Long time = lastDamageTime.get(victim.getUniqueId());
        if (uuid == null || time == null || System.currentTimeMillis() - time > KILL_CREDIT_MS) {
            return null;
        }
        return Bukkit.getPlayer(uuid);
    }

    public static void forget(Player victim) {
        lastDamager.remove(victim.getUniqueId());
        lastDamageTime.remove(victim.getUniqueId());
    }

    /** Игрок, нанёсший урон: напрямую, стрелой, молнией или TNT с источником. */
    public static Player findAttacker(EntityDamageEvent e) {
        if (!(e instanceof EntityDamageByEntityEvent byEntity)) {
            return null;
        }
        Entity damager = byEntity.getDamager();
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        if (damager instanceof LightningStrike lightning) {
            return lightning.getCausingPlayer();
        }
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player source) {
            return source;
        }
        return null;
    }
}
