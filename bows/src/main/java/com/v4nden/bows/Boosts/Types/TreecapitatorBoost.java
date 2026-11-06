package com.v4nden.bows.Boosts.Types;

import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Boosts.StarterBoosts;
import com.v4nden.bows.Game.Game;

import net.md_5.bungee.api.ChatColor;

/**
 * Лесоруб: когда владелец ломает бревно, бревна над ним подряд ломаются сами,
 * по одному за тик — ствол осыпается сверху вниз.
 */
public class TreecapitatorBoost implements IBoostType {
    public static final String ID = "treecapitator";
    private static final int MAX_LOGS = 64;

    public Boost create(Player player) {
        return new BoostGenerator()
                // палка, а не топор: топором на ПКМ обтёсывают брёвна; выглядит как топор
                .setMaterial(Material.STICK)
                .setIcon("minecraft:iron_axe")
                .setRarity(5)
                .setId(ID)
                .setName("Лесоруб")
                .setDescription(ChatColor.of("#a6a6a6") + "Пассивный стартовый буст:" + ChatColor.RESET
                        + "\n На всю игру: если\n владелец ломает бревно,\n все бревна над ним\n тоже ломаются")
                .setPassive(true)
                .setRunnable(consumer -> {
                })
                .build(player);
    }

    private static boolean hasBoost(Player player) {
        if (!Game.isRunning || !Game.gamePlayers.contains(player)) {
            return false;
        }
        Boost starter = StarterBoosts.current(player);
        return starter != null && starter.getId().equals(ID);
    }

    public static class Handler implements Listener {
        @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
        public void onBreak(BlockBreakEvent e) {
            Player player = e.getPlayer();
            if (!Tag.LOGS.isTagged(e.getBlock().getType()) || !hasBoost(player)) {
                return;
            }
            ItemStack tool = player.getInventory().getItemInMainHand().clone();

            new BukkitRunnable() {
                Block next = e.getBlock().getRelative(0, 1, 0);
                int broken = 0;

                @Override
                public void run() {
                    if (broken >= MAX_LOGS || !Tag.LOGS.isTagged(next.getType())) {
                        cancel();
                        return;
                    }
                    BlockData data = next.getBlockData();
                    next.getWorld().playSound(next.getLocation(), data.getSoundGroup().getBreakSound(),
                            SoundCategory.BLOCKS, 1, 1);
                    next.breakNaturally(tool);
                    next = next.getRelative(0, 1, 0);
                    broken++;
                }
            }.runTaskTimer(Bows.instance, 1L, 1L);
        }
    }
}
