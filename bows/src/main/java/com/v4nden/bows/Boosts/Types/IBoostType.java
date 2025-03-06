package com.v4nden.bows.Boosts.Types;

import org.bukkit.entity.Player;

import com.v4nden.bows.Boosts.Boost;

public interface IBoostType {
    public Boost create(Player player);
}
