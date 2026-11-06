package com.v4nden.bows.Boosts.Types;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;

import net.md_5.bungee.api.ChatColor;

/**
 * Кольчуга: на всю игру кольчужные шлем и нагрудник, несъёмные и неломаемые.
 * Снимается вместе с инвентарём в конце игры и при выбывании. «Полёт» на время
 * заменяет нагрудник элитрами и потом возвращает его.
 */
public class ChainmailBoost implements IBoostType {
    public static final String ID = "chainmail";

    public Boost create(Player player) {
        return new BoostGenerator()
                // самородок, а не броня: броня надевалась бы на ПКМ; выглядит как кольчуга
                .setMaterial(Material.IRON_NUGGET)
                .setIcon("minecraft:chainmail_chestplate")
                .setRarity(Boost.STARTER_RARITY)
                .setId(ID)
                .setName("Кольчуга")
                .setDescription(ChatColor.of("#a6a6a6") + "Пассивный стартовый буст:" + ChatColor.RESET
                        + "\n На всю игру надевает\n на владельца кольчужные\n шлем и нагрудник")
                .setPassive(true)
                .setRunnable(consumer -> {
                })
                .build(player);
    }

    public static void apply(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.setHelmet(piece(Material.CHAINMAIL_HELMET));
        inventory.setChestplate(piece(Material.CHAINMAIL_CHESTPLATE));
    }

    private static ItemStack piece(Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }
}
