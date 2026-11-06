package com.v4nden.bows.Boosts.Types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlotGroup;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Boosts.StarterBoosts;
import com.v4nden.bows.Game.Game;
import com.v4nden.bows.Game.PlayerSize;

import net.md_5.bungee.api.ChatColor;

/**
 * Великан: начинает игру большим, убийства уменьшают базовый размер по экспоненте —
 * каждый шаг вдвое больше предыдущего. До MIN_SCALE за K = ⌈игроков на старте / 2⌉
 * убийств: размер после k убийств = START - (START - MIN) * (2^k - 1) / (2^K - 1).
 */
public class GiantBoost implements IBoostType {
    public static final String ID = "giant";
    private static final double START_SCALE = 1.5;
    private static final double MIN_SCALE = .5;
    // макс. здоровье линейно от размера: START_SCALE -> MAX_HEALTH_AT_START, MIN_SCALE -> MAX_HEALTH_AT_MIN
    private static final double MAX_HEALTH_AT_START = 30;
    private static final double MAX_HEALTH_AT_MIN = 18;
    private static final NamespacedKey HEALTH_KEY = new NamespacedKey("bows", "starter_giant_health");

    // убийства владельцев «Великана» в текущей игре
    private static final Map<UUID, Integer> kills = new HashMap<>();

    public Boost create(Player player) {
        return new BoostGenerator()
                // самородок, а не булава: булава — оружие; выглядит как булава
                .setMaterial(Material.IRON_NUGGET)
                .setIcon("minecraft:mace")
                .setRarity(Boost.STARTER_RARITY)
                .setId(ID)
                .setName("Великан")
                .setDescription(ChatColor.of("#a6a6a6") + "Пассивный стартовый буст:" + ChatColor.RESET
                        + "\n Владелец начинает игру\n размером 1.5 и с 15\n сердцами. Убийства\n уменьшают его, каждое\n вдвое сильнее прошлого:\n до 0.5 и 9 сердец\n за (игроков / 2) убийств")
                .setPassive(true)
                .setRunnable(consumer -> {
                })
                .build(player);
    }

    public static void apply(Player player) {
        PlayerSize.setBase(player, START_SCALE);
        setMaxHealthFor(player, START_SCALE);
        player.setHealth(player.getAttribute(Attribute.MAX_HEALTH).getValue());
    }

    // меньше великан — меньше и макс. здоровье; текущее урезается до нового максимума
    private static void setMaxHealthFor(Player player, double scale) {
        double progress = (scale - MIN_SCALE) / (START_SCALE - MIN_SCALE);
        double target = MAX_HEALTH_AT_MIN + (MAX_HEALTH_AT_START - MAX_HEALTH_AT_MIN) * progress;
        resetHealth(player);
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        maxHealth.addModifier(new AttributeModifier(HEALTH_KEY, target - maxHealth.getBaseValue(),
                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.ANY));
        if (player.getHealth() > maxHealth.getValue()) {
            player.setHealth(maxHealth.getValue());
        }
    }

    // модификатор сохраняется в данных игрока — снимаем в конце игры (StarterBoosts.resetPassive)
    public static void resetHealth(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        for (AttributeModifier modifier : new ArrayList<>(maxHealth.getModifiers())) {
            if (modifier.getKey().equals(HEALTH_KEY)) {
                maxHealth.removeModifier(modifier);
            }
        }
        if (player.getHealth() > maxHealth.getValue()) {
            player.setHealth(maxHealth.getValue());
        }
    }

    public static void reset() {
        kills.clear();
    }

    public static void onKill(Player killer) {
        Boost starter = StarterBoosts.current(killer);
        if (starter == null || !starter.getId().equals(ID)) {
            return;
        }
        int killCount = kills.merge(killer.getUniqueId(), 1, Integer::sum);
        int killsToMin = Math.max(1, (int) Math.ceil(Game.startPlayerCount / 2.0));
        double progress = Math.min(1, (Math.pow(2, killCount) - 1) / (Math.pow(2, killsToMin) - 1));
        double scale = START_SCALE - (START_SCALE - MIN_SCALE) * progress;
        PlayerSize.setBase(killer, scale);
        setMaxHealthFor(killer, scale);

        killer.getWorld().spawnParticle(Particle.POOF, killer.getLocation().add(0, 1, 0), 20, .4, .6, .4, .02);
        killer.playSound(killer.getLocation(), Sound.ENTITY_PUFFER_FISH_BLOW_OUT, SoundCategory.MASTER, 1, 1.2f);
    }
}
