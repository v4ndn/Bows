package com.v4nden.bows.Boosts.Types;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;

import com.v4nden.bows.BowsUtils;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;

import net.md_5.bungee.api.ChatColor;

public class RiderBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.BOWL)
                .setRarity(2)
                .setId("rider")
                .setName("Райдер")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Выпускает заряд в сторону\n направления головы игрока, \n игрок становится пассажиром\n заряда и двигается по\n траектории его полёта\n в воздухе")
                .setRunnable(consumer -> {
                    Snowball charge = BowsUtils.lauchCharge(consumer, Material.BOWL);
                    charge.addPassenger(consumer);
                })
                .setDelay(.5f)
                .build(player);
    }
}
