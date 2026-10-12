package net.codessystem;

import net.codessystem.command.CodeCommands;
import net.codessystem.gui.GuiManager;
import net.codessystem.listener.ChatInputListener;
import net.codessystem.listener.GuiListener;
import net.codessystem.manager.CodeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class CodesSystem extends JavaPlugin {

    private CodeManager codeManager;
    private GuiManager guiManager;
    private ChatInputListener chatInputListener;
    private GuiListener guiListener;
    private MiniMessage miniMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.miniMessage = MiniMessage.miniMessage();
        this.codeManager = new CodeManager(this);
        this.guiManager = new GuiManager(this);
        this.chatInputListener = new ChatInputListener(this);
        this.guiListener = new GuiListener(this);

        CodeCommands commands = new CodeCommands(this);

        if (getCommand("code") != null) getCommand("code").setExecutor(commands);
        if (getCommand("codes") != null) getCommand("codes").setExecutor(commands);
        if (getCommand("ecode") != null) getCommand("ecode").setExecutor(commands);
        if (getCommand("acode") != null) getCommand("acode").setExecutor(commands);

        getServer().getPluginManager().registerEvents(chatInputListener, this);
        getServer().getPluginManager().registerEvents(guiListener, this);

        getLogger().info("CodesSystem byl úspěšně aktivován!");
    }

    public CodeManager getCodeManager() { return codeManager; }
    public GuiManager getGuiManager() { return guiManager; }
    public ChatInputListener getChatInputListener() { return chatInputListener; }
    public GuiListener getGuiListener() { return guiListener; }
    public MiniMessage getMiniMessage() { return miniMessage; }

    public Component parseText(String input) {
        if (input == null) return Component.empty();

        String prefix = getConfig().getString("messages.prefix", "");
        if (input.contains("%prefix%")) {
            input = input.replace("%prefix%", prefix);
        }

        String colorized = ChatColor.translateAlternateColorCodes('&', input);
        Component comp = LegacyComponentSerializer.legacySection().deserialize(colorized);
        return comp.decoration(TextDecoration.ITALIC, false);
    }

    public Component getMessageComponent(String path) {
        String msg = getConfig().getString("messages." + path, "");
        return parseText(msg);
    }
}
