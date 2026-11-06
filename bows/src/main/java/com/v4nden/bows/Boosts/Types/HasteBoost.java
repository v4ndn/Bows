package com.v4nden.bows.Boosts.Types;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;

import net.md_5.bungee.api.ChatColor;

public class HasteBoost implements IBoostType {
    public static final String ID = "haste";

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.GOLD_INGOT)
                .setRarity(5)
                .setId(ID)
                .setName("Спешка")
                .setDescription(ChatColor.of("#a6a6a6") + "Пассивный стартовый буст:" + ChatColor.RESET
                        + "\n На всю игру даёт\n владельцу эффект спешки\n третьего уровня")
                .setPassive(true)
                .setRunnable(consumer -> {
                })
                .build(player);
    }

    public static void apply(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, PotionEffect.INFINITE_DURATION, 2,
                false, false));
    }

    public static void reset(Player player) {
        player.removePotionEffect(PotionEffectType.HASTE);
    }
}
