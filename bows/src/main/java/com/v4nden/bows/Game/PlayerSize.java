package com.v4nden.bows.Game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

/**
 * Размер игрока в игре: базовый игровой размер (1.0, или меньше со стартовым
 * бустом «Малыш») и временный множитель от бустов вроде «Уменьшения».
 * Итог = база × множитель, на игроке один модификатор bows:size.
 * В конце игры всё снимается — размер снова ванильный.
 */
public class PlayerSize {
    private static final NamespacedKey MODIFIER_KEY = new NamespacedKey("bows", "size");
    // модификаторы прошлых версий плагина, снимаются при сбросе
    private static final List<NamespacedKey> LEGACY_KEYS = List.of(
            new NamespacedKey("bows", "shrink"), new NamespacedKey("bows", "starter_small"));

    private static final Map<UUID, Double> base = new HashMap<>();
    private static final Map<UUID, Double> temporary = new HashMap<>();
    // номер последнего временного изменения: старый таймер не отменяет новое
    private static final Map<UUID, Integer> temporaryVersion = new HashMap<>();

    public static double getBase(Player player) {
        return base.getOrDefault(player.getUniqueId(), 1.0);
    }

    public static void setBase(Player player, double scale) {
        base.put(player.getUniqueId(), scale);
        apply(player);
    }

    /** Временный множитель; возвращает номер изменения для clearTemporary. */
    public static int setTemporary(Player player, double multiplier) {
        temporary.put(player.getUniqueId(), multiplier);
        int version = temporaryVersion.merge(player.getUniqueId(), 1, Integer::sum);
        apply(player);
        return version;
    }

    /** Убирает временный множитель, если после него не было нового. */
    public static void clearTemporary(Player player, int version) {
        if (temporaryVersion.getOrDefault(player.getUniqueId(), 0) != version) {
            return;
        }
        temporary.remove(player.getUniqueId());
        apply(player);
    }

    /** Ванильный размер: в конце игры и при выбывании. */
    public static void reset(Player player) {
        base.remove(player.getUniqueId());
        temporary.remove(player.getUniqueId());
        temporaryVersion.remove(player.getUniqueId());
        removeModifiers(player);
    }

    private static void apply(Player player) {
        removeModifiers(player);
        double scale = base.getOrDefault(player.getUniqueId(), 1.0)
                * temporary.getOrDefault(player.getUniqueId(), 1.0);
        if (scale == 1.0) {
            return;
        }
        // ADD_SCALAR: итоговый размер = ванильный * (1 + amount)
        player.getAttribute(Attribute.SCALE).addModifier(new AttributeModifier(MODIFIER_KEY, scale - 1,
                AttributeModifier.Operation.ADD_SCALAR, EquipmentSlotGroup.ANY));
    }

    private static void removeModifiers(Player player) {
        AttributeInstance scale = player.getAttribute(Attribute.SCALE);
        if (scale == null) {
            return;
        }
        for (AttributeModifier modifier : new ArrayList<>(scale.getModifiers())) {
            if (modifier.getKey().equals(MODIFIER_KEY) || LEGACY_KEYS.contains(modifier.getKey())) {
                scale.removeModifier(modifier);
            }
        }
    }
}
