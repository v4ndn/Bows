package com.v4nden.bows.Boosts;

import org.bukkit.entity.Player;

import com.v4nden.bows.Boosts.Types.ArrowgunBoost;
import com.v4nden.bows.Boosts.Types.BlindnessAuraBoost;
import com.v4nden.bows.Boosts.Types.BreakBoost;
import com.v4nden.bows.Boosts.Types.BridgeBoost;
import com.v4nden.bows.Boosts.Types.DashBoost;
import com.v4nden.bows.Boosts.Types.DrillBoost;
import com.v4nden.bows.Boosts.Types.GlassBoxBoost;
import com.v4nden.bows.Boosts.Types.GlassPilarBoost;
import com.v4nden.bows.Boosts.Types.GlassWallBoost;
import com.v4nden.bows.Boosts.Types.GlowAllBoost;
import com.v4nden.bows.Boosts.Types.GlowClosestBoost;
import com.v4nden.bows.Boosts.Types.GlowRandomBoost;
import com.v4nden.bows.Boosts.Types.FireArrowsBoost;
import com.v4nden.bows.Boosts.Types.FreezeBoost;
import com.v4nden.bows.Boosts.Types.IBoostType;
import com.v4nden.bows.Boosts.Types.GolemBombBoost;
import com.v4nden.bows.Boosts.Types.NoRegenAuraBoost;
import com.v4nden.bows.Boosts.Types.VampirismBoost;
import com.v4nden.bows.Boosts.Types.ShrinkBoost;
import com.v4nden.bows.Boosts.Types.FlightBoost;
import com.v4nden.bows.Boosts.Types.FullDrawBoost;
import com.v4nden.bows.Boosts.Types.GlobalLevitationBoost;
import com.v4nden.bows.Boosts.Types.LevitationBoost;
import com.v4nden.bows.Boosts.Types.LevitationAuraBoost;
import com.v4nden.bows.Boosts.Types.SmiteBoost;
import com.v4nden.bows.Boosts.Types.ThunderstormBoost;
import com.v4nden.bows.Boosts.Types.LightningBoost;
import com.v4nden.bows.Boosts.Types.InvisibilityBoost;
import com.v4nden.bows.Boosts.Types.LocatorBoost;
import com.v4nden.bows.Boosts.Types.MeteorBoost;
import com.v4nden.bows.Boosts.Types.MeteorShowerBoost;
import com.v4nden.bows.Boosts.Types.RageBoost;
import com.v4nden.bows.Boosts.Types.RainBoost;
import com.v4nden.bows.Boosts.Types.RegenerationBoost;
import com.v4nden.bows.Boosts.Types.RiderBoost;
import com.v4nden.bows.Boosts.Types.SlownessAuraBoost;
import com.v4nden.bows.Boosts.Types.SpeedBoost;
import com.v4nden.bows.Boosts.Types.TeleportClosestBoost;
import com.v4nden.bows.Boosts.Types.TeleportRandomBoost;
import com.v4nden.bows.Boosts.Types.TimestopBoost;
import com.v4nden.bows.Boosts.Types.TntBoost;
import com.v4nden.bows.Boosts.Types.WindBoost;

public enum BoostTypes {

	DRILL(new DrillBoost()),
	ARROWGUN(new ArrowgunBoost()),
	RIDER(new RiderBoost()),
	TELEPORTRANDOM(new TeleportRandomBoost()),
	TELEPORTCLOSEST(new TeleportClosestBoost()),
	GLOWCLOSEST(new GlowClosestBoost()),
	LOCATOR(new LocatorBoost()),
	GLOWRANDOM(new GlowRandomBoost()),
	GLOWALL(new GlowAllBoost()),
	TIMESTOP(new TimestopBoost()),
	BREAK(new BreakBoost()),
	TNT(new TntBoost()),
	METEOR(new MeteorBoost()),
	GLASSPILAR(new GlassPilarBoost()),
	GLASSBOX(new GlassBoxBoost()),
	BLINDNESSAURA(new BlindnessAuraBoost()),
	SLOWNESSAURA(new SlownessAuraBoost()),
	DASH(new DashBoost()),
	WIND(new WindBoost()),
	SPEED(new SpeedBoost()),
	BRIDGE(new BridgeBoost()),
	RAIN(new RainBoost()),
	INVISIBILITY(new InvisibilityBoost()),
	REGENERATION(new RegenerationBoost()),
	LIGHTNING(new LightningBoost()),
	THUNDERSTORM(new ThunderstormBoost()),
	SMITE(new SmiteBoost()),
	FIREARROWS(new FireArrowsBoost()),
	LEVITATIONAURA(new LevitationAuraBoost()),
	LEVITATION(new LevitationBoost()),
	GLOBALLEVITATION(new GlobalLevitationBoost()),
	FLIGHT(new FlightBoost()),
	SHRINK(new ShrinkBoost()),
	VAMPIRISM(new VampirismBoost()),
	NOREGENAURA(new NoRegenAuraBoost()),
	GLASSWALL(new GlassWallBoost()),
	FULLDRAW(new FullDrawBoost()),
	GOLEMBOMB(new GolemBombBoost()),
	FREEZE(new FreezeBoost()),
	METEORSHOWER(new MeteorShowerBoost()),
	RAGE(new RageBoost());

	private final IBoostType type;

	BoostTypes(IBoostType type) {
		this.type = type;
	}

	public Boost getBoost(Player owner) {
		return type.create(owner);
	}
}
