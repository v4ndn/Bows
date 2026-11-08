package com.v4nden.bows.Game;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.v4nden.bows.Bows;
import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostIcons;
import com.v4nden.bows.Boosts.StarterBoostTypes;
import com.v4nden.bows.Boosts.StarterBoosts;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.event.player.PlayerClientLoadedWorldEvent;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;

/**
 * Выбор стартового буста в начале игры: сетка иконок стартовых бустов (как в /info),
 * последняя иконка — случайный буст. Клик по иконке выдаёт буст и закрывает окно,
 * Esc — играть без стартового буста. Один выбор на игру.
 *
 * Окно открывается, когда клиент загрузил мир игры: переход в другой мир
 * (у каждой игры свой) закрывает на клиенте все экраны, и окно, открытое
 * до телепорта, пропадало.
 */
public class StarterChoiceDialog {
    private static final int GRID_WIDTH = 200;
    private static final int COLUMNS = 5;
    // ряды через 3 строки по 9px: зазор между рядами как между иконками (см. InfoDialog)
    private static final String ROW_BREAK = "\n\n\n";

    private static final Random random = new Random();

    // кто уже выбрал в текущей игре
    private static final Set<UUID> chosen = new HashSet<>();
    // клики по окну прошлой игры не должны срабатывать
    private static long gameId = 0;
    // кому показать окно, как только клиент загрузит мир игры
    private static final Set<UUID> pending = new HashSet<>();
    // если события загрузки не будет — показать через столько тиков
    private static final long FALLBACK_TICKS = 20 * 10;

    public static void reset() {
        chosen.clear();
        pending.clear();
        gameId++;
    }

    /** Показать окно после загрузки мира (вызывать до телепорта в мир игры). */
    public static void showWhenLoaded(Player player) {
        pending.add(player.getUniqueId());
        long game = gameId;
        Bukkit.getScheduler().runTaskLater(Bows.instance, () -> showPending(player, game), FALLBACK_TICKS);
    }

    private static void showPending(Player player, long game) {
        if (game != gameId || !pending.remove(player.getUniqueId()) || !player.isOnline() || !Game.isRunning
                || !Game.gamePlayers.contains(player)) {
            return;
        }
        show(player);
    }

    public static class Handler implements Listener {
        @EventHandler
        public void onLoaded(PlayerClientLoadedWorldEvent event) {
            showPending(event.getPlayer(), gameId);
        }
    }

    public static void show(Player player) {
        long game = gameId;
        ClickCallback.Options once = ClickCallback.Options.builder()
                .uses(1).lifetime(Duration.ofMinutes(10)).build();
        StarterBoostTypes[] types = StarterBoostTypes.values();

        // пустая строка сверху: иконка выше обычной строки и иначе вылезла бы за край
        TextComponent.Builder grid = Component.text().content("\n");
        int index = 0;
        for (StarterBoostTypes type : types) {
            Boost boost = type.getBoost(player);
            separate(grid, index++);
            grid.append(BoostIcons.icon(boost.getId(), BoostIcons.LARGE_FONT)
                    .hoverEvent(preview(boost).asHoverEvent())
                    .clickEvent(ClickEvent.callback(audience -> {
                        if (audience instanceof Player p) {
                            give(p, type, game);
                        }
                    }, once)));
            boost.dispose();
        }
        separate(grid, index);
        grid.append(BoostIcons.icon("random", BoostIcons.LARGE_FONT)
                .clickEvent(ClickEvent.callback(audience -> {
                    if (audience instanceof Player p) {
                        give(p, types[random.nextInt(types.length)], game);
                    }
                }, once)));
        grid.append(Component.text("\n"));

        // у каждого типа диалога есть хотя бы одна кнопка — делаем её пустой и шириной в 1px
        ActionButton hidden = ActionButton.builder(Component.empty()).width(1).build();
        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(Component.text("Стартовый буст"))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .body(List.of(DialogBody.plainMessage(grid.build(), GRID_WIDTH)))
                        .build())
                .type(DialogType.notice(hidden)));
        player.showDialog(dialog);
    }

    // перед каждой иконкой, кроме первой: отступ или переход на новый ряд
    private static void separate(TextComponent.Builder grid, int index) {
        if (index == 0) {
            return;
        }
        grid.append(index % COLUMNS == 0 ? Component.text(ROW_BREAK) : Component.text(" ").font(BoostIcons.LARGE_FONT));
    }

    private static void give(Player player, StarterBoostTypes type, long game) {
        player.closeDialog();
        if (game != gameId || !Game.isRunning || !Game.gamePlayers.contains(player)
                || !chosen.add(player.getUniqueId())) {
            return;
        }
        StarterBoosts.give(player, type);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, SoundCategory.MASTER, 1, 1);
    }

    // копия предмета буста без uid — только для подсказки
    private static ItemStack preview(Boost boost) {
        ItemStack item = boost.getItem().clone();
        ItemMeta meta = item.getItemMeta();
        List<String> lore = new ArrayList<>(meta.getLore());
        lore.removeLast();
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
