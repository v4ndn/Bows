package com.v4nden.bows.Boosts.Types;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;

import net.md_5.bungee.api.ChatColor;

public class QuiverBoost implements IBoostType {
    public static final String ID = "quiver";
    public static final int MAX_ARROWS = 30;

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.LEATHER)
                .setRarity(5)
                .setId(ID)
                .setName("Колчан")
                .setDescription(ChatColor.of("#a6a6a6") + "Пассивный стартовый буст:" + ChatColor.RESET
                        + "\n На всю игру владелец\n носит до 30 стрел\n вместо 20")
                .setPassive(true)
                .setRunnable(consumer -> {
                })
                .build(player);
    }
}
