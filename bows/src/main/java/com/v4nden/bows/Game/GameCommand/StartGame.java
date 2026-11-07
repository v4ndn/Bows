package com.v4nden.bows.Game.GameCommand;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.v4nden.bows.BowsUtils;
import com.v4nden.bows.Game.GameStarter;

public class StartGame implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player p = (Player) sender;

            // случайная точка мира, проверка местности и прегенерация карты;
            // test — то же самое, но только для вызвавшего; число — размер команды
            // после размера команды — ники, которые будут одной командой
            boolean test = false;
            int teamSize = 0;
            List<String> fixedTeam = new ArrayList<>();
            for (String arg : args) {
                if (teamSize == 0 && arg.equalsIgnoreCase("test")) {
                    test = true;
                } else if (teamSize == 0) {
                    try {
                        teamSize = Math.max(1, Integer.parseInt(arg));
                    } catch (NumberFormatException e) {
                        return false;
                    }
                } else {
                    if (Bukkit.getPlayerExact(arg) == null) {
                        BowsUtils.broadcastSystemMessage(p, "Игрока " + arg + " нет на сервере");
                        return true;
                    }
                    fixedTeam.add(arg);
                }
            }
            GameStarter.start(test ? p : null, teamSize, fixedTeam);
        }

        return true;
    }

}
