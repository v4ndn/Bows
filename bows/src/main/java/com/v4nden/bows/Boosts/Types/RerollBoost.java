package com.v4nden.bows.Boosts.Types;

import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostGenerator;
import com.v4nden.bows.Boosts.StarterBoosts;

import net.md_5.bungee.api.ChatColor;

public class RerollBoost implements IBoostType {
    public Boost create(Player player) {
        return new BoostGenerator()
                .setMaterial(Material.DIAMOND)
                .setRarity(5)
                .setId("reroll")
                .setName("Реролл")
                .setDescription(ChatColor.of("#a6a6a6") + "Стартовый буст:" + ChatColor.RESET
                        + "\n Заменяет все бусты\n в инвентаре владельца\n на случайные новые")
                .setRunnable(consumer -> {
                    for (int slot : StarterBoosts.regularBoostSlots(consumer)) {
                        Boost old = Boost.fromItem(consumer.getInventory().getItem(slot));
                        old.dispose();
                        consumer.getInventory().setItem(slot, StarterBoosts.createRandomRegularBoost(consumer));
                    }

                    consumer.getWorld().spawnParticle(Particle.ENCHANT, consumer.getLocation().add(0, 1, 0), 80,
                            .5, .8, .5, .5);
                    consumer.playSound(consumer.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE,
                            SoundCategory.MASTER, 1, 1);
                })
                .build(player);
    }
}
