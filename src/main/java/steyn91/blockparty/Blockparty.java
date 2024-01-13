package steyn91.blockparty;

import com.xxmicloxx.NoteBlockAPI.model.Playlist;
import com.xxmicloxx.NoteBlockAPI.model.Song;
import com.xxmicloxx.NoteBlockAPI.utils.NBSDecoder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BoundingBox;
import steyn91.blockparty.Stats.Database;
import steyn91.blockparty.Stats.PlaceholderManager;
import steyn91.blockparty.Stats.StatsManager;
import steyn91.blockparty.commands.bpAdminCommand;
import steyn91.blockparty.commands.bpCommand;
import steyn91.blockparty.listeners.playerListeners;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class Blockparty extends JavaPlugin {

    private static Blockparty plugin;
    public static List<Arena> arenas = new ArrayList<>();

    public static Blockparty getPlugin(){
            return plugin;
        }
    public List<Arena> getArenas(){
            return arenas;
        }

    @Override
    public void onEnable() {
        plugin = this;

        getCommand("bp").setExecutor(new bpCommand());
        getCommand("bpadmin").setExecutor(new bpAdminCommand());
        Bukkit.getServer().getPluginManager().registerEvents(new playerListeners(), this);
        new PlaceholderManager().register();

        getConfig().options().copyDefaults();
        saveDefaultConfig();

        Database.initDatabase();
        StatsManager.startSavingCycle();

        LoadArenasFromConfig();
    }

    @Override
    public void onDisable(){
        StatsManager.saveAllToDB();
    }

    public static void LoadArenasFromConfig(){
        arenas.clear();
        File arenasFolder = new File(Blockparty.getPlugin().getDataFolder() + "/Arenas");
        if (!arenasFolder.exists()){
            arenasFolder.mkdir();
        }
        File[] arenasFiles = arenasFolder.listFiles();

        if (arenasFiles.length == 0){
            return;
        }

        for (File file : arenasFiles){

            YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);

            // Палитра блоков
            List<Material> blocks = new ArrayList();
            for (String material : configuration.getStringList("blocks")){
                blocks.add(Material.valueOf(material));
            }

            // Полы
            ArrayList<int[]> patterns = new ArrayList<>();
            List<Integer> corners = configuration.getIntegerList("floorsCorners");
            for (int y = configuration.getInt("floorsYMin"); y <= configuration.getInt("floorsYMax"); y++){
                int[] pattern = new int[6];
                pattern[0] = corners.get(0);
                pattern[1] = y;
                pattern[2] = corners.get(1);
                pattern[3] = corners.get(2);
                pattern[4] = y;
                pattern[5] = corners.get(3);
                patterns.add(pattern);
            }

            // Музыка
            File soundFolder = new File(Blockparty.getPlugin().getDataFolder() + "/Songs");
            File[] soundFiles = soundFolder.listFiles();
            List<Song> songs = new ArrayList<>();
            for (File f : soundFiles){
                Song song = NBSDecoder.parse(f);
                songs.add(song);
            }
            Playlist playlist = new Playlist(songs.get(0));
            for (int i = 1; i < songs.size(); i++){
                playlist.add(songs.get(i));
            }

            // Область для наблюдателей
            List<Integer> spectCorners = configuration.getIntegerList("spectatorsBox");
            BoundingBox spectBox = new BoundingBox(spectCorners.get(0), spectCorners.get(1), spectCorners.get(2), spectCorners.get(3), spectCorners.get(4), spectCorners.get(5));

            arenas.add(new Arena(configuration.getInt("id"), configuration.getString("name"), configuration.getInt("lobbyTime"),
                    configuration.getInt("difficulty"), configuration.getInt("timeReduction"), configuration.getInt("cooldown"), configuration.getInt("initialRoundTime"),
                    configuration.getInt("maxPlayers"), configuration.getInt("minPlayers"), configuration.getLocation("startLocation"), configuration.getLocation("hubLocation"),
                    configuration.getInt("minY"), configuration.getIntegerList("mainFloor"), configuration.getIntegerList("endGameFloor"), patterns,  blocks,
                    playlist, spectBox, configuration.getIntegerList("boosterRange")));
        }
    }
}