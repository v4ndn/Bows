package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;

import com.v4nden.bows.Bows;
import com.v4nden.bows.BowsUtils;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;

import net.md_5.bungee.api.ChatColor;

public class DrillBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.NETHERITE_SCRAP)
                .setRarity(4)
                .setId("drill")
                .setName("Бур")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Выпускает заряд в сторону\n направления головы игрока, заряд \n разрушает все блоки в радиусе 5 - 3\n блоков от его текущей  позиции.\n Радиус убывает с временем, полёта заряда.\n Заряд существует 3 секунды")
                .setRunnable(consumer -> {
                    Snowball charge = BowsUtils.lauchCharge(consumer, Material.NETHERITE_SCRAP);
                    double created = System.currentTimeMillis();

                    int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(Bows.instance, () -> {
                        double percent = 1 - (System.currentTimeMillis() - created) / 3000;
                        for (int x = -3; x <= 3; x++) {
                            for (int z = -3; z <= 3; z++) {
                                for (int y = -3; y <= 3; y++) {
                                    Block block = consumer.getWorld()
                                            .getBlockAt(charge.getLocation().clone().add(Math.round(percent * x),
                                                    Math.round(percent * y), Math.round(percent * z)));

                                    if (block.getType().equals(Material.AIR))
                                        continue;
                                    charge.getWorld().playSound(charge.getLocation(),
                                            block.getBlockData().getSoundGroup().getBreakSound(), 3, 1);
                                    double random = Math.random();
                                    if (random > .9) {
                                        consumer.getInventory().addItem(block.getDrops().toArray(new ItemStack[0]));
                                    } else if (random > .5) {
                                        Item drop = block.getWorld().spawn(block.getLocation(), Item.class);
                                        ItemStack[] drops = block.getDrops().toArray(new ItemStack[0]);
                                        if (drops.length > 0)
                                            drop.setItemStack(drops[0]);
                                    }

                                    block.setType(Material.AIR);
                                }
                            }
                        }
                    }, 0, 1L);

                    Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                        Bukkit.getServer().getScheduler().cancelTask(task);
                    }, 60);
                })
                .build(player);
    }
}
