package net.codessystem.listener;

import net.codessystem.CodesSystem;
import net.codessystem.model.CodeModel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ChatInputListener implements Listener {

    private final CodesSystem plugin;
    private final Map<UUID, Consumer<String>> pendingInputs = new HashMap<>();

    public ChatInputListener(CodesSystem plugin) {
        this.plugin = plugin;
    }

    public void awaitInput(Player player, Consumer<String> callback) {
        pendingInputs.put(player.getUniqueId(), callback);
        player.sendMessage(plugin.getMessage("input-prompt"));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (pendingInputs.containsKey(uuid)) {
            event.setCancelled(true);
            Consumer<String> callback = pendingInputs.remove(uuid);
            String message = event.getMessage().trim();

            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(message));
        }
    }

    public void removePending(Player player) {
        pendingInputs.remove(player.getUniqueId());
    }
}
