package net.codessystem.command;

import net.codessystem.CodesSystem;
import net.codessystem.model.CodeModel;
import org.bukkit.Bukkit;
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
                player.sendMessage(plugin.getMessage("prefix") + "Použití: /code <code>");
                return true;
            }
            redeemCode(player, args[0]);
            return true;
        }

        if (name.equals("codes")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Tento příkaz je pouze pro hráče.");
                return true;
            }
            plugin.getGuiManager().openMainGui(player);
            return true;
        }

        if (name.equals("ecode")) {
            if (args.length == 0) {
                sender.sendMessage(plugin.getMessage("prefix") + "Použití: /ecode <create|delete|edit>");
                return true;
            }
            if (args[0].equalsIgnoreCase("create")) {
                if (args.length < 5) {
                    sender.sendMessage(plugin.getMessage("prefix") + "Použití: /ecode create <kod> <pocet_pouziti> <command> <permise>");
                    return true;
                }
                String codeStr = args[1];
                int uses = Integer.parseInt(args[2]);
                String command = args[3];
                String perm = args[4];

                CodeModel code = new CodeModel(codeStr, codeStr, perm, command, uses);
                plugin.getCodeManager().saveCode(code, null);
                sender.sendMessage(plugin.getMessage("code-created").replace("%code%", codeStr));
                return true;
            }

            if (args[0].equalsIgnoreCase("delete")) {
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessage("prefix") + "Použití: /ecode delete <kod>");
                    return true;
                }
                CodeModel code = plugin.getCodeManager().getCode(args[1]);
                if (code == null) {
                    sender.sendMessage(plugin.getMessage("code-not-found"));
                    return true;
                }
                if (sender instanceof Player player) {
                    plugin.getGuiListener().promptDeleteConfirmation(player, code, false);
                } else {
                    plugin.getCodeManager().deleteCode(code.getCode());
                    sender.sendMessage(plugin.getMessage("code-deleted").replace("%code%", code.getCode()));
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("edit")) {
                if (args.length < 4) {
                    sender.sendMessage(plugin.getMessage("prefix") + "Použití: /ecode edit <kod> <name|permission|uses-single|uses-global|refresh> <hodnota>");
                    return true;
                }
                CodeModel code = plugin.getCodeManager().getCode(args[1]);
                if (code == null) {
                    sender.sendMessage(plugin.getMessage("code-not-found"));
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
                    default -> {
                        sender.sendMessage(plugin.getMessage("prefix") + "Neznámé pole ke změně.");
                        return true;
                    }
                }
                plugin.getCodeManager().saveCode(code, oldClean);
                sender.sendMessage(plugin.getMessage("code-edited").replace("%field%", field).replace("%code%", code.getCode()).replace("%value%", value));
                return true;
            }
        }

        if (name.equals("acode")) {
            if (args.length == 0) return true;

            if (args[0].equalsIgnoreCase("reload")) {
                plugin.reloadConfig();
                plugin.getCodeManager().loadAllCodes();
                sender.sendMessage(plugin.getMessage("plugin-reloaded"));
                return true;
            }

            if (args[0].equalsIgnoreCase("list")) {
                sender.sendMessage(plugin.getMessage("prefix") + "&aAktivní kódy:");
                for (CodeModel code : plugin.getCodeManager().getAllCodes()) {
                    sender.sendMessage(" &8- &e" + code.getCode() + " &7(" + code.getDisplayName() + "&7)");
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("reset")) {
                if (args.length < 3) return true;
                String targetPlayer = args[1];
                CodeModel code = plugin.getCodeManager().getCode(args[2]);
                if (code != null) {
                    code.getUsedPlayers().remove(targetPlayer.toLowerCase());
                    plugin.getCodeManager().saveCode(code, code.getCleanFileName());
                    sender.sendMessage(plugin.getMessage("code-reset-success").replace("%code%", code.getCode()).replace("%player%", targetPlayer));
                }
                return true;
            }

            if (args[0].equalsIgnoreCase("availability")) {
                if (args.length < 3) return true;
                String targetPlayer = args[1];
                CodeModel code = plugin.getCodeManager().getCode(args[2]);
                if (code != null) {
                    boolean used = code.getUsedPlayers().contains(targetPlayer.toLowerCase());
                    if (used) {
                        sender.sendMessage(plugin.getMessage("availability-used").replace("%player%", targetPlayer).replace("%code%", code.getCode()));
                    } else {
                        sender.sendMessage(plugin.getMessage("availability-not-used").replace("%player%", targetPlayer).replace("%code%", code.getCode()));
                    }
                }
                return true;
            }
        }

        return true;
    }

    private void redeemCode(Player player, String codeStr) {
        CodeModel code = plugin.getCodeManager().getCode(codeStr);
        if (code == null) {
            player.sendMessage(plugin.getMessage("code-not-found"));
            return;
        }

        if (!player.hasPermission(code.getPermission())) {
            player.sendMessage(plugin.getMessage("no-permission"));
            return;
        }

        String ip = player.getAddress() != null ? player.getAddress().getHostString() : "unknown";
        String playerUUID = player.getUniqueId().toString();

        long ipUses = code.getUsedIps().stream().filter(i -> i.equalsIgnoreCase(ip)).count();
        if (ipUses >= code.getUsesSingle()) {
            player.sendMessage(plugin.getMessage("code-ip-limit"));
            return;
        }

        if (code.getUsesGlobal() > 0 && code.getCurrentGlobalUses() >= code.getUsesGlobal()) {
            player.sendMessage(plugin.getMessage("code-global-limit"));
            return;
        }

        if (code.getUsedPlayers().contains(playerUUID.toLowerCase())) {
            player.sendMessage(plugin.getMessage("code-already-used"));
            return;
        }

        code.getUsedPlayers().add(playerUUID.toLowerCase());
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

        player.sendMessage(plugin.getMessage("code-success").replace("%code%", code.getCode()));

        if (code.isRefresh()) {
            String oldCleanName = code.getCleanFileName();
            String newCodeStr = plugin.getCodeManager().generateRandomCode(8);
            code.setCode(newCodeStr);
            code.getUsedPlayers().clear();
            code.getUsedIps().clear();
            code.setCurrentGlobalUses(0);
            plugin.getCodeManager().saveCode(code, oldCleanName);
        } else {
            plugin.getCodeManager().saveCode(code, code.getCleanFileName());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        String name = cmd.getName().toLowerCase();

        if (name.equals("code")) {
            if (args.length == 1) {
                for (CodeModel code : plugin.getCodeManager().getAllCodes()) {
                    list.add(code.getCode());
                }
            }
        } else if (name.equals("ecode")) {
            if (args.length == 1) {
                list.addAll(List.of("create", "delete", "edit"));
            } else if (args.length == 2 && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("edit"))) {
                for (CodeModel code : plugin.getCodeManager().getAllCodes()) {
                    list.add(code.getCode());
                }
            } else if (args.length == 3 && args[0].equalsIgnoreCase("edit")) {
                list.addAll(List.of("name", "permission", "uses-single", "uses-global", "refresh"));
            }
        } else if (name.equals("acode")) {
            if (args.length == 1) {
                list.addAll(List.of("reset", "reload", "list", "availability"));
            } else if (args.length == 3 && (args[0].equalsIgnoreCase("reset") || args[0].equalsIgnoreCase("availability"))) {
                for (CodeModel code : plugin.getCodeManager().getAllCodes()) {
                    list.add(code.getCode());
                }
            }
        }
        return list;
    }
}
