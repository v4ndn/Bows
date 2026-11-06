package com.v4nden.bows.Boosts.Types;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.PlayerSize;

import net.md_5.bungee.api.ChatColor;

public class SmallBoost implements IBoostType {
    public static final String ID = "small";
    private static final double SCALE = .85;

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.RABBIT_HIDE)
                .setRarity(Boost.STARTER_RARITY)
                .setId(ID)
                .setName("Малыш")
                .setDescription(ChatColor.of("#a6a6a6") + "Пассивный стартовый буст:" + ChatColor.RESET
                        + "\n На всю игру уменьшает\n владельца до 85% от\n обычного размера")
                .setPassive(true)
                .setRunnable(consumer -> {
                })
                .build(player);
    }

    public static void apply(Player player) {
        PlayerSize.setBase(player, SCALE);
    }
}
