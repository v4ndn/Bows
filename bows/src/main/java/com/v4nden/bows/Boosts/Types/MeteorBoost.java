package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.Vector;

import com.v4nden.bows.Bows;
import com.v4nden.bows.BowsUtils;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;
import com.v4nden.bows.Utils.TemporaryListener;

import net.md_5.bungee.api.ChatColor;

public class MeteorBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.FIRE_CHARGE)
                .setRarity(2)
                .setId("meteor")
                .setName("Метеор")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Бросает заряд в сторону\n направления головы игрока, \n на точке где заряд \n контактировал с блоком\n вызывает заряженный\n динамит падающий сверху")
                .setRunnable(consumer -> {
                    Snowball charge = BowsUtils.lauchCharge(consumer, Material.FIRE_CHARGE);
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH,
                                SoundCategory.MASTER, 1,
                                1);
                    }
                    new TemporaryListener<>(ProjectileHitEvent.class,
                            EventPriority.NORMAL, e -> {
                                ProjectileHitEvent event = (ProjectileHitEvent) e;
                                if (event.getEntity().equals(charge)) {
                                    Location loc = event.getEntity().getLocation();

                                    consumer.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 100, 1);

                                    for (int i = 0; i <= 30; i++) {
                                        final Integer fi = Integer.valueOf(i);
                                        Bukkit.getServer().getScheduler().runTaskLater(Bows.instance, () -> {
                                            consumer.getWorld().playSound(loc, Sound.BLOCK_NOTE_BLOCK_BELL, 15, fi / 9);
                                        }, i * 2);
                                    }

                                    Game.gamePlayers.forEach((gamePlayer) -> {
                                        if (gamePlayer.getLocation().distance(loc) < 15) {
                                            gamePlayer.sendTitle("МЕТЕОРИТ", "!!!", 3, 15, 3);
                                        }
                                    });

                                    TNTPrimed tnt = consumer.getWorld().spawn(loc.add(new Vector(0, 150, 0)),
                                            TNTPrimed.class);

                                    tnt.setSource(consumer);
                                    tnt.setVelocity(new Vector(0, -3, 0));
                                    tnt.setIsIncendiary(true);
                                    tnt.setFuseTicks(60);

                                    return true;
                                }
                                return false;
                            });
                })
                .build(player);
    }
}
