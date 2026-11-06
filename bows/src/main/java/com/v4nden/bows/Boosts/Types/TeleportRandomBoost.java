package com.v4nden.bows.Boosts.Types;

import java.util.List;
import java.util.Random;

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

public class TeleportRandomBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.ENDER_PEARL)
                .setRarity(2)
                .setId("teleportrandom")
                .setIcon("teleportrandom")
                .setName("Телепорт к рандомному игроку")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Спустя задержку телепортирует\n владельца к случайному\n игроку в текущей игре, звук\n задержки перед телепортацией\n слышан как владельцу, так и\n случайному игроку")
                .setRunnable(consumer -> {
                    List<LivingEntity> others = Game.targetsExcept(consumer);
                    if (others.isEmpty()) {
                        return;
                    }
                    LivingEntity randomplayer = others.get(new Random().nextInt(others.size()));

                    consumer.sendTitle("3...", "Телепортация через", 5, 10, 5);
                    consumer.playSound(consumer.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 0);
                    if (randomplayer instanceof Player target)
                        target.playSound(target.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 0);

                    Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                        consumer.sendTitle("2...", "Телепортация через", 5, 10, 5);
                        consumer.playSound(consumer.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 1);
                        if (randomplayer instanceof Player target)
                            target.playSound(target.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 1);
                    }, 20);
                    Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                        consumer.sendTitle("1...", "Телепортация через", 5, 10, 5);
                        consumer.playSound(consumer.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 2);
                        if (randomplayer instanceof Player target)
                            target.playSound(target.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 10, 2);
                    }, 40);
                    Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                        consumer.teleport(randomplayer.getLocation());
                        consumer.playSound(consumer.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 10, 0);
                    }, 60);
                })
                .build(player);
    }
}
