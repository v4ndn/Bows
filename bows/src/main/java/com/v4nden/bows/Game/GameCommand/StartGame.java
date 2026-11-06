package com.v4nden.bows.Game.GameCommand;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.v4nden.bows.Game.GameStarter;

public class StartGame implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;

            // случайная точка мира, проверка местности и прегенерация карты;
            // тестовая игра — то же самое, но только для вызвавшего
            boolean test = args.length > 0 && args[0].equalsIgnoreCase("test");
            GameStarter.start(test ? p : null);
        }

        return true;
    }

}
