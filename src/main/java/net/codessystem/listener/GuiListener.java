package net.codessystem.listener;

import net.codessystem.CodesSystem;
import net.codessystem.model.CodeModel;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class GuiListener implements Listener {

    private final CodesSystem plugin;

    public GuiListener(CodesSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        FileConfiguration config = plugin.getConfig();

        String mainTitle = PlainTextComponentSerializer.plainText().serialize(plugin.parseText(config.getString("gui.main.title", "Správa Kódů")));
        String exhTitle = PlainTextComponentSerializer.plainText().serialize(plugin.parseText(config.getString("gui.exhausted.title", "Vyčerpané Kódy")));
        String editorTitlePrefix = PlainTextComponentSerializer.plainText().serialize(plugin.parseText(config.getString("gui.editor.title", "Úprava Kódu"))).split("%code%")[0];

        if (title.contains(mainTitle)) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;

            int slot = event.getSlot();

            if (slot == config.getInt("gui.main.create-button.slot", 48)) {
                player.closeInventory();
                plugin.getChatInputListener().awaitInput(player, input -> {
                    if (input.equalsIgnoreCase("cancel")) {
                        player.sendMessage(plugin.getMessageComponent("input-cancelled"));
                        plugin.getGuiManager().openMainGui(player);
                        return;
                    }
                    String generatedCode = plugin.getCodeManager().generateRandomCode(8);
                    CodeModel newCode = new CodeModel(generatedCode, input, "codes.use.default", "[console] give %player% diamond 1", 10);
                    plugin.getCodeManager().saveCode(newCode, null);
                    plugin.getGuiManager().openEditorGui(player, newCode);
                });
                return;
            }

            if (slot == config.getInt("gui.main.exhausted-button.slot", 50)) {
                plugin.getGuiManager().openExhaustedGui(player);
                return;
            }

            if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                for (var lineComp : item.getItemMeta().lore()) {
                    String plain = PlainTextComponentSerializer.plainText().serialize(lineComp);
                    if (plain.startsWith("Kód: ")) {
                        String codeStr = plain.replace("Kód: ", "").trim();
                        CodeModel code = plugin.getCodeManager().getCode(codeStr);
                        if (code != null) {
                            plugin.getGuiManager().openEditorGui(player, code);
                        }
                        break;
                    }
                }
            }
        } else if (title.contains(exhTitle)) {
            event.setCancelled(true);
            ItemStack item = event.getCurrentItem();
            if (item == null || item.getType() == Material.AIR) return;

            if (event.getSlot() == config.getInt("gui.exhausted.back-button.slot", 49)) {
                plugin.getGuiManager().openMainGui(player);
                return;
            }

            if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                for (var lineComp : item.getItemMeta().lore()) {
                    String plain = PlainTextComponentSerializer.plainText().serialize(lineComp);
                    if (plain.startsWith("Kód: ")) {
                        String codeStr = plain.replace("Kód: ", "").trim();
                        CodeModel code = plugin.getCodeManager().getCode(codeStr);
                        if (code != null) {
                            if (event.isRightClick()) {
                                code.setCurrentGlobalUses(0);
                                code.getUsedPlayers().clear();
                                code.getUsedIps().clear();
                                plugin.getCodeManager().saveCode(code, code.getCleanFileName());
                                player.sendMessage(plugin.parseText("&aKód byl úspěšně resetován!"));
                                plugin.getGuiManager().openExhaustedGui(player);
                            } else {
                                plugin.getGuiManager().openEditorGui(player, code);
                            }
                        }
                        break;
                    }
                }
            }
        } else if (title.startsWith(editorTitlePrefix)) {
            event.setCancelled(true);
            int slot = event.getSlot();

            String codeStr = title.substring(editorTitlePrefix.length()).trim();
            CodeModel code = plugin.getCodeManager().getCode(codeStr);
            if (code == null) return;

            String oldCleanName = code.getCleanFileName();

            if (slot == config.getInt("gui.editor.items.name.slot", 9)) {
                player.closeInventory();
                plugin.getChatInputListener().awaitInput(player, input -> {
                    if (!input.equalsIgnoreCase("cancel")) {
                        code.setDisplayName(input);
                        plugin.getCodeManager().saveCode(code, oldCleanName);
                    }
                    plugin.getGuiManager().openEditorGui(player, code);
                });
            } else if (slot == config.getInt("gui.editor.items.permission.slot", 10)) {
                player.closeInventory();
                plugin.getChatInputListener().awaitInput(player, input -> {
                    if (!input.equalsIgnoreCase("cancel")) {
                        code.setPermission(input);
                        plugin.getCodeManager().saveCode(code, oldCleanName);
                    }
                    plugin.getGuiManager().openEditorGui(player, code);
                });
            } else if (slot == config.getInt("gui.editor.items.command.slot", 11)) {
                player.closeInventory();
                plugin.getChatInputListener().awaitInput(player, input -> {
                    if (!input.equalsIgnoreCase("cancel")) {
                        code.setCommand(input);
                        plugin.getCodeManager().saveCode(code, oldCleanName);
                    }
                    plugin.getGuiManager().openEditorGui(player, code);
                });
            } else if (slot == config.getInt("gui.editor.items.uses-single.slot", 12)) {
                player.closeInventory();
                plugin.getChatInputListener().awaitInput(player, input -> {
                    if (!input.equalsIgnoreCase("cancel")) {
                        try {
                            code.setUsesSingle(Integer.parseInt(input));
                            plugin.getCodeManager().saveCode(code, oldCleanName);
                        } catch (NumberFormatException ignored) {}
                    }
                    plugin.getGuiManager().openEditorGui(player, code);
                });
            } else if (slot == config.getInt("gui.editor.items.uses-global.slot", 13)) {
                player.closeInventory();
                plugin.getChatInputListener().awaitInput(player, input -> {
                    if (!input.equalsIgnoreCase("cancel")) {
                        try {
                            code.setUsesGlobal(Integer.parseInt(input));
                            plugin.getCodeManager().saveCode(code, oldCleanName);
                        } catch (NumberFormatException ignored) {}
                    }
                    plugin.getGuiManager().openEditorGui(player, code);
                });
            } else if (slot == config.getInt("gui.editor.items.refresh.slot", 14)) {
                code.setRefresh(!code.isRefresh());
                plugin.getCodeManager().saveCode(code, oldCleanName);
                plugin.getGuiManager().openEditorGui(player, code);
            } else if (slot == config.getInt("gui.editor.items.stable-code.slot", 15)) {
                player.closeInventory();
                plugin.getChatInputListener().awaitInput(player, input -> {
                    if (!input.equalsIgnoreCase("cancel")) {
                        code.setStableCode(input);
                        plugin.getCodeManager().saveCode(code, oldCleanName);
                    }
                    plugin.getGuiManager().openEditorGui(player, code);
                });
            } else if (slot == config.getInt("gui.editor.items.icon.slot", 16)) {
                player.closeInventory();
                plugin.getChatInputListener().awaitInput(player, input -> {
                    if (!input.equalsIgnoreCase("cancel")) {
                        Material mat = Material.matchMaterial(input);
                        if (mat != null) {
                            code.setIconMaterial(mat.name());
                            plugin.getCodeManager().saveCode(code, oldCleanName);
                        }
                    }
                    plugin.getGuiManager().openEditorGui(player, code);
                });
            } else if (slot == config.getInt("gui.editor.items.register.slot", 22)) {
                plugin.getCodeManager().saveCode(code, oldCleanName);
                plugin.getGuiManager().openMainGui(player);
            } else if (slot == config.getInt("gui.editor.items.delete.slot", 26)) {
                player.closeInventory();
                promptDeleteConfirmation(player, code, true);
            }
        }
    }

    public void promptDeleteConfirmation(Player player, CodeModel code, boolean fromGui) {
        String msg = plugin.getConfig().getString("messages.delete-confirm-prompt", "")
                .replace("%code%", code.getActiveCode());
        player.sendMessage(plugin.parseText(msg));

        plugin.getChatInputListener().awaitInput(player, input -> {
            if (input.equalsIgnoreCase("accept")) {
                plugin.getCodeManager().deleteCode(code.getActiveCode());
                player.sendMessage(plugin.getMessageComponent("code-deleted"));
                if (fromGui) {
                    plugin.getGuiManager().openMainGui(player);
                }
            } else {
                player.sendMessage(plugin.getMessageComponent("delete-cancelled"));
                if (fromGui) {
                    plugin.getGuiManager().openEditorGui(player, code);
                }
            }
        });

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getChatInputListener().removePending(player);
        }, 100L);
    }
}
