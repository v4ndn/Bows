package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;

import net.md_5.bungee.api.ChatColor;

public class SmiteBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.HEART_OF_THE_SEA)
                .setRarity(5)
                .setId("smite")
                .setName("Кара небес")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Бьёт молнией всех\n живых игроков на карте,\n кроме владельца буста")
                .setRunnable(consumer -> {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.ITEM_TRIDENT_THUNDER,
                                SoundCategory.MASTER, 1,
                                1);
                    }
                    // targets() — копия: молния может убить игрока и он удалится из gamePlayers
                    Game.targets().forEach(gamePlayer -> {
                        if (!gamePlayer.equals(consumer)) {
                            gamePlayer.getWorld().strikeLightning(gamePlayer.getLocation()).setCausingPlayer(consumer);
                        }
                    });
                })
                .setDelay(3)
                .build(player);
    }
}
