package com.v4nden.bows.Boosts.Types;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;

import net.md_5.bungee.api.ChatColor;

public class SpeedBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.LIGHT_BLUE_DYE).setRarity(1).setId("spedd").setName("Скорость")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Накладывает на владельца\n буста эффект скорости\n первого уровня на 10 секунд")
                .setRunnable(consumer -> {
                    consumer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 10, 1));

                    consumer.getLocation().getWorld().spawnParticle(Particle.CLOUD, consumer.getLocation(), 100,
                            2, 2,
                            2);

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.BLOCK_BEACON_ACTIVATE,
                                SoundCategory.MASTER,
                                1,
                                1);
                    }

                }).setDelay(3).build(player);
    }
}
