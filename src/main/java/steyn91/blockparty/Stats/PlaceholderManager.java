package steyn91.blockparty.Stats;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlaceholderManager extends PlaceholderExpansion {

    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params){
        if (player == null) return "";

        String playerName = player.getName();

        if (StatsManager.getStatsByName(playerName) == null) return "";

        if (params.contains("wins")){

            return String.valueOf(StatsManager.getStatsByName(playerName).getWins());

        }

        else if (params.contains(("games"))){

            return String.valueOf(StatsManager.getStatsByName(playerName).getGames());

        }

        return "¯|_ツ_|¯";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "blockparty";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Steyn91";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public boolean persist() {
        return true;
    }
}
