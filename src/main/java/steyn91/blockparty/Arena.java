package steyn91.blockparty;

import com.xxmicloxx.NoteBlockAPI.model.Playlist;
import com.xxmicloxx.NoteBlockAPI.model.RepeatMode;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import steyn91.blockparty.Stats.StatsManager;

import java.util.*;

public class Arena {

    // Структура: сет блоков -> roundtime -> очистка блоков -> cooldown -> повторение

    // Из конфига
    private final int id;
    private final int difficulty;
    private final int timeReduction;
    private final int cooldown;
    private final int initialRoundTime;
    private final int maxPlayers;
    private final int minPlayers;
    private final Location startLocation;
    private final Location hubLocation;
    private final int minY;
    private final List<Integer> mainFloor;
    private final List<Integer> endGameFloor;
    private final List<Material> blocksPalette;
    private final List<int[]> patterns;
    private final int lobbyTime = 30;
    private final BoundingBox spectatableArea;

    // Технические
    private final Blockparty plugin = Blockparty.getPlugin();
    private final RadioSongPlayer songPlayer;
    private final Component arenaFullMessage = Component.text("Арена уже заполнена или на ней идёт игра").color(NamedTextColor.RED);
    private final Component loseMessage = Component.text("Вы проиграли!").color(NamedTextColor.RED);
    private final Component winMessage = Component.text("Вы победили!").color(NamedTextColor.GREEN);
    private final Component difficultyIncreaseInform = Component.text("Сложность увеличена!").color(NamedTextColor.YELLOW);
    private final Component joinMessage;
    private final Component leaveMessage;
    private final Component blockName = Component.text("Встань на такой блок").color(NamedTextColor.YELLOW);

    // Динамические
    private final List<Player> spectators = new ArrayList<>();
    private final List<Player> players = new ArrayList<>();
    private final List<String> dyingOrder = new ArrayList<>();
    private Material currentBlock;
    private Material nextBlock;
    private int[] nextPattern;
    private int currentRoundTime;
    private int playedRounds;
    private boolean gameActive;
    private boolean timerStarted;
    private final BossBar bossBar = BossBar.bossBar(Component.text("Ждём игроков...").color(NamedTextColor.WHITE), 1, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);

    public List<Player> getPlayers(){
        return players;
    }
    public int getId(){
        return id;
    }
    public int getMinY(){
        return  minY;
    }
    public BoundingBox getSpectatableArea(){
        return spectatableArea;
    }
    public Location getStartLocation(){
        return startLocation;
    }

    public Arena(int id, String name, int difficulty, int timeReduction, int cooldown, int initialRoundTime,
                 int maxPlayers, int minPlayers, Location startLocation, Location hubLocation,
                 int minY, List<Integer> mainFloor, List<Integer> endGameFloor, List<int[]> patterns, List<Material> blocksPalette,
                 Playlist playlist, BoundingBox spectatableArea){
        this.id = id;

        this.difficulty = difficulty;
        this.timeReduction = timeReduction;
        this.cooldown = cooldown;
        this.initialRoundTime = initialRoundTime;

        this.maxPlayers = maxPlayers;
        this.minPlayers = minPlayers;
        this.startLocation = startLocation;
        this.hubLocation = hubLocation;

        this.minY = minY;
        this.mainFloor = mainFloor;
        this.endGameFloor = endGameFloor;
        this.patterns = patterns;
        this.blocksPalette = blocksPalette;

        this.songPlayer = new RadioSongPlayer(playlist);
        this.songPlayer.setRandom(true);
        this.songPlayer.setRepeatMode(RepeatMode.ONE);
        this.songPlayer.setCategory(com.xxmicloxx.NoteBlockAPI.model.SoundCategory.RECORDS);

        this.spectatableArea = spectatableArea;

        joinMessage = Component.text("Вы присоединились к арене " + name).color(NamedTextColor.GRAY);
        leaveMessage = Component.text("Вы покинули арену " + name).color(NamedTextColor.GRAY);
    }

    public void join(Player p){
        if (gameActive || players.size() == maxPlayers){
            p.sendMessage(arenaFullMessage);
            return;
        }

        players.add(p);
        p.sendMessage(joinMessage);
        p.teleport(startLocation);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi kit bp " + p.getName());
        p.showBossBar(bossBar);

        if (players.size() < minPlayers || timerStarted) {
            return;
        }

        startLobbyTimer();
    }

