package com.v4nden.bows.Boosts.Types;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;

import net.md_5.bungee.api.ChatColor;

public class SecondChanceBoost implements IBoostType {
    public static final String ID = "secondchance";
    // шанс спасти от смерти; ручное использование срабатывает всегда
    public static final double REVIVE_CHANCE = .5;

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.TOTEM_OF_UNDYING)
                .setRarity(5)
                .setId(ID)
                .setName("Второй шанс")
                .setDescription(ChatColor.of("#a6a6a6") + "Стартовый буст:" + ChatColor.RESET
                        + "\n Если владелец умирает,\n с шансом 50% вместо смерти\n ему восстанавливается\n половина здоровья и его\n переносит в случайную точку\n зоны. При использовании\n срабатывает всегда")
                .setRunnable(consumer -> revive(consumer, false))
                .build(player);
    }

    public static void revive(Player player, boolean fromDeath) {
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH).getValue();
        player.setHealth(Math.max(fromDeath ? 0 : player.getHealth(), maxHealth / 2));
        player.setFireTicks(0);
        player.setFallDistance(0);

        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 60,
                .5, .8, .5, .3);
        player.teleport(randomPointInZone(player.getWorld()));
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 60,
                .5, .8, .5, .3);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, SoundCategory.MASTER, .6f, 1);
        }
    }

    // случайная точка внутри текущей границы мира, на поверхности
    private static Location randomPointInZone(World world) {
        WorldBorder border = world.getWorldBorder();
        double half = Math.max(1, border.getSize() / 2 - 2);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double x = border.getCenter().getX() + random.nextDouble(-half, half);
        double z = border.getCenter().getZ() + random.nextDouble(-half, half);
        return world.getHighestBlockAt((int) Math.floor(x), (int) Math.floor(z)).getLocation().add(.5, 1, .5);
    }
}
