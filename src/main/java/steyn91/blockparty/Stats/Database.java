package steyn91.blockparty.Stats;

import steyn91.blockparty.Blockparty;
import org.bukkit.Bukkit;

import java.sql.*;

public class Database {

    private static final Blockparty plugin = Blockparty.getPlugin();
    private static Connection connection;

    // Для получения подключения
    public static Connection getConnection() {

        String url = plugin.getConfig().getString("url");
        assert url != null;
        String user = plugin.getConfig().getString("user");
        String pwd = plugin.getConfig().getString("password");

        if (connection == null){
            // Подключение к БД
            try {
                connection = DriverManager.getConnection(url, user, pwd);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            Bukkit.getLogger().info("Подключено к БД");
        }

        try {
            connection.createStatement().execute("SELECT 1");
        } catch (SQLException e) {
            // Подключение к БД
            try {
                connection = DriverManager.getConnection(url, user, pwd);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            Bukkit.getLogger().info("Подключено к БД");
        }
        return connection;
    }

    public static void initDatabase(){
        try {
            Statement statement = getConnection().createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS blockparty_stats (playerName varchar(16) primary key, wins int, games int, rounds int, boosters int, lastUpdate DATE)";
            statement.execute(sql);
            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static PlayerStatsModel getPlayerStat(String playerName){

        try {

            PreparedStatement statement = getConnection().prepareStatement("SELECT * FROM blockparty_stats WHERE playerName = ?");

            statement.setString(1, playerName);

            ResultSet resultSet = statement.executeQuery();

            PlayerStatsModel statsModel;

            if (resultSet.next()){

                statsModel = new PlayerStatsModel(
                        resultSet.getString("playerName"),
                        resultSet.getInt("wins"),
                        resultSet.getInt("games"),
                        resultSet.getInt("rounds"),
                        resultSet.getInt("boosters"),
                        resultSet.getDate("lastUpdate"));

                statement.close();

                return statsModel;

            }

            statement.close();


        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void removePlayerStat(String playerName){

        try {

            PreparedStatement statement = getConnection().prepareStatement("DELETE FROM blockparty_stats WHERE playerName = ?");

            statement.setString(1, playerName);

            statement.executeUpdate();
            statement.close();


        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static void createPlayerStat(String playerName){
        try {

            PreparedStatement statement = getConnection().prepareStatement("INSERT INTO blockparty_stats(playerName, wins, games, rounds, boosters, lastUpdate) VALUES (?, ?, ?, ?, ?, ?)");

            statement.setString(1, playerName);
            statement.setInt(2, 0);
            statement.setInt(3, 0);
            statement.setInt(4, 0);
            statement.setInt(5, 0);
            statement.setDate(6, new Date(new java.util.Date().getTime()));

            statement.executeUpdate();
            statement.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }


    }

    public static void updatePlayerStat(PlayerStatsModel statsModel){

        String playerName = statsModel.getPlayerName();

        if (getPlayerStat(playerName) == null){
            createPlayerStat(playerName);
        }

        try {

            PreparedStatement statement = getConnection().prepareStatement("UPDATE blockparty_stats SET wins = ?, games = ?, rounds = ?, boosters = ?, lastUpdate = ? WHERE playerName = ?");

            statement.setInt(1, statsModel.getWins());
            statement.setInt(2, statsModel.getGames());
            statement.setInt(3, statsModel.getRounds());
            statement.setInt(4, statsModel.getBoosters());
            statement.setDate(5, new Date(new java.util.Date().getTime()));
            statement.setString(6, playerName);

            statement.executeUpdate();
            statement.close();


        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

}
