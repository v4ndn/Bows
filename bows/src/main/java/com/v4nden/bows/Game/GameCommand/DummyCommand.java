package com.v4nden.bows.Game.GameCommand;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import com.v4nden.bows.BowsUtils;
import com.v4nden.bows.Game.Dummies;
import com.v4nden.bows.Game.Game;

public class DummyCommand implements CommandExecutor, TabCompleter {
    private static final int MAX_AT_ONCE = 20;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }
        Player p = (Player) sender;

        if (!Game.isRunning || !Game.isTest) {
            BowsUtils.broadcastSystemMessage(p, "Куклы доступны только в тестовой игре: /startgame test");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("clear")) {
            int count = Dummies.alive().size();
            Dummies.clear();
            BowsUtils.broadcastSystemMessage(p, "Удалено кукол: " + count);
            return true;
        }

        int count = 1;
        if (args.length > 0) {
            try {
                count = Math.max(1, Math.min(MAX_AT_ONCE, Integer.parseInt(args[0])));
            } catch (NumberFormatException e) {
                return false;
            }
        }
        for (int i = 0; i < count; i++) {
            Dummies.spawn(p.getLocation());
        }
        BowsUtils.broadcastSystemMessage(p, "Создано кукол: " + count);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        return args.length == 1 ? List.of("1", "5", "clear") : List.of();
    }
}
