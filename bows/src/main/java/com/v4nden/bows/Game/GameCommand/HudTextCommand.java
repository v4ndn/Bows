package com.v4nden.bows.Game.GameCommand;

import java.util.Arrays;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.v4nden.bows.Utils.HudText;

/** Тест HUD-текста: /hudtext <секунды> <текст...>, «@ник» — голова скина игрока */
public class HudTextCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player) || args.length < 2) {
            return false;
        }
        double seconds;
        try {
            seconds = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            return false;
        }
        HudText.show((Player) sender, String.join(" ", Arrays.copyOfRange(args, 1, args.length)),
                Math.max(0, seconds));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return args.length == 1 ? List.of("1", "3", "5") : List.of();
    }
}
