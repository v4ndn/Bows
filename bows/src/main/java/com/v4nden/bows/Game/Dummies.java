package com.v4nden.bows.Game;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import com.v4nden.bows.BowsUtils;
import com.v4nden.bows.Boosts.Types.GiantBoost;

import net.kyori.adventure.text.Component;

/**
 * Куклы для тестовой игры: мобы без своего поведения, на которых действуют
 * бусты наравне с участниками (см. Game.targets()).
 *
 * Манекен (1.21.9+): выглядит как игрок, не враждебный — на мирной сложности
 * не исчезает (хаск исчезал), сам не ходит и не атакует, но падает, отлетает
 * от ударов и получает эффекты.
 */
public class Dummies implements Listener {
    public static final List<LivingEntity> dummies = new ArrayList<>();
    private static int counter = 0;

    public static void spawn(Location location) {
        Mannequin dummy = location.getWorld().spawn(location, Mannequin.class, mannequin -> {
            mannequin.setImmovable(false); // отлетает от взрывов, ветра и т.д.
            mannequin.setSilent(true);
            mannequin.setPersistent(false);
            mannequin.setCustomName("Кукла #" + (++counter));
            mannequin.setCustomNameVisible(true);
            mannequin.setDescription(Component.empty());
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
            GiantBoost.onKill(killer);
        }
    }
}
