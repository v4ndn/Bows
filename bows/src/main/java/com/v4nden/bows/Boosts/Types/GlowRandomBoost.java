package com.v4nden.bows.Boosts.Types;

import java.util.List;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Game.Game;

import net.md_5.bungee.api.ChatColor;

public class GlowRandomBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.GLOWSTONE_DUST)
                .setRarity(2)
                .setId("glowrandom")
                .setName("Подсветить рандомного игрока")
                .setDescription(ChatColor.of("#a6a6a6") + "При использовании:" + ChatColor.RESET
                        + "\n Спустя задержку перед применением\n выдаёт случайному игрокув текущей\n игре эффект свечения, звук задержки\n перед эффектом слышан\n как владельцу, так и\n случайному игроку")
                .setRunnable(consumer -> {
                    List<LivingEntity> others = Game.selectableTargetsExcept(consumer);
                    if (others.isEmpty()) {
                        return;
                    }
                    LivingEntity randomplayer = others.get(new Random().nextInt(others.size()));

                    randomplayer.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 5, 1));
                })
                .setDelay(1)
                .build(player);
    }
}
