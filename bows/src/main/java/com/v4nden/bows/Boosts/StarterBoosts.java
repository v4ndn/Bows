package com.v4nden.bows.Boosts;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.v4nden.bows.Boosts.Types.ChainmailBoost;
import com.v4nden.bows.Boosts.Types.DoubleJumpBoost;
import com.v4nden.bows.Boosts.Types.GathererBoost;
import com.v4nden.bows.Boosts.Types.GiantBoost;
import com.v4nden.bows.Boosts.Types.QuiverBoost;
import com.v4nden.bows.Boosts.Types.SecondChanceBoost;
import com.v4nden.bows.Boosts.Types.SmallBoost;
import com.v4nden.bows.Boosts.Types.ToughBoost;
import com.v4nden.bows.Game.PlayerSize;

/**
 * Стартовый буст лежит в правом слоте хотбара и не перекладывается оттуда.
 * Обычные бусты — всё, что есть в BoostTypes; стартовые в обмен и реролл не попадают.
 */
public class StarterBoosts {
    public static final int SLOT = 8;

    private static final Random random = new Random();

    public static void give(Player player) {
        StarterBoostTypes[] types = StarterBoostTypes.values();
        give(player, types[random.nextInt(types.length)]);
    }

    public static void give(Player player, StarterBoostTypes type) {
        PlayerInventory inventory = player.getInventory();
        Boost previous = Boost.fromItem(inventory.getItem(SLOT));
        if (previous != null) {
            previous.dispose();
        }
        Boost boost = type.getBoost(player);
        boost.setLockedSlot(SLOT);
        inventory.setItem(SLOT, boost.getItem());
        applyPassive(player, type);
    }

    // пассивные стартовые бусты действуют с выдачи и до конца игры
    private static void applyPassive(Player player, StarterBoostTypes type) {
        switch (type) {
            case SMALL -> SmallBoost.apply(player);
            case TOUGH -> ToughBoost.apply(player);
            case CHAINMAIL -> ChainmailBoost.apply(player);
            case GATHERER -> GathererBoost.apply(player);
            case DOUBLEJUMP -> DoubleJumpBoost.apply(player);
            case GIANT -> GiantBoost.apply(player);
            case QUIVER -> {
                ItemStack arrows = player.getInventory().getItem(1);
                if (arrows != null && arrows.getType() == Material.ARROW) {
                    arrows.setAmount(QuiverBoost.MAX_ARROWS);
                }
            }
            default -> {
            }
        }
    }

    /** Снимает эффекты пассивных стартовых бустов (конец игры, выбывание). */
    public static void resetPassive(Player player) {
        PlayerSize.reset(player);
        GathererBoost.reset(player);
        ToughBoost.reset(player);
        GiantBoost.resetHealth(player);
    }

    /** Сколько стрел игрок может носить: больше с «Колчаном». */
    public static int maxArrows(Player player) {
        Boost starter = current(player);
        return starter != null && starter.getId().equals(QuiverBoost.ID) ? QuiverBoost.MAX_ARROWS : 20;
    }

    /** Если у игрока есть «Второй шанс» — тратит его и с шансом спасает от смерти. */
    public static boolean tryRevive(Player player) {
        ItemStack item = player.getInventory().getItem(SLOT);
        Boost boost = Boost.fromItem(item);
        if (boost == null || !boost.getId().equals(SecondChanceBoost.ID)) {
            return false;
        }
        // буст тратится в любом случае, срабатывает — с шансом
        boost.dispose();
        player.getInventory().setItem(SLOT, null);
        if (random.nextDouble() >= SecondChanceBoost.REVIVE_CHANCE) {
            return false;
        }
        SecondChanceBoost.revive(player, true);
        boost.broadcastUse();
        return true;
    }

    /** Неиспользованный стартовый буст игрока, если он ещё лежит в своём слоте. */
    public static Boost current(Player player) {
        Boost boost = Boost.fromItem(player.getInventory().getItem(SLOT));
        return boost != null && isStarter(boost) ? boost : null;
    }

    /** Слоты инвентаря с обычными (не стартовыми) живыми бустами. */
    public static List<Integer> regularBoostSlots(Player player) {
        List<Integer> slots = new ArrayList<>();
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            Boost boost = Boost.fromItem(contents[slot]);
            if (boost != null && !isStarter(boost)) {
                slots.add(slot);
            }
        }
        return slots;
    }

    /** Забирает у игрока все обычные бусты и возвращает их id. */
    public static List<String> takeRegularBoosts(Player player) {
        List<String> ids = new ArrayList<>();
        for (int slot : regularBoostSlots(player)) {
            Boost boost = Boost.fromItem(player.getInventory().getItem(slot));
            ids.add(boost.getId());
            boost.dispose();
            player.getInventory().setItem(slot, null);
        }
        return ids;
    }

    /** Новый предмет обычного буста по id для владельца. */
    public static ItemStack createRegularBoost(String id, Player owner) {
        return BoostTypes.valueOf(id.toUpperCase()).getBoost(owner).getItem();
    }

    public static ItemStack createRandomRegularBoost(Player owner) {
        BoostTypes[] types = BoostTypes.values();
        return types[random.nextInt(types.length)].getBoost(owner).getItem();
    }

    private static boolean isStarter(Boost boost) {
        for (StarterBoostTypes type : StarterBoostTypes.values()) {
            if (type.name().equalsIgnoreCase(boost.getId())) {
                return true;
            }
        }
        return false;
    }
}
