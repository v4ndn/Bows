package com.v4nden.bows.Game;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.v4nden.bows.Boosts.Boost;
import com.v4nden.bows.Boosts.BoostIcons;
import com.v4nden.bows.Boosts.BoostTypes;
import com.v4nden.bows.Boosts.StarterBoostTypes;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

/**
 * Меню /info — диалог (1.21.6+): вкладки по редкости бустов и вкладка стартовых,
 * в каждой — сетка иконок бустов, при наведении — подсказка предмета буста.
 *
 * Элементы тела диалога идут только столбиком, поэтому сетка — это текст:
 * иконка буста — символ шрифта bows:boost_icons из ресурспака (BoostIcons) (битмап 16px из
 * текстуры предмета, см. resourcepack/gen_icon_font.py), пробел этого шрифта —
 * отступ 11px. Ряды через 3 строки по 9px — между ними те же 11px.
 */
public class InfoDialog {
    private static final int COLUMNS = 6;
    private static final int ROWS_PER_PAGE = 4;
    private static final int BOOSTS_PER_PAGE = COLUMNS * ROWS_PER_PAGE;
    private static final String ROW_BREAK = "\n\n\n";
    private static final int GRID_WIDTH = 200;
    private static final String TAB_SEPARATOR = "    ";
    // строка вкладок шире сетки
    private static final int TAB_ROW_EXTRA_WIDTH = 120;
    private static final int PAGE_BUTTON_WIDTH = 100;

    private static final String[] RARITY_COLORS = { "#fafafa", "#4aff65", "#4a89ff", "#834aff", "#e97218" };
    // вкладки 0..4 — редкость 1..5, вкладка 5 — стартовые бусты
    private static final int STARTER_TAB = 5;
    private static final int TAB_COUNT = 6;


    // кнопки живут, пока открыт диалог; новый диалог создаёт новые
    private static final ClickCallback.Options CALLBACK_OPTIONS = ClickCallback.Options.builder()
            .uses(ClickCallback.UNLIMITED_USES).lifetime(Duration.ofMinutes(10)).build();

    public static void open(Player player) {
        open(player, 0, 0);
    }

    private static void open(Player player, int tab, int page) {
        List<Boost> boosts = boostsOf(player, tab);
        int pages = Math.max(1, (boosts.size() + BOOSTS_PER_PAGE - 1) / BOOSTS_PER_PAGE);
        int current = Math.min(page, pages - 1);
        int from = current * BOOSTS_PER_PAGE;
        List<Boost> onPage = boosts.subList(from, Math.min(boosts.size(), from + BOOSTS_PER_PAGE));

        DialogBody grid = onPage.isEmpty()
                ? DialogBody.plainMessage(Component.text("Здесь пока нет бустов"))
                : DialogBody.plainMessage(grid(onPage), GRID_WIDTH);
        boosts.forEach(Boost::dispose);

        // кнопки диалога всегда под телом, поэтому вкладки — кликабельный текст над сеткой
        DialogBody tabs = DialogBody.plainMessage(tabs(tab), GRID_WIDTH + TAB_ROW_EXTRA_WIDTH);

        List<ActionButton> buttons = new ArrayList<>();
        if (pages > 1) {
            buttons.add(ActionButton.builder(Component.text(current > 0 ? "← Назад" : " "))
                    .width(PAGE_BUTTON_WIDTH).action(navigate(tab, Math.max(0, current - 1))).build());
            buttons.add(ActionButton.builder(Component.text("Страница " + (current + 1) + "/" + pages))
                    .width(PAGE_BUTTON_WIDTH).build());
            buttons.add(ActionButton.builder(Component.text(current < pages - 1 ? "Вперёд →" : " "))
                    .width(PAGE_BUTTON_WIDTH).action(navigate(tab, Math.min(pages - 1, current + 1))).build());
        }

        ActionButton close = ActionButton.builder(Component.text("Закрыть")).width(PAGE_BUTTON_WIDTH).build();
        Dialog dialog = Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(Component.text("Бусты"))
                        .canCloseWithEscape(true)
                        // кнопка без действия не закрывает диалог; с паузой так нельзя
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .body(List.of(tabs, grid))
                        .build())
                // multi_action нужна хотя бы одна кнопка, без страниц — только «Закрыть»
                .type(buttons.isEmpty()
                        ? DialogType.notice(close)
                        : DialogType.multiAction(buttons).columns(buttons.size()).exitAction(close).build()));
        player.showDialog(dialog);
    }

    private static Component grid(List<Boost> boosts) {
        // пустая строка сверху: иконка выше обычной строки и иначе вылезла бы за край
        TextComponent.Builder text = Component.text().content("\n");
        for (int i = 0; i < boosts.size(); i++) {
            if (i > 0) {
                text.append(i % COLUMNS == 0 ? Component.text(ROW_BREAK) : gap());
            }
            text.append(icon(boosts.get(i)));
        }
        return text.append(Component.text("\n")).build();
    }

    private static Component tabs(int selected) {
        TextComponent.Builder text = Component.text();
        for (int i = 0; i < TAB_COUNT; i++) {
            if (i > 0) {
                text.append(Component.text(TAB_SEPARATOR));
            }
            Component label = tabLabel(i);
            if (i == selected) {
                label = Component.text("[ ", NamedTextColor.WHITE).append(label)
                        .append(Component.text(" ]", NamedTextColor.WHITE));
            }
            final int target = i;
            text.append(label
                    .hoverEvent(Component.text("Открыть вкладку"))
                    .clickEvent(ClickEvent.callback(audience -> {
                        if (audience instanceof Player player) {
                            open(player, target, 0);
                        }
                    }, CALLBACK_OPTIONS)));
        }
        return text.build();
    }

    private static Component icon(Boost boost) {
        return BoostIcons.icon(boost.getId(), BoostIcons.LARGE_FONT).hoverEvent(preview(boost).asHoverEvent());
    }

    private static Component gap() {
        return Component.text(" ").font(BoostIcons.LARGE_FONT);
    }

    private static DialogAction navigate(int tab, int page) {
        return DialogAction.customClick((response, audience) -> {
            if (audience instanceof Player player) {
                open(player, tab, page);
            }
        }, CALLBACK_OPTIONS);
    }

    private static Component tabLabel(int tab) {
        if (tab == STARTER_TAB) {
            return Component.text("▼", TextColor.fromHexString(Boost.STARTER_COLOR));
        }
        return Component.text("★".repeat(tab + 1), TextColor.fromHexString(RARITY_COLORS[tab]));
    }

    // бусты вкладки; создаются только ради данных предмета, вызывающий их отключает
    private static List<Boost> boostsOf(Player player, int tab) {
        List<Boost> boosts = new ArrayList<>();
        if (tab == STARTER_TAB) {
            for (StarterBoostTypes type : StarterBoostTypes.values()) {
                boosts.add(type.getBoost(player));
            }
            return boosts;
        }
        for (BoostTypes type : BoostTypes.values()) {
            Boost boost = type.getBoost(player);
            if (boost.getRarity() == tab + 1) {
                boosts.add(boost);
            } else {
                boost.dispose();
            }
        }
        return boosts;
    }

    // копия предмета буста без uid: это не настоящий буст, активировать его нельзя
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
