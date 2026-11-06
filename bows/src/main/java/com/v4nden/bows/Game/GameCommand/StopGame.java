package com.v4nden.bows.Game.GameCommand;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.v4nden.bows.Game.Game;

public class StopGame implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!Game.isRunning) {
            sender.sendMessage("Игра не запущена");
            return true;
        }

        Game.stopGame();
        sender.sendMessage("Игра остановлена");
        return true;
    }

}
