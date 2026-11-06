package com.v4nden.bows.Boosts;

import org.bukkit.entity.Player;

import com.v4nden.bows.Boosts.Types.DoubleJumpBoost;
import com.v4nden.bows.Boosts.Types.HasteBoost;
import com.v4nden.bows.Boosts.Types.IBoostType;
import com.v4nden.bows.Boosts.Types.IronHelmetBoost;
import com.v4nden.bows.Boosts.Types.QuiverBoost;
import com.v4nden.bows.Boosts.Types.RerollBoost;
import com.v4nden.bows.Boosts.Types.SecondChanceBoost;
import com.v4nden.bows.Boosts.Types.SmallBoost;
import com.v4nden.bows.Boosts.Types.ToughBoost;
import com.v4nden.bows.Boosts.Types.TreecapitatorBoost;
import com.v4nden.bows.Boosts.Types.SwapBoost;

/** Стартовые бусты: один выдаётся каждому в начале игры (см. StarterBoosts). */
public enum StarterBoostTypes {

	SECONDCHANCE(new SecondChanceBoost()),
	SWAP(new SwapBoost()),
	REROLL(new RerollBoost()),
	SMALL(new SmallBoost()),
	QUIVER(new QuiverBoost()),
	HASTE(new HasteBoost()),
	TOUGH(new ToughBoost()),
	IRONHELMET(new IronHelmetBoost()),
	DOUBLEJUMP(new DoubleJumpBoost()),
	TREECAPITATOR(new TreecapitatorBoost());

	private final IBoostType type;

	StarterBoostTypes(IBoostType type) {
		this.type = type;
	}

	public Boost getBoost(Player owner) {
		return type.create(owner);
	}
}
