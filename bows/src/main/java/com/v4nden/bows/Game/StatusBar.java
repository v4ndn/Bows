package com.v4nden.bows.Game;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.v4nden.bows.Boosts.StarterBoosts;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * Элемент статуса на месте полоски опыта: круг бустов (сколько из 10), круг
 * времени до следующего буста, круг «насколько смотришь на центр карты» и
 * полоса движения барьера к финальному радиусу.
 *
 * Это строка actionbar из шрифта bows:hud (resourcepack/gen_hud.py) шириной
 * 182px, как полоска опыта; шейдер rendertype_text.vsh по цвету-метке опускает
 * её на место полоски. Саму полоску ресурспак прячет прозрачными текстурами.
 */
public class StatusBar {
    private static final Key FONT = Key.key("bows", "hud");
    // должна совпадать с STATUS_MARKER в шейдере
    private static final TextColor MARKER = TextColor.color(254, 7, 0);

    // коды и размеры — как в gen_hud.py
    private static final int BOOST_FIRST = 0xE300;
    private static final int TIMER_FIRST = 0xE310;
    private static final int TIMER_STATES = 16;
    private static final int DIRECTION_FIRST = 0xE340;
    private static final int DIRECTION_STATES = 16;
    private static final char FRAME = '\uE320';
    private static final int FILL_FIRST = 0xE321;
    private static final int[] FILL_PIECES = { 1, 2, 4, 8, 16, 32, 64, 128 };
    // кусок 1px со сдвигом узора — после куска 2px
    private static final char FILL_1_SHIFTED = '\uE329';
    private static final char SPACE_BACK_BAR = '\uE330';
    private static final char SPACE_BACK_1 = '\uE331';
    private static final int SPACE_POSITIVE_FIRST = 0xE332;
    private static final int[] SPACE_POSITIVE = { 1, 2, 4, 8, 16, 32, 64, 128 };
    // заливка внутри рамки полосы: 182 − 3 круга по 9px − рамка
    private static final int BAR_INNER = 153;

    public static void send(Player player) {
        int boosts = Math.min(Game.MAX_BOOSTS, StarterBoosts.regularBoostSlots(player).size());
        int timer = (int) Math.round(Game.boostProgress() * (TIMER_STATES - 1));
        int filled = (int) Math.round(Game.borderProgress() * BAR_INNER);
        int direction = (int) Math.round(facingCenter(player) * (DIRECTION_STATES - 1));

        StringBuilder text = new StringBuilder();
        text.append((char) (BOOST_FIRST + boosts));
        space(text, 1);
        text.append((char) (TIMER_FIRST + timer));
        space(text, 1);
        text.append((char) (DIRECTION_FIRST + direction));
        space(text, 1);
        // рамка, возврат внутрь неё, куски заливки встык, добивка до полной ширины
        text.append(FRAME).append(SPACE_BACK_BAR);
        int rest = filled;
        for (int i = FILL_PIECES.length - 1; i >= 0; i--) {
            while (rest >= FILL_PIECES[i]) {
                boolean shifted = FILL_PIECES[i] == 1 && filled % 4 >= 2;
                text.append(shifted ? FILL_1_SHIFTED : (char) (FILL_FIRST + i)).append(SPACE_BACK_1);
                rest -= FILL_PIECES[i];
            }
        }
        space(text, BAR_INNER + 1 - filled);

        // слои эффектов экрана — первыми (под элементом статуса), ширина у них нулевая
        player.sendActionBar(Component.text()
                .append(ScreenEffect.overlays(player))
                .append(Component.text(text.toString())
                        .font(FONT)
                        .color(MARKER)
                        .shadowColor(ShadowColor.none()))
                .build());
    }

    // 1 — смотрит точно на центр карты, 0 — точно от него
    private static double facingCenter(Player player) {
        Location location = player.getLocation();
        Vector toCenter = Game.gameLocation.toVector().subtract(location.toVector()).setY(0);
        Vector look = location.getDirection().setY(0);
        if (toCenter.lengthSquared() < 1e-6 || look.lengthSquared() < 1e-6) {
            return 1;
        }
        return (toCenter.normalize().dot(look.normalize()) + 1) / 2;
    }

    // положительный отступ из пробелов шрифта со степенями двойки
    private static void space(StringBuilder text, int pixels) {
        for (int i = SPACE_POSITIVE.length - 1; i >= 0; i--) {
            while (pixels >= SPACE_POSITIVE[i]) {
                text.append((char) (SPACE_POSITIVE_FIRST + i));
                pixels -= SPACE_POSITIVE[i];
            }
        }
    }
}
