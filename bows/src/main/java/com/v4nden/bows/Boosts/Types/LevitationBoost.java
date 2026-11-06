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

public class LevitationBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.PHANTOM_MEMBRANE).setRarity(1).setId("levitation").setName("Левитация")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Накладывает на владельца\n буста эффект левитации\n на 10 секунд")
                .setRunnable(consumer -> {
                    consumer.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20 * 10, 0));

                    consumer.getWorld().spawnParticle(Particle.END_ROD, consumer.getLocation(), 40,
                            .5, .2, .5, .05);

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_SHULKER_BULLET_HIT,
                                SoundCategory.MASTER,
                                1,
                                1);
                    }

                }).setDelay(1).build(player);
    }
}
