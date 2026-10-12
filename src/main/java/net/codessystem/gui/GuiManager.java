package net.codessystem.gui;

import net.codessystem.CodesSystem;
import net.codessystem.model.CodeModel;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GuiManager {

    private final CodesSystem plugin;

    public GuiManager(CodesSystem plugin) {
        this.plugin = plugin;
    }

    public void openMainGui(Player player) {
        FileConfiguration config = plugin.getConfig();
        Component title = plugin.parseText(config.getString("gui.main.title", "Správa Kódů"));
        int size = config.getInt("gui.main.size", 54);

        Inventory inv = Bukkit.createInventory(null, size, title);

        int slot = 0;
        for (CodeModel code : plugin.getCodeManager().getAllCodes()) {
            if (code.getUsesGlobal() > 0 && code.getCurrentGlobalUses() >= code.getUsesGlobal()) continue;
            if (slot >= size - 9) break;

            ItemStack item = new ItemStack(Material.PAPER);
            ItemMeta meta = item.getItemMeta();
            meta.displayName(plugin.parseText(code.getDisplayName()));

            List<Component> lore = new ArrayList<>();
            lore.add(plugin.parseText("&7Kód: &e" + code.getActiveCode()));
            lore.add(plugin.parseText("&7Permise: &f" + code.getPermission()));
            lore.add(plugin.parseText("&7Příkaz: &f" + code.getCommand()));
            lore.add(plugin.parseText("&7Globálně: &f" + code.getCurrentGlobalUses() + "/" + code.getUsesGlobal()));
            lore.add(plugin.parseText("&7Refresh: &f" + code.isRefresh()));
            lore.add(Component.empty());
            lore.add(plugin.parseText("&eKlikni pro úpravu kódu."));
            meta.lore(lore);
            item.setItemMeta(meta);

            inv.setItem(slot++, item);
        }

        int createSlot = config.getInt("gui.main.create-button.slot", 48);
        Material createMat = Material.valueOf(config.getString("gui.main.create-button.material", "EMERALD_BLOCK"));
        String createName = config.getString("gui.main.create-button.name", "&a+ Vytvořit Nový Kód");
        List<String> createLore = config.getStringList("gui.main.create-button.lore");
        inv.setItem(createSlot, createItem(createMat, createName, createLore));

        int exhSlot = config.getInt("gui.main.exhausted-button.slot", 50);
        Material exhMat = Material.valueOf(config.getString("gui.main.exhausted-button.material", "REDSTONE_BLOCK"));
        String exhName = config.getString("gui.main.exhausted-button.name", "&cVyčerpané Kódy");
        List<String> exhLore = config.getStringList("gui.main.exhausted-button.lore");
        inv.setItem(exhSlot, createItem(exhMat, exhName, exhLore));

        player.openInventory(inv);
    }

    public void openExhaustedGui(Player player) {
        FileConfiguration config = plugin.getConfig();
        Component title = plugin.parseText(config.getString("gui.exhausted.title", "Vyčerpané Kódy"));
        int size = config.getInt("gui.exhausted.size", 54);

        Inventory inv = Bukkit.createInventory(null, size, title);

        int slot = 0;
        for (CodeModel code : plugin.getCodeManager().getAllCodes()) {
            if (code.getUsesGlobal() > 0 && code.getCurrentGlobalUses() >= code.getUsesGlobal()) {
                if (slot >= size - 9) break;

                ItemStack item = new ItemStack(Material.STRUCTURE_VOID);
                ItemMeta meta = item.getItemMeta();
                meta.displayName(plugin.parseText(code.getDisplayName()));

                List<Component> lore = new ArrayList<>();
                lore.add(plugin.parseText("&7Kód: &e" + code.getActiveCode()));
                lore.add(plugin.parseText("&7Vyčerpáno: &c" + code.getCurrentGlobalUses() + "/" + code.getUsesGlobal()));
                lore.add(Component.empty());
                lore.add(plugin.parseText("&eKlikni pro úpravu/reset kódu."));
                meta.lore(lore);
                item.setItemMeta(meta);

                inv.setItem(slot++, item);
            }
        }

        int backSlot = config.getInt("gui.exhausted.back-button.slot", 49);
        Material backMat = Material.valueOf(config.getString("gui.exhausted.back-button.material", "BARRIER"));
        String backName = config.getString("gui.exhausted.back-button.name", "&cZpět");
        List<String> backLore = config.getStringList("gui.exhausted.back-button.lore");
        inv.setItem(backSlot, createItem(backMat, backName, backLore));

        player.openInventory(inv);
    }

    public void openEditorGui(Player player, CodeModel code) {
        FileConfiguration config = plugin.getConfig();
        String titleStr = config.getString("gui.editor.title", "Úprava Kódu: %code%").replace("%code%", code.getActiveCode());
        Component title = plugin.parseText(titleStr);
        int size = config.getInt("gui.editor.size", 27);

        Inventory inv = Bukkit.createInventory(null, size, title);

        setEditorItem(inv, config, "name", code.getDisplayName(), code);
        setEditorItem(inv, config, "permission", code.getPermission(), code);
        setEditorItem(inv, config, "command", code.getCommand(), code);
        setEditorItem(inv, config, "uses-single", String.valueOf(code.getUsesSingle()), code);
        setEditorItem(inv, config, "uses-global", "", code);
        setEditorItem(inv, config, "refresh", String.valueOf(code.isRefresh()), code);
        setEditorItem(inv, config, "stable-code", code.getStableCode(), code);
        setEditorItem(inv, config, "register", "", code);
        setEditorItem(inv, config, "delete", "", code);

        player.openInventory(inv);
    }

    private void setEditorItem(Inventory inv, FileConfiguration config, String key, String value, CodeModel code) {
        String path = "gui.editor.items." + key;
        if (!config.contains(path)) return;

        int slot = config.getInt(path + ".slot");
        Material mat = Material.valueOf(config.getString(path + ".material", "STONE"));
        String title = config.getString(path + ".title", "Item");
        List<String> rawLore = config.getStringList(path + ".lore");

        List<String> lore = new ArrayList<>();
        for (String line : rawLore) {
            line = line.replace("%value%", value)
                    .replace("%used%", String.valueOf(code.getCurrentGlobalUses()))
                    .replace("%max%", String.valueOf(code.getUsesGlobal()));
            lore.add(line);
        }

        inv.setItem(slot, createItem(mat, title, lore));
    }

    private ItemStack createItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.parseText(name));

        List<Component> compLore = new ArrayList<>();
        for (String line : lore) {
            compLore.add(plugin.parseText(line));
        }
        meta.lore(compLore);
        item.setItemMeta(meta);
        return item;
    }
}
