package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;

import net.md_5.bungee.api.ChatColor;

public class WindBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.LIGHT_GRAY_DYE)
                .setRarity(2)
                .setId("wind")
                .setPreviewRadius(15)
                .setName("Ветер")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Запускает владельца\n относительно поворота головы\n вперёд ~30 блоков")
                .setRunnable(consumer -> {
                    Game.targets().forEach(gamePlayer -> {
                        if (gamePlayer.equals(consumer))
                            return;
                        if (gamePlayer.getLocation().distance(consumer.getLocation()) > 15)
                            return;
                        gamePlayer.setVelocity(consumer.getLocation().getDirection().multiply((1.5)));
                    });

                    Location location = consumer.getLocation().add(0, .7, 0);
                    Vector direction = location.getDirection();

                    for (int i = 0; i <= 50; i++) {
                        consumer.getWorld().spawnParticle(Particle.CLOUD, location.getX(), location.getY(),
                                location.getZ(),
                                0,
                                (float) direction.getX() * Math.random(), (float) direction.getY() * Math.random(),
                                (float) direction.getZ() * Math.random(), 3 + Math.random(), null);
                    }

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_BREEZE_IDLE_AIR,
                                SoundCategory.MASTER, 1,
                                1);
                    }
                })
                .setDelay(.5f)
                .build(player);
    }
}
