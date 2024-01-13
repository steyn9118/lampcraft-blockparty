package steyn91.blockparty.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import steyn91.blockparty.Arena;
import steyn91.blockparty.Utils;

public class playerListeners implements Listener{

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event){
        if (Utils.getArenaOfPlayer(event.getPlayer()) != null) Utils.getArenaOfPlayer(event.getPlayer()).leave(event.getPlayer());
    }

    @EventHandler
    public void onPlayerMovementEvent(PlayerMoveEvent event){
        Arena arena = Utils.getArenaOfPlayer(event.getPlayer());
        if (arena == null) return;
        if (!arena.getSpectatableArea().contains(event.getPlayer().getLocation().getX(), event.getPlayer().getLocation().getY(), event.getPlayer().getLocation().getZ())) event.getPlayer().teleport(arena.getStartLocation());
        if (arena.getMinY() >= event.getPlayer().getLocation().getBlockY()) arena.death(event.getPlayer());

    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        if (event.getWhoClicked().isOp()) return;
        if (Utils.getArenaOfPlayer((Player) event.getWhoClicked()) != null) event.setCancelled(true);
    }

    @EventHandler
    public void onItemPickup(PlayerAttemptPickupItemEvent event){
        Arena arena = Utils.getArenaOfPlayer(event.getPlayer());
        if (arena != null) arena.boosterPickup(event.getPlayer());
        event.getItem().remove();
        event.setCancelled(true);
    }
}













