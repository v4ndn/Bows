package com.v4nden.bows.Boosts.Types;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Boosts.StarterBoosts;
import com.v4nden.bows.Game.Game;

import net.md_5.bungee.api.ChatColor;

/**
 * Добытчик: Спешка III на всю игру; сломанное бревно рушит бревна над ним,
 * сломанная руда — всю связанную жилу того же вида. Блоки ломаются по одному
 * за тик, с выпадением и опытом как от инструмента в руке.
 */
public class GathererBoost implements IBoostType {
    public static final String ID = "gatherer";
    private static final int MAX_LOGS = 64;
    private static final int MAX_VEIN = 32;

    public Boost create(Player player) {
        return new BoostGenerator()
                // палка, а не кирка: кирка — инструмент; выглядит как кирка
                .setMaterial(Material.STICK)
                .setIcon("minecraft:iron_pickaxe")
                .setRarity(Boost.STARTER_RARITY)
                .setId(ID)
                .setName("Добытчик")
                .setDescription(ChatColor.of("#a6a6a6") + "Пассивный стартовый буст:" + ChatColor.RESET
                        + "\n На всю игру даёт\n спешку третьего уровня.\n Сломанное бревно рушит\n все бревна над ним,\n сломанная руда — всю жилу")
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

    private static boolean hasBoost(Player player) {
        if (!Game.isRunning || !Game.gamePlayers.contains(player)) {
            return false;
        }
        Boost starter = StarterBoosts.current(player);
        return starter != null && starter.getId().equals(ID);
    }

    private static boolean isOre(Material material) {
        return material.name().endsWith("_ORE") || material == Material.ANCIENT_DEBRIS;
    }

    // обычная и глубинносланцевая руда — одна жила
    private static String oreKind(Material material) {
        return material.name().replace("DEEPSLATE_", "");
    }

    // руда той же жилы, связанная с начальным блоком (соседи по граням, рёбрам и углам)
    private static List<Block> vein(Block start, String kind) {
        List<Block> found = new ArrayList<>();
        Set<Block> seen = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);
        while (!queue.isEmpty() && found.size() < MAX_VEIN) {
            Block block = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        Block next = block.getRelative(dx, dy, dz);
                        if (seen.add(next) && isOre(next.getType()) && oreKind(next.getType()).equals(kind)) {
                            found.add(next);
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return found;
    }

    // столб бревен над сломанным
    private static List<Block> logsAbove(Block start) {
        List<Block> found = new ArrayList<>();
        Block next = start.getRelative(0, 1, 0);
        while (found.size() < MAX_LOGS && Tag.LOGS.isTagged(next.getType())) {
            found.add(next);
            next = next.getRelative(0, 1, 0);
        }
        return found;
    }

    public static class Handler implements Listener {
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onBreak(BlockBreakEvent e) {
            Player player = e.getPlayer();
            Material type = e.getBlock().getType();
            if (!hasBoost(player)) {
                return;
            }
            List<Block> blocks;
            if (Tag.LOGS.isTagged(type)) {
                blocks = logsAbove(e.getBlock());
            } else if (isOre(type)) {
                blocks = vein(e.getBlock(), oreKind(type));
            } else {
                return;
            }
            if (blocks.isEmpty()) {
                return;
            }
            ItemStack tool = player.getInventory().getItemInMainHand().clone();

            new BukkitRunnable() {
                int index = 0;

                @Override
                public void run() {
                    // блок могли сломать или заменить, пока шла цепочка
                    while (index < blocks.size()) {
                        Block block = blocks.get(index++);
                        boolean still = Tag.LOGS.isTagged(block.getType()) || isOre(block.getType());
                        if (still) {
                            block.getWorld().playSound(block.getLocation(),
                                    block.getBlockData().getSoundGroup().getBreakSound(), SoundCategory.BLOCKS, 1, 1);
                            block.breakNaturally(tool, true, true);
                            return;
                        }
                    }
                    cancel();
                }
            }.runTaskTimer(Bows.instance, 1L, 1L);
        }
    }
}
