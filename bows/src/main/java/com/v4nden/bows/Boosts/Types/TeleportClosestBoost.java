package com.v4nden.bows.Boosts.Types;

import java.util.Comparator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;

import net.md_5.bungee.api.ChatColor;

public class TeleportClosestBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.ENDER_PEARL)
                .setRarity(3)
                .setId("teleportclosest")
                .setIcon("teleportclosest")
                .setName("Телепорт к ближайшему игроку")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Спустя задержку телепортирует\n владельца к ближайшему\n игроку в текущей игре, звук\n задержки перед телепортацией\n слышан как владельцу, так и\n ближайшему игроку")
                .setRunnable(consumer -> {
                    LivingEntity closest = Game.targetsExcept(consumer).stream()
                            .min(Comparator.comparingDouble(
                                    (gamePlayer) -> gamePlayer.getLocation().distance(consumer.getLocation())))
                            .orElse(null);
                    if (closest == null) {
                        return;
                    }

                    consumer.sendTitle("3...", "Телепортация через", 5, 10, 5);
                    consumer.playSound(consumer.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 0);
                    if (closest instanceof Player target)
                        target.playSound(target.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 0);

                    Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                        consumer.sendTitle("2...", "Телепортация через", 5, 10, 5);
                        consumer.playSound(consumer.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 1);
                        if (closest instanceof Player target)
                            target.playSound(target.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 1);
                    }, 20);
                    Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                        consumer.sendTitle("1...", "Телепортация через", 5, 10, 5);
                        consumer.playSound(consumer.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 2);
                        if (closest instanceof Player target)
                            target.playSound(target.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 2);
                    }, 40);
                    Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                        consumer.teleport(closest.getLocation());
                        consumer.playSound(consumer.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 10, 0);
                    }, 60);
                })
                .build(player);
    }
}
