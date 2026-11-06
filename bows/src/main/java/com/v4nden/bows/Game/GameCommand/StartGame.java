package com.v4nden.bows.Game.GameCommand;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.v4nden.bows.BowsUtils;
import com.v4nden.bows.Game.Game;

public class StartGame implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;

            if (args.length > 0 && args[0].equalsIgnoreCase("test")) {
                Game.startTestGame(p);
                BowsUtils.broadcastSystemMessage(p, "Тестовая игра запущена, остановить: /stopgame");
            } else {
                Game.startGame(p.getLocation());
            }
        }

        return true;
    }

}