    public void leave(Player p){
        if (!gameActive || !players.contains(p)){
            p.sendMessage(leaveMessage);
            clearPlayer(p);
            return;
        }
        clearPlayer(p);

        for (Player player : players){
            player.sendMessage(Component.text(p.getName() + " выбывает. Осталось игроков: " + players.size()).color(NamedTextColor.RED));
        }
    }

    public void death(Player player){
        if (!players.contains(player)) return;
        becameSpectator(player);
        dyingOrder.add(player.getName());
        for (Player p : players){
            player.sendMessage(Component.text(p.getName() + " выбывает. Осталось игроков: " + players.size()).color(NamedTextColor.RED));
        }
    }

    // Механика игры

    private void startLobbyTimer(){
        timerStarted = true;
        bossBar.color(BossBar.Color.GREEN);
        bossBar.name(Component.text("Игра скоро начнётся!").color(NamedTextColor.GREEN));

        BukkitRunnable lobbyCountdown = new BukkitRunnable() {

            int lobbyTimer = lobbyTime;

            @Override
            public void run() {

                if (players.size() < minPlayers){
                    timerStarted = false;
                    Utils.resetExpTimer(players, true);
                    this.cancel();
                }

                lobbyTimer -= 1;
                Utils.setExpTimer(players, lobbyTime, lobbyTimer, (lobbyTimer < 5));

                if (lobbyTimer == 0){
                    startGame();
                    timerStarted = false;
                    this.cancel();
                }
            }
        };
        lobbyCountdown.runTaskTimer(Blockparty.getPlugin(), 0, 20);
    }

    private void startGame(){

        gameActive = true;

        bossBar.color(BossBar.Color.RED);
        bossBar.name(Component.text("Текущая сложность: " + currentRoundTime + " секунд").color(NamedTextColor.WHITE));

        songPlayer.playNextSong();
        songPlayer.setPlaying(true);

        nextBlock = Utils.getRandomBlock(blocksPalette, null);
        nextPattern = Utils.getRandomPattern(patterns, null);
        currentRoundTime = initialRoundTime;

        for (Player p : players) {
            StatsManager.updateGames(p.getName());
            p.teleport(startLocation);
            songPlayer.addPlayer(p);
            p.sendActionBar(Component.text("Сейчас играет: " + songPlayer.getSong().getTitle() + " - " + songPlayer.getSong().getAuthor()));
        }

        currentRoundTime = 0;
        playedRounds = 0;

        roundStart();
        BukkitRunnable endgameWatchdog = new BukkitRunnable() {
            @Override
            public void run() {
                if (players.size() > 1) return;

                Player winner = players.get(0);
                winner.showTitle(Title.title(winMessage, Component.text("")));
                for (Player spectator : spectators){
                    spectator.sendMessage(Component.text("Победил игрок " + winner.getName()).color(NamedTextColor.YELLOW));
                }
                dyingOrder.add(winner.getName());
                StatsManager.updateWins(winner.getName());
                clearPlayer(winner);

                showTop();
                stopGame();
            }
        };
        endgameWatchdog.runTaskTimerAsynchronously(plugin, 0, 5);
    }

    private void stopGame(){
        bossBar.color(BossBar.Color.WHITE);
        bossBar.name(Component.text("Ждём игроков...").color(NamedTextColor.WHITE));

        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "clone " + endGameFloor.get(0) + " " + endGameFloor.get(1) + " " + endGameFloor.get(2) + " " + endGameFloor.get(3) + " " + endGameFloor.get(4) + " " + endGameFloor.get(5) + " " + mainFloor.get(0) + " " + mainFloor.get(1) + " " + mainFloor.get(2));

        songPlayer.setPlaying(false);
        players.clear();
        spectators.clear();

