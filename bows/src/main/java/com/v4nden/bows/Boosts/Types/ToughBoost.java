package com.v4nden.bows.Boosts.Types;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;

import net.md_5.bungee.api.ChatColor;

public class ToughBoost implements IBoostType {
    public static final String ID = "tough";
    private static final NamespacedKey MODIFIER_KEY = new NamespacedKey("bows", "starter_tough");
    private static final double EXTRA_HEALTH = 6; // 3 сердца

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.GLISTERING_MELON_SLICE)
                .setRarity(5)
                .setId(ID)
                .setName("Здоровяк")
                .setDescription(ChatColor.of("#a6a6a6") + "Пассивный стартовый буст:" + ChatColor.RESET
                        + "\n На всю игру добавляет\n владельцу 3 сердца\n максимального здоровья")
                .setPassive(true)
                .setRunnable(consumer -> {
                })
                .build(player);
    }

    public static void apply(Player player) {
        reset(player);
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        maxHealth.addModifier(new AttributeModifier(MODIFIER_KEY, EXTRA_HEALTH,
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        player.setHealth(maxHealth.getValue());
    }

    // модификатор сохраняется в данных игрока, поэтому снимаем его в конце игры
    public static void reset(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        for (AttributeModifier modifier : new ArrayList<>(maxHealth.getModifiers())) {
            if (modifier.getKey().equals(MODIFIER_KEY)) {
                maxHealth.removeModifier(modifier);
            }
        }
        if (player.getHealth() > maxHealth.getValue()) {
            player.setHealth(maxHealth.getValue());
        }
    }
}
