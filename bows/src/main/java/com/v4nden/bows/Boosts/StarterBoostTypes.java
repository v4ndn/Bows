package com.v4nden.bows.Boosts;

import org.bukkit.entity.Player;

import com.v4nden.bows.Boosts.Types.ChainmailBoost;
import com.v4nden.bows.Boosts.Types.DoubleJumpBoost;
import com.v4nden.bows.Boosts.Types.ElusiveBoost;
import com.v4nden.bows.Boosts.Types.GathererBoost;
import com.v4nden.bows.Boosts.Types.GiantBoost;
import com.v4nden.bows.Boosts.Types.IBoostType;
import com.v4nden.bows.Boosts.Types.QuiverBoost;
import com.v4nden.bows.Boosts.Types.RerollBoost;
import com.v4nden.bows.Boosts.Types.SecondChanceBoost;
import com.v4nden.bows.Boosts.Types.SmallBoost;
import com.v4nden.bows.Boosts.Types.ToughBoost;
import com.v4nden.bows.Boosts.Types.SwapBoost;

/** Стартовые бусты: один выдаётся каждому в начале игры (см. StarterBoosts). */
public enum StarterBoostTypes {

	SECONDCHANCE(new SecondChanceBoost()),
	SWAP(new SwapBoost()),
	REROLL(new RerollBoost()),
	SMALL(new SmallBoost()),
	QUIVER(new QuiverBoost()),
	TOUGH(new ToughBoost()),
	CHAINMAIL(new ChainmailBoost()),
	DOUBLEJUMP(new DoubleJumpBoost()),
	GATHERER(new GathererBoost()),
	ELUSIVE(new ElusiveBoost()),
	GIANT(new GiantBoost());

	private final IBoostType type;

	StarterBoostTypes(IBoostType type) {
		this.type = type;
	}

	public Boost getBoost(Player owner) {
		return type.create(owner);
	}
}
