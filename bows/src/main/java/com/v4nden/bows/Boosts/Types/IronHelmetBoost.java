package com.v4nden.bows.Boosts.Types;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;

import net.md_5.bungee.api.ChatColor;

public class IronHelmetBoost implements IBoostType {
    public static final String ID = "ironhelmet";

    public Boost create(Player player) {
        return new BoostGenerator()
                // самородок, а не шлем: шлем надевался бы на ПКМ; выглядит как шлем
                .setMaterial(Material.IRON_NUGGET)
                .setIcon("minecraft:iron_helmet")
                .setRarity(5)
                .setId(ID)
                .setName("Железный шлем")
                .setDescription(ChatColor.of("#a6a6a6") + "Пассивный стартовый буст:" + ChatColor.RESET
                        + "\n На всю игру надевает\n на владельца железный\n шлем")
                .setPassive(true)
                .setRunnable(consumer -> {
                })
                .build(player);
    }

    public static void apply(Player player) {
        ItemStack helmet = new ItemStack(Material.IRON_HELMET);
        ItemMeta meta = helmet.getItemMeta();
        // несъёмный до конца игры; в конце игры инвентарь очищается вместе с бронёй
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        meta.setUnbreakable(true);
        helmet.setItemMeta(meta);
        player.getInventory().setHelmet(helmet);
    }
}
