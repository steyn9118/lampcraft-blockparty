package steyn91.blockparty;

import com.xxmicloxx.NoteBlockAPI.model.Playlist;
import com.xxmicloxx.NoteBlockAPI.model.RepeatMode;
import com.xxmicloxx.NoteBlockAPI.model.playmode.MonoStereoMode;
import com.xxmicloxx.NoteBlockAPI.songplayer.RadioSongPlayer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
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
    private final int lobbyTime;
    private final BoundingBox spectatableArea;
    private final List<Integer> boosterRange;
    private final Playlist playlist;

    // Технические
    private final Blockparty plugin = Blockparty.getPlugin();
    private RadioSongPlayer songPlayer;
    private final Component arenaFullMessage = Component.text("Арена уже заполнена или на ней идёт игра, поэтому вы присоединены как наблюдатель").color(NamedTextColor.RED);
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
    public List<Player> getSpectators(){
        return spectators;
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

    public Arena(int id, String name, int lobbyTime, int difficulty, int timeReduction, int cooldown, int initialRoundTime,
                 int maxPlayers, int minPlayers, Location startLocation, Location hubLocation,
                 int minY, List<Integer> mainFloor, List<Integer> endGameFloor, List<int[]> patterns, List<Material> blocksPalette,
                 Playlist playlist, BoundingBox spectatableArea, List<Integer> boosterRange){
        this.id = id;
        this.lobbyTime = lobbyTime;

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

        this.playlist = playlist;

        playNextSong();

        this.spectatableArea = spectatableArea;
        this.boosterRange = boosterRange;

        joinMessage = Component.text("Вы присоединились к арене " + name).color(NamedTextColor.GRAY);
        leaveMessage = Component.text("Вы покинули арену " + name).color(NamedTextColor.GRAY);
    }

    public void join(Player p){
        if (gameActive || players.size() == maxPlayers){
            p.sendMessage(arenaFullMessage);
            becameSpectator(p);
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
            player.sendMessage(Component.text(p.getName() + " вышел. Осталось игроков: " + players.size()).color(NamedTextColor.RED));
        }
    }

    public void death(Player player){
        if (!players.contains(player)) return;

        becameSpectator(player);
        dyingOrder.add(player.getName());
        player.showTitle(Title.title(loseMessage, Component.text("")));
        players.remove(player);

        for (Player p : players){
            p.sendMessage(Component.text(player.getName() + " выбывает. Осталось игроков: " + players.size()).color(NamedTextColor.RED));
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
                    this.cancel();
                    timerStarted = false;
                    Utils.resetExpTimer(players, true);
                }

                lobbyTimer -= 1;
                Utils.setExpTimer(players, lobbyTimer, lobbyTime, (lobbyTimer <= 5));

                if (lobbyTimer == 0){
                    this.cancel();
                    timerStarted = false;
                    startGame();
                }
            }
        };
        lobbyCountdown.runTaskTimer(Blockparty.getPlugin(), 0, 20);
    }

    private void startGame(){

        gameActive = true;
        currentRoundTime = initialRoundTime;
        playedRounds = 0;

        bossBar.color(BossBar.Color.RED);
        bossBar.name(Component.text("Текущая сложность: " + currentRoundTime + " секунд").color(NamedTextColor.WHITE));

        songPlayer.destroy();
        playNextSong();
        songPlayer.setPlaying(true);

        nextBlock = Utils.getRandomBlock(blocksPalette, null);
        nextPattern = Utils.getRandomPattern(patterns, null);

        for (Player p : players) {
            p.showTitle(Title.title(Component.text("Игра началась!").color(NamedTextColor.GREEN), Component.text("Успей встать на нужный блок").color(NamedTextColor.WHITE)));
            StatsManager.updateGames(p.getName());
            p.teleport(startLocation);
            songPlayer.addPlayer(p);
            songPlayer.setEnable10Octave(true);
            p.sendActionBar(Component.text("Сейчас играет: " + songPlayer. getSong().getTitle() + " - " + songPlayer.getSong().getAuthor()));
        }

        roundStart();
        BukkitRunnable endgameWatchdog = new BukkitRunnable() {
            @Override
            public void run() {
                if (players.size() > 1) return;

                if (players.size() == 0){
                    stopGame();
                    this.cancel();
                    return;
                }

                win(players.get(0));
                this.cancel();
            }
        };
        endgameWatchdog.runTaskTimer(plugin, 0, 5);
    }

    private void win(Player winner){
        winner.showTitle(Title.title(winMessage, Component.text("")));
        for (Player spectator : spectators){
            spectator.showTitle(Title.title(Component.text("Победил игрок " + winner.getName()).color(NamedTextColor.YELLOW), Component.text("")));
        }
        dyingOrder.add(winner.getName());
        StatsManager.updateWins(winner.getName());
        clearPlayer(winner);

        showTop();
        stopGame();
    }

    private void stopGame(){
        BukkitRunnable clearMap = new BukkitRunnable() {
            @Override
            public void run() {
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "clone " + endGameFloor.get(0) + " " + endGameFloor.get(1) + " " + endGameFloor.get(2) + " " + endGameFloor.get(3) + " " + endGameFloor.get(4) + " " + endGameFloor.get(5) + " " + mainFloor.get(0) + " " + mainFloor.get(1) + " " + mainFloor.get(2));
                gameActive = false;
            }
        };
        clearMap.runTaskLater(plugin, 20);

        for (int i = spectators.size(); i > 0; i--){
            Player player = spectators.get(0);
            clearPlayer(player);
        }

        bossBar.color(BossBar.Color.WHITE);
        bossBar.name(Component.text("Ждём игроков...").color(NamedTextColor.WHITE));

        songPlayer.setPlaying(false);

        players.clear();
        spectators.clear();

        dyingOrder.clear();
    }

    // Механика раундов

    private void roundStart(){
        currentBlock = nextBlock;
        nextBlock = Utils.getRandomBlock(blocksPalette, nextBlock);

        summonBoosters();

        int[] currentPattern = Utils.getRandomPattern(patterns, nextPattern);
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "clone " + currentPattern[0] + " " + currentPattern[1] + " " + currentPattern[2] + " " + currentPattern[3] + " " + currentPattern[4] + " " + currentPattern[5] + " " + mainFloor.get(0) + " " + mainFloor.get(1) + " " + mainFloor.get(2));

        BukkitRunnable roundTimer = new BukkitRunnable() {
            int count = currentRoundTime;

            @Override
            public void run() {
                if (!gameActive){
                    this.cancel();
                    return;
                }

                if (count == currentRoundTime){
                    for (Player player : players){
                        StatsManager.updateRounds(player.getName());
                        ItemStack itemStack = new ItemStack(currentBlock, 1);
                        ItemMeta meta = itemStack.getItemMeta();
                        meta.displayName(blockName);
                        itemStack.setItemMeta(meta);
                        Utils.fillHotbar(player, itemStack);
                    }
                }

                Utils.setExpTimer(players, count, currentRoundTime, true);

                if (count == 0){
                    for (Material material : blocksPalette){
                        if (material.equals(currentBlock)){
                            continue;
                        }
                        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "fill " + mainFloor.get(0) + " " + mainFloor.get(1) + " " + mainFloor.get(2) + " " + mainFloor.get(3) + " " + mainFloor.get(4) + " " + mainFloor.get(5) + " minecraft:air replace minecraft:" + material.toString().toLowerCase(Locale.ROOT));
                    }
                    playedRounds++;
                    cooldownStart();
                    for (Entity entity : startLocation.getWorld().getNearbyEntities(spectatableArea, entity -> entity.getType().equals(EntityType.DROPPED_ITEM))){
                        entity.remove();
                    }
                    this.cancel();
                }
                count -= 1;
            }
        };
        roundTimer.runTaskTimer(plugin, 20 * 3, 20);
    }

    private void cooldownStart(){
        for (Player player : players){
            player.getInventory().remove(currentBlock);
        }

        BukkitRunnable cooldownTimer = new BukkitRunnable() {
            int count = cooldown;

            @Override
            public void run() {
                if (!gameActive){
                    this.cancel();
                    return;
                }

                count -= 1;

                Utils.setExpTimer(players, count, cooldown, false);

                if (count == 0){
                    if (playedRounds % difficulty == 0) increaseDifficulty();
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
        currentRoundTime -= timeReduction;
        bossBar.name(Component.text("Текущая сложность: " + currentRoundTime + " секунд").color(NamedTextColor.WHITE));
        for (Player player : players){
            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 100, 1);
            player.sendActionBar(difficultyIncreaseInform);
        }
    }

    private void becameSpectator(Player player){
        player.getInventory().clear();
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "cmi kit bp " + player.getName());
        spectators.add(player);
        Utils.resetExpTimer(player, false);
        Utils.hidePlayer(players, player);
        Utils.showPlayersToPlayer(spectators, player);
        player.setAllowFlight(true);
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
            player.setAllowFlight(false);
            Utils.showPlayerToPlayers(players, player);
        }

        player.hideBossBar(bossBar);
        player.getInventory().clear();
        player.teleport(hubLocation);
        Utils.resetExpTimer(player, false);
        songPlayer.removePlayer(player);
    }

    public void boosterPickup(Player player) {
        player.playSound(player, Sound.ITEM_BOTTLE_FILL_DRAGONBREATH, 100, 2);
        StatsManager.updateBoosterCollected(player.getName());
        switch (new Random().nextInt(3)) {
            case (0):
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 15, 1));
                player.sendMessage(Component.text("Ты подобрал бустер СКОРОСТИ!").color(NamedTextColor.GREEN));
                player.sendActionBar(Component.text("Ты подобрал бустер СКОРОСТИ!").color(NamedTextColor.GREEN));
                break;
            case (1):
                player.sendMessage(Component.text("Ты подобрал бустер ПОДСКАЗКУ!").color(NamedTextColor.GREEN));
                player.sendActionBar(Component.text("Ты подобрал бустер ПОДСКАЗКУ!").color(NamedTextColor.GREEN));
                ItemStack item = new ItemStack(nextBlock);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(Component.text("Блок в следующем раунде").color(NamedTextColor.YELLOW));
                item.setItemMeta(meta);
                player.getInventory().setItem(0, item);
                break;
            case (2):
                player.sendMessage(Component.text("Ты подобрал бустер ПЛАТФОРМУ!").color(NamedTextColor.GREEN));
                player.sendActionBar(Component.text("Ты подобрал бустер ПЛАТФОРМУ!").color(NamedTextColor.GREEN));
                Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "fill " + (player.getLocation().getBlockX() - 1) + " " + mainFloor.get(1) + " " + (player.getLocation().getBlockZ() - 1) + " " + (player.getLocation().getBlockX() + 1) + " " + mainFloor.get(4) + " " + (player.getLocation().getBlockZ() + 1) + " minecraft:" + currentBlock.toString().toLowerCase(Locale.ROOT));
                break;
        }
    }

    private void showTop(){
        Collections.reverse(dyingOrder);
        Component topLine = Component.text("+---- ТОП ----+").color(NamedTextColor.GRAY);
        Component bottomLine = Component.text("+---- --- ----+").color(NamedTextColor.GRAY);
        List<Component> places = new ArrayList<>();
        for (int i = 0; i < dyingOrder.size(); i++){
            if (i == 3) break;
            places.add(Component.text((i + 1) + ". "+ dyingOrder.get(i)).color(NamedTextColor.YELLOW));
        }
        for (Player player : spectators){
            player.sendMessage(topLine);
            for (Component place : places){
                player.sendMessage(place);
            }
            player.sendMessage(bottomLine);
        }
    }

    private void summonBoosters(){
        int rnd = new Random().nextInt(100);
        if (rnd > 75) return;
        else if (rnd > 50){
            ItemStack boosterStack = new ItemStack(Material.NOTE_BLOCK);
            Item booster = startLocation.getWorld().dropItem(new Location(startLocation.getWorld(), (Math.min(boosterRange.get(0), boosterRange.get(3)) + new Random().nextInt(Math.abs(boosterRange.get(0) - boosterRange.get(3)))), boosterRange.get(1), (Math.min(boosterRange.get(2), boosterRange.get(5)) + new Random().nextInt(Math.abs(boosterRange.get(2) - boosterRange.get(5))))), boosterStack);
            booster.setGlowing(true);
            booster.customName(Component.text("Бустер").color(NamedTextColor.YELLOW));
            booster.setCustomNameVisible(true);
        }
        else if (rnd > 25){
            for (int i = 0; i < 2; i++){
                ItemStack boosterStack = new ItemStack(Material.NOTE_BLOCK);
                Item booster = startLocation.getWorld().dropItem(new Location(startLocation.getWorld(), (Math.min(boosterRange.get(0), boosterRange.get(3)) + new Random().nextInt(Math.abs(boosterRange.get(0) - boosterRange.get(3)))), boosterRange.get(1), (Math.min(boosterRange.get(2), boosterRange.get(5)) + new Random().nextInt(Math.abs(boosterRange.get(2) - boosterRange.get(5))))), boosterStack);
                booster.setGlowing(true);
                booster.customName(Component.text("Бустер").color(NamedTextColor.YELLOW));
                booster.setCustomNameVisible(true);
            }
        }
        else if (rnd > 5){
            for (int i = 0; i < 3; i++){
                ItemStack boosterStack = new ItemStack(Material.NOTE_BLOCK);
                Item booster = startLocation.getWorld().dropItem(new Location(startLocation.getWorld(), (Math.min(boosterRange.get(0), boosterRange.get(3)) + new Random().nextInt(Math.abs(boosterRange.get(0) - boosterRange.get(3)))), boosterRange.get(1), (Math.min(boosterRange.get(2), boosterRange.get(5)) + new Random().nextInt(Math.abs(boosterRange.get(2) - boosterRange.get(5))))), boosterStack);
                booster.setGlowing(true);
                booster.customName(Component.text("Бустер").color(NamedTextColor.YELLOW));
                booster.setCustomNameVisible(true);
            }
        }
        else {
            for (int i = 0; i < 4; i++) {
                ItemStack boosterStack = new ItemStack(Material.NOTE_BLOCK);
                Item booster = startLocation.getWorld().dropItem(new Location(startLocation.getWorld(), (Math.min(boosterRange.get(0), boosterRange.get(3)) + new Random().nextInt(Math.abs(boosterRange.get(0) - boosterRange.get(3)))), boosterRange.get(1), (Math.min(boosterRange.get(2), boosterRange.get(5)) + new Random().nextInt(Math.abs(boosterRange.get(2) - boosterRange.get(5))))), boosterStack);
                booster.setGlowing(true);
                booster.customName(Component.text("Бустер").color(NamedTextColor.YELLOW));
                booster.setCustomNameVisible(true);
            }
        }
    }

    private void playNextSong(){
        songPlayer = new RadioSongPlayer(playlist.get(new Random().nextInt(playlist.getCount())));
        songPlayer.setRepeatMode(RepeatMode.ONE);
        songPlayer.setCategory(com.xxmicloxx.NoteBlockAPI.model.SoundCategory.RECORDS);
        songPlayer.setChannelMode(new MonoStereoMode());
    }
}