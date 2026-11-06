package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;

import net.md_5.bungee.api.ChatColor;

public class GlassWallBoost implements IBoostType {
    private static final int HALF_WIDTH = 2; // ширина 5
    private static final int HEIGHT = 3;
    private static final int DISTANCE = 2;

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.QUARTZ)
                .setRarity(1)
                .setId("glasswall")
                .setName("Стенка")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Ставит перед владельцем\n стеклянную стену 5х3\n в 2 блоках от него")
                .setRunnable(consumer -> {
                    // направление взгляда, прижатое к ближайшей стороне света
                    Vector forward = cardinal(consumer.getLocation().getDirection());
                    Vector side = new Vector(-forward.getZ(), 0, forward.getX());
                    Location base = consumer.getLocation().getBlock().getLocation()
                            .add(forward.clone().multiply(DISTANCE));

                    for (int y = 0; y < HEIGHT; y++) {
                        for (int i = -HALF_WIDTH; i <= HALF_WIDTH; i++) {
                            Block block = base.clone().add(side.clone().multiply(i)).add(0, y, 0).getBlock();
                            // стена не ломает существующие блоки
                            if (block.isPassable()) {
                                block.setType(Material.GLASS);
                            }
                        }
                    }

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(base, Sound.BLOCK_GLASS_PLACE, SoundCategory.MASTER, 1, 1);
                    }
                })
                .setDelay(.5f)
                .build(player);
    }

    private static Vector cardinal(Vector direction) {
        if (Math.abs(direction.getX()) > Math.abs(direction.getZ())) {
            return new Vector(Math.signum(direction.getX()), 0, 0);
        }
        return new Vector(0, 0, direction.getZ() >= 0 ? 1 : -1);
    }
}
