package net.codessystem.command;

import net.codessystem.CodesSystem;
import net.codessystem.model.CodeModel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CodeCommands implements CommandExecutor, TabCompleter {

    private final CodesSystem plugin;

    public CodeCommands(CodesSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase();

        if (name.equals("code")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Tento příkaz je pouze pro hráče.");
                return true;
            }
            if (args.length < 1) {
                player.sendMessage(plugin.getMessageComponent("help-code"));
                return true;
            }
            redeemCode(player, args[0]);
            return true;
        }

        if (name.equals("codes")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Tento příkaz je pouze pro hráče.");
            } else {
                plugin.getGuiManager().openMainGui(player);
            }
            return true;
        }

        if (name.equals("ecode")) {
            if (args.length == 0) {
                sender.sendMessage(plugin.getMessageComponent("help-ecode"));
                return true;
            }
            if (args[0].equalsIgnoreCase("create")) {
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageComponent("help-ecode-create"));
                    return true;
                }
                String displayName = args[1];
                String generatedCode = plugin.getCodeManager().generateRandomCode(8);

                CodeModel newCode = new CodeModel(generatedCode, displayName, "codes.use.default", "[console] give %player% diamond 1", 10);
                plugin.getCodeManager().saveCode(newCode, null);

                String rawMsg = plugin.getConfig().getString("messages.code-created", "").replace("%display%", displayName);
                sender.sendMessage(plugin.parseText(rawMsg));

                if (sender instanceof Player player) {
                    plugin.getGuiManager().openEditorGui(player, newCode);
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("delete")) {
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageComponent("help-ecode-delete"));
                    return true;
                }
                CodeModel code = plugin.getCodeManager().getCode(args[1]);
                if (code == null) {
                    sender.sendMessage(plugin.getMessageComponent("code-not-found"));
                    return true;
                }
                if (sender instanceof Player player) {
                    plugin.getGuiListener().promptDeleteConfirmation(player, code, false);
                } else {
                    plugin.getCodeManager().deleteCode(code.getActiveCode());
                    String rawMsg = plugin.getConfig().getString("messages.code-deleted", "").replace("%code%", code.getActiveCode());
                    sender.sendMessage(plugin.parseText(rawMsg));
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("edit")) {
                if (args.length < 4) {
                    sender.sendMessage(plugin.getMessageComponent("help-ecode-edit"));
                    return true;
                }
                CodeModel code = plugin.getCodeManager().getCode(args[1]);
                if (code == null) {
                    sender.sendMessage(plugin.getMessageComponent("code-not-found"));
                    return true;
                }

                String field = args[2].toLowerCase();
                String value = args[3];
                String oldClean = code.getCleanFileName();

                switch (field) {
                    case "name" -> code.setDisplayName(value);
                    case "permission" -> code.setPermission(value);
                    case "uses-single" -> code.setUsesSingle(Integer.parseInt(value));
                    case "uses-global" -> code.setUsesGlobal(Integer.parseInt(value));
                    case "refresh" -> code.setRefresh(Boolean.parseBoolean(value));
                    case "stable-code" -> code.setStableCode(value);
                    case "icon" -> {
                        Material mat = Material.matchMaterial(value);
                        if (mat != null) {
                            code.setIconMaterial(mat.name());
                        } else {
                            sender.sendMessage(plugin.parseText("&cNeznámý materiál!"));
                            return true;
                        }
                    }
                    default -> {
                        sender.sendMessage(plugin.getMessageComponent("help-ecode-edit"));
                        return true;
                    }
                }
                plugin.getCodeManager().saveCode(code, oldClean);

                String rawMsg = plugin.getConfig().getString("messages.code-edited", "")
                        .replace("%field%", field)
                        .replace("%code%", code.getActiveCode())
                        .replace("%value%", value);
                sender.sendMessage(plugin.parseText(rawMsg));
                return true;
            }
        }

        if (name.equals("acode")) {
            if (args.length == 0) {
                sender.sendMessage(plugin.getMessageComponent("help-acode"));
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("codes.admin.reload")) {
                    sender.sendMessage(plugin.getMessageComponent("no-permission"));
                    return true;
                }
                plugin.reloadConfig();
                plugin.getCodeManager().loadAllCodes();
                sender.sendMessage(plugin.getMessageComponent("plugin-reloaded"));
                return true;
            }

            if (args[0].equalsIgnoreCase("list")) {
                if (!sender.hasPermission("codes.admin.list")) {
                    sender.sendMessage(plugin.getMessageComponent("no-permission"));
                    return true;
                }
                sender.sendMessage(plugin.getMessageComponent("list-header"));
                for (CodeModel code : plugin.getCodeManager().getAllCodes()) {
                    String line = plugin.getConfig().getString("messages.list-format", "&8- &e%code% &7(&f%display%&7)")
                            .replace("%code%", code.getActiveCode())
                            .replace("%display%", code.getDisplayName());
                    sender.sendMessage(plugin.parseText(line));
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("getcode")) {
                if (!sender.hasPermission("codes.admin.getcode")) {
                    sender.sendMessage(plugin.getMessageComponent("no-permission"));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessageComponent("help-acode-getcode"));
                    return true;
                }
                CodeModel code = plugin.getCodeManager().getCode(args[1]);
                if (code != null) {
                    String rawMsg = plugin.getConfig().getString("messages.getcode-success", "")
                            .replace("%display%", code.getDisplayName());

                    Component prefixComp = plugin.parseText(plugin.getConfig().getString("messages.prefix", ""));
                    Component baseMsg = plugin.parseText(rawMsg);
                    
                    Component codeComp = Component.text(code.getActiveCode(), NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.copyToClipboard(code.getActiveCode()))
                            .hoverEvent(HoverEvent.showText(plugin.parseText("&eKlikni pro zkopírování")));

                    sender.sendMessage(prefixComp.append(baseMsg).append(codeComp));
                } else {
                    sender.sendMessage(plugin.getMessageComponent("code-not-found"));
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("reset")) {
                if (!sender.hasPermission("codes.admin.reset")) {
                    sender.sendMessage(plugin.getMessageComponent("no-permission"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(plugin.getMessageComponent("help-acode-reset"));
                    return true;
                }
                String targetPlayerName = args[1];
                CodeModel code = plugin.getCodeManager().getCode(args[2]);
                if (code != null) {
                    OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(targetPlayerName);
                    String uuidStr = offPlayer.getUniqueId().toString().toLowerCase();

                    code.getUsedPlayers().remove(uuidStr);
                    code.getUsedPlayers().remove(targetPlayerName.toLowerCase());

                    if (offPlayer.isOnline() && offPlayer.getPlayer() != null && offPlayer.getPlayer().getAddress() != null) {
                        String ip = offPlayer.getPlayer().getAddress().getHostString();
                        code.getUsedIps().remove(ip);
                    }

                    plugin.getCodeManager().saveCode(code, code.getCleanFileName());

                    String rawMsg = plugin.getConfig().getString("messages.code-reset-success", "")
                            .replace("%code%", code.getActiveCode())
                            .replace("%player%", targetPlayerName);
                    sender.sendMessage(plugin.parseText(rawMsg));
                } else {
                    sender.sendMessage(plugin.getMessageComponent("code-not-found"));
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("availability")) {
                if (!sender.hasPermission("codes.admin.availability")) {
                    sender.sendMessage(plugin.getMessageComponent("no-permission"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(plugin.getMessageComponent("help-acode-availability"));
                    return true;
                }
                String targetPlayerName = args[1];
                CodeModel code = plugin.getCodeManager().getCode(args[2]);
                if (code != null) {
                    OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(targetPlayerName);
                    String uuidStr = offPlayer.getUniqueId().toString().toLowerCase();
                    boolean used = code.getUsedPlayers().contains(uuidStr) || code.getUsedPlayers().contains(targetPlayerName.toLowerCase());

                    if (used) {
                        String rawMsg = plugin.getConfig().getString("messages.availability-used", "")
                                .replace("%player%", targetPlayerName)
                                .replace("%display%", code.getDisplayName())
                                .replace("%code%", code.getActiveCode());
                        sender.sendMessage(plugin.parseText(rawMsg));
                    } else {
                        String rawMsg = plugin.getConfig().getString("messages.availability-not-used", "")
                                .replace("%player%", targetPlayerName)
                                .replace("%display%", code.getDisplayName())
                                .replace("%code%", code.getActiveCode());
                        sender.sendMessage(plugin.parseText(rawMsg));
                    }
                } else {
                    sender.sendMessage(plugin.getMessageComponent("code-not-found"));
                }
                return true;
            }
        }

        return true;
    }

    private void redeemCode(Player player, String codeStr) {
        CodeModel code = plugin.getCodeManager().getCode(codeStr);
        if (code == null) {
            player.sendMessage(plugin.getMessageComponent("code-not-found"));
            return;
        }

        if (!player.hasPermission(code.getPermission())) {
            player.sendMessage(plugin.getMessageComponent("no-permission"));
            return;
        }

        String ip = player.getAddress() != null ? player.getAddress().getHostString() : "unknown";
        String playerUUID = player.getUniqueId().toString().toLowerCase();

        if (code.getUsesSingle() != -1) {
            long ipUses = code.getUsedIps().stream().filter(i -> i.equalsIgnoreCase(ip)).count();
            if (ipUses >= code.getUsesSingle()) {
                player.sendMessage(plugin.getMessageComponent("code-ip-limit"));
                return;
            }
        }

        if (code.getUsesGlobal() != -1 && code.getUsesGlobal() > 0 && code.getCurrentGlobalUses() >= code.getUsesGlobal()) {
            player.sendMessage(plugin.getMessageComponent("code-global-limit"));
            return;
        }

        if (code.getUsedPlayers().contains(playerUUID)) {
            player.sendMessage(plugin.getMessageComponent("code-already-used"));
            return;
        }

        code.getUsedPlayers().add(playerUUID);
        code.getUsedIps().add(ip.toLowerCase());
        code.setCurrentGlobalUses(code.getCurrentGlobalUses() + 1);

        String cmd = code.getCommand().replace("%player%", player.getName());

        if (cmd.startsWith("[console]")) {
            String execute = cmd.replace("[console]", "").trim();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), execute);
        } else if (cmd.startsWith("[player]")) {
            String execute = cmd.replace("[player]", "").trim();
            player.performCommand(execute);
        } else {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
        }

        String rawMsg = plugin.getConfig().getString("messages.code-success", "")
                .replace("%code%", code.getActiveCode());
        player.sendMessage(plugin.parseText(rawMsg));

        if (code.isRefresh()) {
            plugin.getCodeManager().refreshCodeString(code);
        } else {
            plugin.getCodeManager().saveCode(code, code.getCleanFileName());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        String name = cmd.getName().toLowerCase();

        if (name.equals("code")) {
            return list; 
        }

        if (name.equals("ecode")) {
            if (args.length == 1) {
                list.addAll(List.of("create", "delete", "edit"));
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("edit"))) {
                for (CodeModel code : plugin.getCodeManager().getAllCodes()) {
                    list.add(code.getCleanFileName());
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("edit")) {
                list.addAll(List.of("name", "permission", "uses-single", "uses-global", "refresh", "stable-code", "icon"));
            }
        } else if (name.equals("acode")) {
            if (args.length == 1) {
                list.addAll(List.of("reset", "reload", "list", "availability", "getcode"));
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("availability"))) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    list.add(p.getName());
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("getcode")) {
                for (CodeModel code : plugin.getCodeManager().getAllCodes()) {
                    list.add(code.getCleanFileName());
                }
            } else if (args.length == 3 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("availability"))) {
                for (CodeModel code : plugin.getCodeManager().getAllCodes()) {
                    list.add(code.getCleanFileName());
                }
            }
        }
        return list;
    }
}
