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

public class RegenerationBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.GHAST_TEAR).setRarity(2).setId("regeneration").setName("Регенерация")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Накладывает на владельца\n буста эффект регенерации\n второго уровня на 10 секунд\n (восстанавливает ~4 сердца)")
                .setRunnable(consumer -> {
                    consumer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 1));

                    consumer.getLocation().getWorld().spawnParticle(Particle.HEART, consumer.getLocation().add(0, 1, 0),
                            15,
                            1, 1,
                            1);

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT,
                                SoundCategory.MASTER,
                                1,
                                1);
                    }

                }).setDelay(3).build(player);
    }
}