        dyingOrder.clear();
        gameActive = false;
    }

    // Механика раундов

    private void roundStart(){
        for (Player player : players){
            StatsManager.updateRounds(player.getName());
            ItemStack itemStack = new ItemStack(currentBlock, 1);
            itemStack.getItemMeta().displayName(blockName);
            Utils.fillHotbar(player, itemStack);
        }

        songPlayer.setPlaying(true);

        int[] currentPattern = Utils.getRandomPattern(patterns, nextPattern);
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "clone " + currentPattern[0] + " " + currentPattern[1] + " " + currentPattern[2] + " " + currentPattern[3] + " " + currentPattern[4] + " " + currentPattern[5] + " " + mainFloor.get(0) + " " + mainFloor.get(1) + " " + mainFloor.get(2));

        BukkitRunnable roundTimer = new BukkitRunnable() {
            int count = currentRoundTime;

            @Override
            public void run() {
                if (!gameActive) this.cancel();

                Utils.setExpTimer(players, count, currentRoundTime, true);

                if (count == 0){
                    for (Material material : blocksPalette){
                        if (material.equals(currentBlock)){
                            continue;
                        }
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "fill " + mainFloor.get(0) + " " + mainFloor.get(1) + " " + mainFloor.get(2) + " " + mainFloor.get(3) + " " + mainFloor.get(4) + " " + mainFloor.get(5) + " minecraft:air replace minecraft:" + material.toString().toLowerCase(Locale.ROOT));
                    }
                    songPlayer.setPlaying(false);
                    playedRounds++;
                    cooldownStart();
                    this.cancel();
                }
                count -= 1;
            }
        };
        roundTimer.runTaskTimer(plugin, 20 * 2, 20);
    }

    private void cooldownStart(){
        for (Player player : players){
            player.getInventory().remove(currentBlock);
        }

        BukkitRunnable cooldownTimer = new BukkitRunnable() {
            int count = cooldown;

            @Override
            public void run() {
                if (!gameActive) this.cancel();

                count -= 1;

                Utils.setExpTimer(players, count, cooldown, false);

                if (count == 0){
                    if (playedRounds % difficulty == 0 && currentRoundTime != 1){
                        currentRoundTime -= 1;
                    }
                    currentBlock = nextBlock;
                    nextBlock = Utils.getRandomBlock(blocksPalette, nextBlock);
                    roundStart();
                    this.cancel();
                }
            }
        };
        cooldownTimer.runTaskTimer(plugin, 0, 20);
    }

    // Вспомогательные методы

    private void increaseDifficulty(){
        if (currentRoundTime == 0) return;
        bossBar.name(Component.text("Текущая сложность: " + currentRoundTime + " секунд").color(NamedTextColor.WHITE));
        for (Player player : players){
            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 100, 1);
            player.sendActionBar(difficultyIncreaseInform);
        }

        currentRoundTime -= timeReduction;
    }

    private void becameSpectator(Player player){
        players.remove(player);
        spectators.add(player);
        player.showTitle(Title.title(loseMessage, Component.text("")));
        Utils.hidePlayer(players, player);
        Utils.showPlayersToPlayer(spectators, player);
        player.setFlying(true);
    }

    private void clearPlayer(Player player){
        if (players.contains(player)){
            players.remove(player);
            Utils.showPlayersToPlayer(spectators, player);
        }
        else {
            spectators.remove(player);
            player.setFlying(false);
            Utils.showPlayerToPlayers(players, player);
        }

        player.hideBossBar(bossBar);
        player.getInventory().clear();
        player.teleport(hubLocation);
        Utils.resetExpTimer(players, false);
        songPlayer.removePlayer(player);
    }

    public void boosterPickup(Player player){
        player.playSound(player, Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, 100, 2);
        StatsManager.updateBoosterCollected(player.getName());
        switch (new Random().nextInt(3)){
            case (0):
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 15, 1));
                player.sendActionBar(Component.text("Ты подобрал бустер СКОРОСТИ!").color(NamedTextColor.GREEN));
            case (1):
                player.sendActionBar(Component.text("Ты подобрал бустер ПОДСКАЗКУ!").color(NamedTextColor.GREEN));
                ItemStack item = new ItemStack(nextBlock);
                item.getItemMeta().displayName(Component.text("Блок в следующем раунде").color(NamedTextColor.YELLOW));
                player.getInventory().setItem(0, item);
            case (2):
                player.sendActionBar(Component.text("Ты подобрал бустер ПЛАТФОРМУ!").color(NamedTextColor.GREEN));
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "fill " + (player.getLocation().getBlockX() - 1) + " " + mainFloor.get(1) + " " + (player.getLocation().getBlockZ() - 1) + " " + (player.getLocation().getBlockX() + 1) + " " + mainFloor.get(4) + " " + (player.getLocation().getBlockZ() + 1) + " minecraft:" + currentBlock.toString().toLowerCase(Locale.ROOT));
        }
    }

    private void showTop(){
        Collections.reverse(dyingOrder);
        Component topLine = Component.text("+---- ТОП ----+");
        Component bottomLine = Component.text("+---- --- ----+");
        List<Component> places = new ArrayList<>();
        for (int i = 0; i < dyingOrder.size(); i++){
            if (i == 3) break;
            places.add(Component.text((i + 1) + ". "+ dyingOrder.get(i)).color(NamedTextColor.WHITE));
        }
        for (Player player : spectators){
            player.sendMessage(topLine);
            for (Component place : places){
                player.sendMessage(place);
            }
            player.sendMessage(bottomLine);
        }
    }

}