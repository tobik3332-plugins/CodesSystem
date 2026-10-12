package net.codessystem;

import net.codessystem.command.CodeCommands;
import net.codessystem.gui.GuiManager;
import net.codessystem.listener.ChatInputListener;
import net.codessystem.listener.GuiListener;
import net.codessystem.manager.CodeManager;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class CodesSystem extends JavaPlugin {

    private CodeManager codeManager;
    private GuiManager guiManager;
    private ChatInputListener chatInputListener;
    private GuiListener guiListener;

    @Override
    public void onEnable() {
        saveDefaultConfig();

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

    public String getMessage(String path) {
        String msg = getConfig().getString("messages." + path, "");
        String prefix = getConfig().getString("messages.prefix", "");
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }
}
