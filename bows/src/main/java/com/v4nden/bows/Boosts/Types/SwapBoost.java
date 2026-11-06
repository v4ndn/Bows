package com.v4nden.bows.Boosts.Types;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Boosts.StarterBoosts;
import com.v4nden.bows.Game.Game;

import net.md_5.bungee.api.ChatColor;

public class SwapBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.EMERALD)
                .setRarity(5)
                .setId("swap")
                .setName("Обмен")
                .setDescription(ChatColor.of("#a6a6a6") + "Стартовый буст:" + ChatColor.RESET
                        + "\n Меняет все бусты владельца\n на все бусты случайного\n живого игрока")
                .setRunnable(consumer -> {
                    List<Player> others = new ArrayList<>(Game.gamePlayers);
                    others.remove(consumer);
                    if (others.isEmpty()) {
                        return;
                    }
                    Player other = others.get(new Random().nextInt(others.size()));

                    // бусты пересоздаются: у буста есть владелец, чужой активировать нельзя
                    List<String> mine = StarterBoosts.takeRegularBoosts(consumer);
                    List<String> theirs = StarterBoosts.takeRegularBoosts(other);
                    theirs.forEach(id -> consumer.getInventory().addItem(StarterBoosts.createRegularBoost(id, consumer)));
                    mine.forEach(id -> other.getInventory().addItem(StarterBoosts.createRegularBoost(id, other)));

                    for (Player p : List.of(consumer, other)) {
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_TRADE, SoundCategory.MASTER, 1, 1);
                    }
                })
                .build(player);
    }
}
