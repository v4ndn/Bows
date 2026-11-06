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
import com.v4nden.bows.Game.Game;

import net.md_5.bungee.api.ChatColor;

public class GlobalLevitationBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.POPPED_CHORUS_FRUIT).setRarity(4).setId("globallevitation")
                .setName("Глобальная левитация")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Накладывает на всех живых\n игроков на карте, включая\n владельца, эффект левитации\n второго уровня на 5 секунд")
                .setRunnable(consumer -> {
                    Game.targets().forEach(gamePlayer -> {
                        gamePlayer.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20 * 5, 1));
                        gamePlayer.getWorld().spawnParticle(Particle.END_ROD, gamePlayer.getLocation(), 30,
                                .5, .2, .5, .05);
                    });

                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.playSound(p.getLocation(), Sound.ENTITY_SHULKER_BULLET_HIT,
                                SoundCategory.MASTER,
                                1,
                                .5f);
                    }

                }).setDelay(3).build(player);
    }
}
