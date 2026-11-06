package com.v4nden.bows.Boosts.Types;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;

import com.v4nden.bows.Bows;
import com.v4nden.bows.BowsUtils;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Utils.TemporaryListener;

import net.md_5.bungee.api.ChatColor;

public class ThunderstormBoost implements IBoostType {
    private static final int STRIKES = 5;
    private static final double RADIUS = 7;
    private static final long STRIKE_INTERVAL_TICKS = 14L; // 0.7 секунды

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.NAUTILUS_SHELL)
                .setRarity(3)
                .setId("thunderstorm")
                .setName("Гроза")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Бросает заряд в сторону\n направления головы игрока,\n вокруг точки где заряд\n контактировал с блоком\n 5 раз бьёт молния в\n случайное место в радиусе\n 7 блоков")
                .setRunnable(consumer -> {
                    Snowball charge = BowsUtils.lauchCharge(consumer, Material.NAUTILUS_SHELL);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,
                                SoundCategory.MASTER, 1,
                                1);
                    }
                    new TemporaryListener<>(ProjectileHitEvent.class,
                            EventPriority.NORMAL, e -> {
                                ProjectileHitEvent event = (ProjectileHitEvent) e;
                                if (event.getEntity().equals(charge)) {
                                    Location center = charge.getLocation();
                                    for (int i = 0; i < STRIKES; i++) {
                                        Bukkit.getServer().getScheduler().runTaskLater(Bows.instance,
                                                () -> center.getWorld().strikeLightning(randomPointAround(center))
                                                        .setCausingPlayer(consumer),
                                                i * STRIKE_INTERVAL_TICKS);
                                    }
                                    return true;
                                }
                                return false;
                            });
                })
                .build(player);
    }

    // случайная точка в круге радиуса RADIUS, опущенная на поверхность
    private static Location randomPointAround(Location center) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(Math.PI * 2);
        double distance = Math.sqrt(random.nextDouble()) * RADIUS;

        World world = center.getWorld();
        double x = center.getX() + Math.cos(angle) * distance;
        double z = center.getZ() + Math.sin(angle) * distance;
        int y = world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1;
        return new Location(world, x, y, z);
    }
}
