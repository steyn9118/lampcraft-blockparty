package steyn91.blockparty;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class Utils {

    private final static Blockparty plugin = Blockparty.getPlugin();
    private final static Random random = new Random();

    public static Arena getArenaOfPlayer(Player player){
        for (Arena arena : plugin.getArenas()){
            if (arena.getPlayers().contains(player)) return arena;
        }
        return null;
    }

    public static Arena getArenaByID(int id){
        for (Arena arena : plugin.getArenas()){
            if (arena.getId() == id) return arena;
        }
        return null;
    }

    public static void setExpTimer(List<Player> players, int current, int max, boolean notify){
        for (Player player : players){
            player.setLevel(current);
            if (max == 0) player.setExp(1);
            else player.setExp((float) current / max);
            if (notify) player.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 100, 1);
        }
    }

    public static void resetExpTimer(List<Player> players, boolean toFull){
        if (toFull) setExpTimer(players, 0, 0, false);
        else setExpTimer(players, 0, 1, false);
    }

    public static void hidePlayer(List<Player> players, Player hidingPlayer){
        for (Player player : players){
            player.hidePlayer(plugin, hidingPlayer);
        }
    }

    public static void showPlayerToPlayers(List<Player> players, Player showingPlayer){
        for (Player player : players){
            player.showPlayer(plugin, showingPlayer);
        }
    }

    public static void showPlayersToPlayer(List<Player> showingPlayers, Player player){
        for (Player showingPlayer : showingPlayers){
            player.showPlayer(plugin, showingPlayer);
        }
    }

    public static Material getRandomBlock(List<Material> blocks, @Nullable Material nextBlock){
        if (nextBlock == null) return blocks.get(random.nextInt(blocks.size()));
        while (true){
            Material randomBlock = blocks.get(random.nextInt(blocks.size()));
            if (randomBlock != nextBlock) return randomBlock;
        }
    }

    public static int[] getRandomPattern(List<int[]> patterns, int[] nextPattern){
        if (nextPattern == null) return patterns.get(random.nextInt(patterns.size()));
        while (true){
            int[] randomPattern = patterns.get(random.nextInt(patterns.size()));
            if (nextPattern != randomPattern) return randomPattern;
        }
    }

    public static void fillHotbar(Player player, ItemStack item){
        for (int i = 0; i < 9; i++){
            player.getInventory().setItem(i, item);
        }
    }
}
