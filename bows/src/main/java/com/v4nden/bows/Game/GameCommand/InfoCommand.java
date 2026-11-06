package com.v4nden.bows.Game.GameCommand;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.v4nden.bows.Game.InfoDialog;

/** /info — меню с описаниями всех бустов */
public class InfoCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            InfoDialog.open(player);
        }
        return true;
    }
}
