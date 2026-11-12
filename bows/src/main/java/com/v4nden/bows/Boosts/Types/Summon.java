package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Game.Game;

// общий отсчёт и телепорт цели к владельцу для бустов «Призыв ...»
class Summon {
    static final int COUNTDOWN_SECONDS = 3;

    static void summon(Player owner, LivingEntity target) {
        long game = Game.gameStart;
        for (int i = 0; i < COUNTDOWN_SECONDS; i++) {
            int left = COUNTDOWN_SECONDS - i;
            float pitch = i;
            Bukkit.getScheduler().runTaskLater(Bows.instance, () -> {
                if (!isActual(owner, target, game)) {
                    return;
                }
                owner.sendTitle(left + "...", "Призыв через", 5, 10, 5);
                owner.playSound(owner.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, pitch);
                if (target instanceof Player victim) {
                    victim.sendTitle(left + "...", "Вас призовут через", 5, 10, 5);
                    victim.playSound(victim.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, pitch);
                }
            }, i * 20L);
        }
        Bukkit.getScheduler().runTaskLater(Bows.instance, () -> {
            if (!isActual(owner, target, game)) {
                return;
            }
            // цель смотрит туда же, куда смотрела
            Location destination = owner.getLocation();
            destination.setYaw(target.getLocation().getYaw());
            destination.setPitch(target.getLocation().getPitch());
            // с транспортом телепорт не сработает
            target.leaveVehicle();
            target.teleport(destination);
            target.setFallDistance(0);
            owner.playSound(owner.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 10, 0);
            if (target instanceof Player victim) {
                victim.playSound(victim.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 10, 0);
            }
        }, COUNTDOWN_SECONDS * 20L);
    }

    // та же игра, оба ещё в ней и в одном мире
    private static boolean isActual(Player owner, LivingEntity target, long game) {
        return Game.isRunning && Game.gameStart == game && owner.isOnline() && !owner.isDead()
                && Game.gamePlayers.contains(owner) && target.isValid() && !target.isDead()
                && Game.targetsExcept(owner).contains(target) && target.getWorld().equals(owner.getWorld());
    }
}
