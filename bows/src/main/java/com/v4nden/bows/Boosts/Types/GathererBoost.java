package com.v4nden.bows.Boosts.Types;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Campfire;
import org.bukkit.block.Furnace;
import org.bukkit.block.data.Lightable;
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
 * Вокруг владельца печи, коптильни, плавильни, варочные стойки и костры
 * работают втрое быстрее, саженцы растут в 10 раз быстрее.
 */
public class GathererBoost implements IBoostType {
    public static final String ID = "gatherer";
    private static final int MAX_LOGS = 64;
    private static final int MAX_VEIN = 32;
    public static final int AURA_RADIUS = 8;
    // лишних тиков работы за тик: 2 — втрое быстрее
    private static final int EXTRA_WORK_TICKS = 2;
    // лишних случайных тиков саженцам: 9 — в 10 раз быстрее
    private static final int EXTRA_GROWTH_TICKS = 9;

    public Boost create(Player player) {
        return new BoostGenerator()
                // палка, а не кирка: кирка — инструмент; выглядит как кирка
                .setMaterial(Material.STICK)
                .setIcon("minecraft:iron_pickaxe")
                .setRarity(Boost.STARTER_RARITY)
                .setId(ID)
                .setName("Добытчик")
                .setDescription(ChatColor.of("#a6a6a6") + "Пассивный стартовый буст:" + ChatColor.RESET
                        + "\n На всю игру даёт\n спешку третьего уровня.\n Сломанное бревно рушит\n все бревна над ним,\n сломанная руда — всю жилу.\n В радиусе 8 блоков печи,\n варочные стойки и костры\n работают в 3 раза быстрее,\n а деревья растут\n в 10 раз быстрее")
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

    public static void startAura() {
        Bukkit.getScheduler().runTaskTimer(Bows.instance, () -> {
            // в остановке времени мир стоит — и печи вместе с ним
            if (!Game.isRunning || Bukkit.getServerTickManager().isFrozen()) {
                return;
            }
            // блок, до которого дотянулись два добытчика, ускоряется один раз
            Set<Block> worked = new HashSet<>();
            Set<Long> grown = new HashSet<>();
            for (Player player : Game.gamePlayers) {
                if (hasBoost(player)) {
                    speedUpWork(player.getLocation(), worked);
                    speedUpGrowth(player.getLocation(), grown);
                }
            }
        }, 1L, 1L);
    }

    private static void speedUpWork(Location center, Set<Block> worked) {
        World world = center.getWorld();
        int minChunkX = (center.getBlockX() - AURA_RADIUS) >> 4, maxChunkX = (center.getBlockX() + AURA_RADIUS) >> 4;
        int minChunkZ = (center.getBlockZ() - AURA_RADIUS) >> 4, maxChunkZ = (center.getBlockZ() + AURA_RADIUS) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!world.isChunkLoaded(cx, cz)) {
                    continue;
                }
                Chunk chunk = world.getChunkAt(cx, cz);
                for (BlockState state : chunk.getTileEntities(block -> isWorkBlock(block.getType())
                        && block.getLocation().add(.5, .5, .5).distanceSquared(center) <= AURA_RADIUS * AURA_RADIUS,
                        true)) {
                    if (worked.add(state.getBlock())) {
                        for (int i = 0; i < EXTRA_WORK_TICKS; i++) {
                            work(state);
                        }
                        state.update(false, false);
                    }
                }
            }
        }
    }

    private static boolean isWorkBlock(Material material) {
        return material == Material.FURNACE || material == Material.BLAST_FURNACE || material == Material.SMOKER
                || material == Material.BREWING_STAND || Tag.CAMPFIRES.isTagged(material);
    }

    // один лишний тик работы. Ванилла сама доводит счётчик до конца и выдаёт результат,
    // поэтому последний шаг ей и оставляем — иначе она перескочит момент готовности
    private static void work(BlockState state) {
        if (state instanceof Furnace furnace) {
            if (furnace.getBurnTime() > 1 && furnace.getCookTime() > 0
                    && furnace.getCookTime() + 1 < furnace.getCookTimeTotal()) {
                furnace.setCookTime((short) (furnace.getCookTime() + 1));
                // топливо тоже горит быстрее: на предмет его уходит столько же
                furnace.setBurnTime((short) (furnace.getBurnTime() - 1));
            }
        } else if (state instanceof BrewingStand stand) {
            if (stand.getBrewingTime() > 1) {
                stand.setBrewingTime(stand.getBrewingTime() - 1);
            }
        } else if (state instanceof Campfire campfire) {
            if (!((Lightable) campfire.getBlockData()).isLit()) {
                return;
            }
            for (int slot = 0; slot < campfire.getSize(); slot++) {
                if (campfire.getItem(slot) != null && campfire.getCookTime(slot) + 1 < campfire.getCookTimeTotal(slot)) {
                    campfire.setCookTime(slot, campfire.getCookTime(slot) + 1);
                }
            }
        }
    }

    // как ванильные случайные тики: в каждой секции 16³ ещё EXTRA_GROWTH_TICKS × randomTickSpeed
    // случайных блоков; попавшим саженцам — случайный тик (рост с теми же шансами и светом)
    private static void speedUpGrowth(Location center, Set<Long> grown) {
        World world = center.getWorld();
        Integer speed = world.getGameRuleValue(GameRule.RANDOM_TICK_SPEED);
        if (speed == null || speed <= 0) {
            return;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int minY = Math.max(world.getMinHeight(), center.getBlockY() - AURA_RADIUS) >> 4;
        int maxY = Math.min(world.getMaxHeight() - 1, center.getBlockY() + AURA_RADIUS) >> 4;
        for (int sx = (center.getBlockX() - AURA_RADIUS) >> 4; sx <= (center.getBlockX() + AURA_RADIUS) >> 4; sx++) {
            for (int sz = (center.getBlockZ() - AURA_RADIUS) >> 4; sz <= (center.getBlockZ() + AURA_RADIUS) >> 4; sz++) {
                if (!world.isChunkLoaded(sx, sz)) {
                    continue;
                }
                for (int sy = minY; sy <= maxY; sy++) {
                    // секция, которую уже ускорил другой добытчик, второй раз не ускоряется
                    if (!grown.add(((long) sx & 0x3FFFFF) << 42 | ((long) sz & 0x3FFFFF) << 20 | (sy & 0xFFFFF))) {
                        continue;
                    }
                    for (int i = 0; i < speed * EXTRA_GROWTH_TICKS; i++) {
                        Block block = world.getBlockAt((sx << 4) + random.nextInt(16), (sy << 4) + random.nextInt(16),
                                (sz << 4) + random.nextInt(16));
                        if (Tag.SAPLINGS.isTagged(block.getType())
                                && block.getLocation().add(.5, .5, .5).distanceSquared(center) <= AURA_RADIUS * AURA_RADIUS) {
                            block.randomTick();
                        }
                    }
                }
            }
        }
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
