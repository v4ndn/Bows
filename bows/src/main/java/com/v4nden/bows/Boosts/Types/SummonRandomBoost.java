package com.v4nden.bows.Boosts.Types;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;

import net.md_5.bungee.api.ChatColor;

public class SummonRandomBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.NAUTILUS_SHELL)
                .setRarity(2)
                .setId("summonrandom")
                .setName("Призыв рандомного игрока")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Спустя 3 секунды\n телепортирует случайного\n игрока к владельцу, отсчёт\n видят и слышат как владелец,\n так и призываемый игрок")
                .setRunnable(consumer -> {
                    List<LivingEntity> others = Game.selectableTargetsExcept(consumer);
                    if (others.isEmpty()) {
                        return;
                    }
                    Summon.summon(consumer, others.get(ThreadLocalRandom.current().nextInt(others.size())));
                })
                .build(player);
    }
}
