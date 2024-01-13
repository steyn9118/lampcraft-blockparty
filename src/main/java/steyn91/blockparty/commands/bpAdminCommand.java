package steyn91.blockparty.commands;

import net.kyori.adventure.text.Component;
import steyn91.blockparty.Arena;
import steyn91.blockparty.Blockparty;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import steyn91.blockparty.Utils;

public class bpAdminCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (command.getName().equalsIgnoreCase("bpadmin")){

            if (args.length < 1 || sender instanceof ConsoleCommandSender || sender instanceof CommandBlock){
                return false;
            }

            Player p = (Player) sender;
            if (!p.hasPermission("blockparty.admin")){
                p.sendMessage("Нет прав");
                return false;
            }

            if (args[0].equalsIgnoreCase("reload")){
                Blockparty.LoadArenasFromConfig();
            }

            if (args[0].equals("debug") && args.length == 2){
                Arena arena = Utils.getArenaByID(Integer.parseInt(args[1]));
                assert arena != null;
                sender.sendMessage(Component.text(arena.getPlayers().toString() + " " + arena.getSpectators().toString() + " "));
            }

        }

        return false;
    }
}
