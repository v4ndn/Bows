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

public class InvisibilityBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.FERMENTED_SPIDER_EYE).setRarity(2).setId("invisibility").setName("Невидимость")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Накладывает на владельца\n буста эффект невидимости\n на 10 секунд, частицы\n эффекта не видны")
                .setRunnable(consumer -> {
                    // без частиц, иначе они выдают позицию игрока
                    consumer.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 10, 0, false, false));

                    consumer.getLocation().getWorld().spawnParticle(Particle.LARGE_SMOKE, consumer.getLocation(), 100,
                            1, 1,
                            1, .02);

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(consumer.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE,
                                SoundCategory.MASTER,
                                1,
                                1);
                    }

                }).setDelay(3).build(player);
    }
}
