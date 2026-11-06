package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.util.Vector;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Utils.TemporaryListener;

import net.md_5.bungee.api.ChatColor;

public class FullDrawBoost implements IBoostType {
    private static final int ARROWS = 5;
    // скорость стрелы из лука при полной натяжке
    private static final double FULL_SPEED = 3.0;

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.FLINT)
                .setRarity(2)
                .setId("fulldraw")
                .setName("Полная натяжка")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Следующие 5 стрел при\n любой натяжке лука\n вылетают на полной\n скорости и с полным уроном")
                .setRunnable(consumer -> {
                    consumer.playSound(consumer.getLocation(), Sound.ITEM_CROSSBOW_LOADING_END,
                            SoundCategory.MASTER, 1, 1);

                    int[] left = { ARROWS };
                    new TemporaryListener<EntityShootBowEvent>(EntityShootBowEvent.class, EventPriority.HIGH, e -> {
                        if (e.isCancelled() || !e.getEntity().equals(consumer)
                                || !(e.getProjectile() instanceof AbstractArrow arrow)) {
                            return false;
                        }
                        Vector velocity = arrow.getVelocity();
                        Vector direction = velocity.lengthSquared() > 1e-6 ? velocity.normalize()
                                : consumer.getLocation().getDirection();
                        arrow.setVelocity(direction.multiply(FULL_SPEED));
                        // как у полностью натянутой стрелы
                        arrow.setCritical(true);

                        left[0]--;
                        return left[0] <= 0;
                    });
                })
                .build(player);
    }
}
