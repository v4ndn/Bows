package com.v4nden.bows.Game;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Husk;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

import com.v4nden.bows.BowsUtils;

/**
 * Куклы для тестовой игры: мобы без своего поведения, на которых действуют
 * бусты наравне с участниками (см. Game.targets()).
 *
 * Хаск, а не зомби — не горит на солнце. Неосведомлённый моб (setAware(false))
 * не ходит и не атакует, но падает, отлетает от ударов и получает эффекты.
 */
public class Dummies implements Listener {
    public static final List<LivingEntity> dummies = new ArrayList<>();
    private static int counter = 0;

    public static void spawn(Location location) {
        Husk dummy = location.getWorld().spawn(location, Husk.class, husk -> {
            husk.setAdult();
            husk.setAware(false);
            husk.setSilent(true);
            husk.setPersistent(false);
            husk.setRemoveWhenFarAway(false);
            husk.setCanPickupItems(false);
            husk.setCustomName("Кукла #" + (++counter));
            husk.setCustomNameVisible(true);

            EntityEquipment equipment = husk.getEquipment();
            equipment.clear();
            equipment.setHelmet(new ItemStack(Material.PLAYER_HEAD));
            equipment.setHelmetDropChance(0);
        });
        dummies.add(dummy);
    }

    public static List<LivingEntity> alive() {
        dummies.removeIf(dummy -> !dummy.isValid());
        return dummies;
    }

    public static void clear() {
        dummies.forEach(LivingEntity::remove);
        dummies.clear();
        counter = 0;
    }

    @EventHandler
    public void onDummyDeath(EntityDeathEvent e) {
        if (!dummies.remove(e.getEntity())) {
            return;
        }
        e.getDrops().clear();
        e.setDroppedExp(0);
        BowsUtils.broadcastSystemMessage(Bukkit.getOnlinePlayers(), e.getEntity().getCustomName() + " убита!");
        // в тестовой игре убийства кукол тоже объявляются — так их можно проверить
        Player killer = e.getEntity().getKiller();
        if (Game.isRunning && killer != null) {
            KillAnnouncer.onKill(killer, e.getEntity().getCustomName());
        }
    }
}
