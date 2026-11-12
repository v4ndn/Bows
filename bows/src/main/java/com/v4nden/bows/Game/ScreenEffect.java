package com.v4nden.bows.Game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import com.v4nden.bows.Boosts.Types.RageBoost;
import com.v4nden.bows.Boosts.Types.TimestopBoost;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * Полноэкранные эффекты: таймстоп, рейдж, финал, мало хп, невидимость.
 *
 * Каждый — символ 1×1 из шрифта bows:hud с цветом (252, номер эффекта, сила);
 * шейдер rendertype_text.vsh растягивает его на весь экран, rendertype_text.fsh
 * рисует сам эффект. Символы едут в строке StatusBar (actionbar, раз в 2 тика)
 * с нулевой шириной, так что элемент статуса не сдвигается. Сила плавно идёт
 * к целевой, поэтому эффекты появляются и гаснут без рывков.
 */
public enum ScreenEffect {
    // номера — как в шейдере
    TIMESTOP(1, 0.08),
    RAGE(2, 0.25),
    FINAL(3, 0.05),
    LOW_HEALTH(4, 0.1),
    INVISIBILITY(5, 0.1);

    private static final Key FONT = Key.key("bows", "hud");
    private static final int MARKER_RED = 252;
    // символ слоя и возврат на его ширину — как в resourcepack/gen_hud.py
    private static final String OVERLAY = "";
    private static final double LOW_HEALTH_FROM = 6;

    // игрок -> текущая сила каждого эффекта
    private static final Map<UUID, double[]> strengths = new HashMap<>();

    private final int id;
    // насколько сила меняется за обновление (раз в 2 тика)
    private final double step;

    ScreenEffect(int id, double step) {
        this.id = id;
        this.step = step;
    }

    /** Символы слоёв для строки actionbar игрока; пусто, если эффектов нет. */
    public static Component overlays(Player player) {
        double[] current = strengths.computeIfAbsent(player.getUniqueId(), uuid -> new double[values().length]);
        TextComponent.Builder builder = Component.text();
        for (ScreenEffect effect : values()) {
            double target = effect.target(player);
            double value = current[effect.ordinal()];
            value = value < target ? Math.min(target, value + effect.step) : Math.max(target, value - effect.step);
            current[effect.ordinal()] = value;
            int strength = (int) Math.round(value * 255);
            if (strength > 0) {
                builder.append(Component.text(OVERLAY)
                        .font(FONT)
                        .color(TextColor.color(MARKER_RED, effect.id, strength))
                        .shadowColor(ShadowColor.none()));
            }
        }
        return builder.build();
    }

    public static void reset() {
        strengths.clear();
    }

    private double target(Player player) {
        switch (this) {
            case TIMESTOP:
                return TimestopBoost.isFrozen(player) ? 1 : 0;
            case RAGE:
                return RageBoost.isRaging(player) ? 1 : 0;
            case FINAL:
                return Game.finalProgress() < 0 ? 0 : 0.5 + 0.5 * Game.finalProgress();
            case LOW_HEALTH:
                double health = player.getHealth();
                return health >= LOW_HEALTH_FROM ? 0 : 0.4 + 0.6 * (1 - health / LOW_HEALTH_FROM);
            case INVISIBILITY:
                return player.hasPotionEffect(PotionEffectType.INVISIBILITY) ? 1 : 0;
            default:
                return 0;
        }
    }
}
