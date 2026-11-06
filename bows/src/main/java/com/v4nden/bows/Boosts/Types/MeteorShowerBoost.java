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
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.v4nden.bows.Bows;
import com.v4nden.bows.BowsUtils;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;
import com.v4nden.bows.Utils.TemporaryListener;

import net.md_5.bungee.api.ChatColor;

public class MeteorShowerBoost implements IBoostType {
    private static final double RADIUS = 50;
    private static final int DURATION_TICKS = 20 * 5;
    private static final int INTERVAL_TICKS = 2; // ~50 метеоритов за 5 секунд

    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.BLAZE_ROD)
                .setRarity(5)
                .setId("meteorshower")
                .setName("Метеоритный дождь")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Бросает заряд в сторону\n направления головы игрока,\n вокруг точки где заряд\n контактировал с блоком\n 5 секунд падают метеориты\n в радиусе 50 блоков")
                .setRunnable(consumer -> {
                    Snowball charge = BowsUtils.lauchCharge(consumer, Material.BLAZE_ROD);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,
                                SoundCategory.MASTER, 1, 1);
                    }
                    new TemporaryListener<>(ProjectileHitEvent.class, EventPriority.NORMAL, e -> {
                        ProjectileHitEvent event = (ProjectileHitEvent) e;
                        if (!event.getEntity().equals(charge)) {
                            return false;
                        }
                        startShower(consumer, charge.getLocation());
                        return true;
                    });
                })
                .build(player);
    }

    private static void startShower(Player consumer, Location center) {
        Game.gamePlayers.forEach(gamePlayer -> {
            if (gamePlayer.getWorld().equals(center.getWorld())
                    && gamePlayer.getLocation().distance(center) < RADIUS + 15) {
                gamePlayer.sendTitle("МЕТЕОРИТНЫЙ ДОЖДЬ", "!!!", 3, 30, 10);
            }
        });
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.MASTER, .7f, 1.5f);
        }

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ticks > DURATION_TICKS) {
                    cancel();
                    return;
                }
                dropMeteor(consumer, randomPointAround(center));
                ticks += INTERVAL_TICKS;
            }
        }.runTaskTimer(Bows.instance, 0L, INTERVAL_TICKS);
    }

    // как метеорит из MeteorBoost: горящий TNT в 150 блоках над точкой
    private static void dropMeteor(Player owner, Location ground) {
        TNTPrimed tnt = ground.getWorld().spawn(ground.clone().add(new Vector(0, 150, 0)), TNTPrimed.class);
        tnt.setSource(owner);
        tnt.setVelocity(new Vector(0, -3, 0));
        tnt.setIsIncendiary(true);
        tnt.setFuseTicks(60);
        ground.getWorld().playSound(ground, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.MASTER, 4, .6f);
    }

    // случайная точка в круге радиуса RADIUS на поверхности
    private static Location randomPointAround(Location center) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double angle = random.nextDouble(Math.PI * 2);
        double distance = Math.sqrt(random.nextDouble()) * RADIUS;
        World world = center.getWorld();
        int x = (int) Math.floor(center.getX() + Math.cos(angle) * distance);
        int z = (int) Math.floor(center.getZ() + Math.sin(angle) * distance);
        return world.getHighestBlockAt(x, z).getLocation().add(.5, 1, .5);
    }
}
