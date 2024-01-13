package steyn91.blockparty.Stats;

import java.util.Date;

public class PlayerStatsModel {

    private final String playerName;
    private int wins;
    private int games;
    private int rounds;
    private int boosters;
    private Date lastUpdate;

    public PlayerStatsModel(String playerName, int wins, int games, int rounds, int boosters, Date lastUpdate) {
        this.playerName = playerName;
        this.wins = wins;
        this.games = games;
        this.rounds = rounds;
        this.boosters = boosters;
        this.lastUpdate = lastUpdate;
    }

    public int getBoosters() {
        return boosters;
    }
    public void setBoosters(int boosters) {
        this.boosters = boosters;
    }
    public int getRounds() {
        return rounds;
    }

    public void setRounds(int rounds) {
        this.rounds = rounds;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getGames() {
        return games;
    }

    public void setGames(int games) {
        this.games = games;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(Date lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
