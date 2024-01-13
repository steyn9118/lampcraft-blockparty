package steyn91.blockparty.commands;

import net.kyori.adventure.text.Component;
import steyn91.blockparty.Arena;
import steyn91.blockparty.Blockparty;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import steyn91.blockparty.Utils;

import java.util.ArrayList;
import java.util.List;

public class bpCommand implements CommandExecutor {

    private final Blockparty plugin = Blockparty.getPlugin();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (command.getName().equalsIgnoreCase("bp")){
            switch (args.length){
                case (0):
                    return false;
                case (1):
                    // LEAVE
                    if (args[0].equalsIgnoreCase("leave")){
                        Player p = (Player) sender;
                        Utils.getArenaOfPlayer(p).leave(p);
                    }
                    if (!sender.hasPermission("blockparty.list")) return false;
                    // LIST
                    else if (args[0].equalsIgnoreCase("list")){
                        Player p = (Player) sender;
                        List<Arena> arenas = plugin.getArenas();
                        p.sendMessage(ChatColor.YELLOW + "Список доступных арен:");
                        for (Arena arena : arenas) {
                            p.sendMessage(Component.text("ID " + arena.getId()));
                        }
                    }
                case (3):
                    // JOIN
                    if (args[0].equalsIgnoreCase("join" ) && sender.hasPermission("blockparty.join")){
                        Player p = Bukkit.getPlayer(args[1]);
                        assert p != null;
                        Utils.getArenaByID(Integer.parseInt(args[2])).join(p);
                    }
            }
        }
        return false;
    }
}
