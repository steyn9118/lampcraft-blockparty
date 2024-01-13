package steyn91.blockparty.Stats;

import org.bukkit.scheduler.BukkitRunnable;
import steyn91.blockparty.Blockparty;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings("ConstantConditions")
public class StatsManager {

    private static final Blockparty plugin = Blockparty.getPlugin();
    private static final List<PlayerStatsModel> cache = new ArrayList<>(100);

    public static PlayerStatsModel getStatsByName(String playerName){
        for (PlayerStatsModel stats : cache){
            if (stats.getPlayerName().equals(playerName)){
                return stats;
            }
        }
        PlayerStatsModel dbstats = Database.getPlayerStat(playerName);
        if (dbstats != null){
            cache.add(dbstats);
            return dbstats;
        }
        return null;
    }

    public static void updateGames(String playerName){
        if (getStatsByName(playerName) == null){
            cache.add(new PlayerStatsModel(playerName, 0, 0, 0, 0, new Date()));
        }

        PlayerStatsModel stats = getStatsByName(playerName);
        stats.setGames(stats.getGames() + 1);
    }

    public static void updateWins(String playerName){
        if (getStatsByName(playerName) == null){
            cache.add(new PlayerStatsModel(playerName, 0, 0, 0, 0, new Date()));
        }

        PlayerStatsModel stats = getStatsByName(playerName);
        stats.setWins(stats.getWins() + 1);
    }

    public static void updateRounds(String playerName){
        if (getStatsByName(playerName) == null){
            cache.add(new PlayerStatsModel(playerName, 0, 0, 0, 0, new Date()));
        }

        PlayerStatsModel stats = getStatsByName(playerName);
        stats.setRounds(stats.getRounds() + 1);
    }

    public static void updateBoosterCollected(String playerName){
        if (getStatsByName(playerName) == null){
            cache.add(new PlayerStatsModel(playerName, 0, 0, 0, 0, new Date()));
        }

        PlayerStatsModel stats = getStatsByName(playerName);
        stats.setBoosters(stats.getBoosters() + 1);
    }

    public static void removeStat(String playerName){
        if (getStatsByName(playerName) == null) return;
        cache.remove(getStatsByName(playerName));
        Database.removePlayerStat(playerName);
    }

    // TODO Очистка статов
    public void cleanUp(int inactiveDays){
        // Очистка старых рекордов, не находящихся в топах
    }

    public static void startSavingCycle(){
        BukkitRunnable saveToDBCycle = new BukkitRunnable() {
            @Override
            public void run() {
                saveAllToDB();
            }
        };
        saveToDBCycle.runTaskTimerAsynchronously(plugin, 20L * 60 * plugin.getConfig().getInt("DatabaseSavingTimer"), 20L * 60 * plugin.getConfig().getInt("DatabaseSavingTimer"));
    }

    public static void saveAllToDB(){
        for (PlayerStatsModel stat : cache){
            Database.updatePlayerStat(stat);
        }
        // Способ очистки кэша ПОЛНАЯ ХУЙНЯ!!! Очень нужно будет переписать на CacheEntry с датой обновления записи
        cache.clear();
    }

}
