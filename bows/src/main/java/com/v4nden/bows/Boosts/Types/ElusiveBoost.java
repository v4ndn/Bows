package com.v4nden.bows.Boosts.Types;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Boosts.StarterBoosts;

import net.md_5.bungee.api.ChatColor;

/**
 * Неуловимый: чужие бусты, выбирающие случайную или ближайшую цель, владельца
 * не выбирают (Game.selectableTargetsExcept). На бусты по площади не влияет.
 */
public class ElusiveBoost implements IBoostType {
    public static final String ID = "elusive";

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.TURTLE_SCUTE)
                .setRarity(Boost.STARTER_RARITY)
                .setId(ID)
                .setName("Неуловимый")
                .setDescription(ChatColor.of("#a6a6a6") + "Пассивный стартовый буст:" + ChatColor.RESET
                        + "\n На всю игру: бусты других\n игроков, выбирающие\n случайную или ближайшую\n цель, никогда не выбирают\n владельца")
                .setPassive(true)
                .setRunnable(consumer -> {
                })
                .build(player);
    }

    public static boolean has(Player player) {
        Boost starter = StarterBoosts.current(player);
        return starter != null && starter.getId().equals(ID);
    }
}
