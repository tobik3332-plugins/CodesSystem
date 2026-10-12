package net.codessystem.manager;

import net.codessystem.CodesSystem;
import net.codessystem.model.CodeModel;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CodeManager {

    private final CodesSystem plugin;
    private final Map<String, CodeModel> codesMap = new HashMap<>();
    private final File codesFolder;

    public CodeManager(CodesSystem plugin) {
        this.plugin = plugin;
        this.codesFolder = new File(plugin.getDataFolder(), "codes");
        if (!codesFolder.exists()) {
            codesFolder.mkdirs();
        }
        loadAllCodes();
    }

    public void loadAllCodes() {
        codesMap.clear();
        File[] files = codesFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);
            String codeStr = config.getString("code");
            if (codeStr == null) continue;

            CodeModel code = new CodeModel(
                    codeStr,
                    config.getString("displayName", codeStr),
                    config.getString("permission", "codes.use.default"),
                    config.getString("command", "[console] give %player% diamond 1"),
                    config.getInt("usesGlobal", 10)
            );
            code.setUsesSingle(config.getInt("usesSingle", 1));
            code.setCurrentGlobalUses(config.getInt("currentGlobalUses", 0));
            code.setRefresh(config.getBoolean("refresh", false));
            code.setUsedPlayers(config.getStringList("usedPlayers"));
            code.setUsedIps(config.getStringList("usedIps"));

            codesMap.put(codeStr.toLowerCase(), code);
        }
    }

    public void saveCode(CodeModel code, String oldCleanFileName) {
        if (oldCleanFileName != null && !oldCleanFileName.equalsIgnoreCase(code.getCleanFileName())) {
            File oldFile = new File(codesFolder, oldCleanFileName + ".yml");
            if (oldFile.exists()) oldFile.delete();
        }

        File file = new File(codesFolder, code.getCleanFileName() + ".yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        config.set("code", code.getCode());
        config.set("displayName", code.getDisplayName());
        config.set("permission", code.getPermission());
        config.set("command", code.getCommand());
        config.set("usesSingle", code.getUsesSingle());
        config.set("usesGlobal", code.getUsesGlobal());
        config.set("currentGlobalUses", code.getCurrentGlobalUses());
        config.set("refresh", code.isRefresh());
        config.set("usedPlayers", code.getUsedPlayers());
        config.set("usedIps", code.getUsedIps());

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        codesMap.values().removeIf(c -> c.getCleanFileName().equalsIgnoreCase(code.getCleanFileName()));
        codesMap.put(code.getCode().toLowerCase(), code);
    }

    public void refreshCodeString(CodeModel code) {
        codesMap.remove(code.getCode().toLowerCase());

        String newCodeStr = generateRandomCode(8);
        code.setCode(newCodeStr);
        code.getUsedPlayers().clear();
        code.getUsedIps().clear();
        code.setCurrentGlobalUses(0);

        saveCode(code, code.getCleanFileName());
    }

    public boolean deleteCode(String codeStr) {
        CodeModel code = getCode(codeStr);
        if (code == null) return false;

        File file = new File(codesFolder, code.getCleanFileName() + ".yml");
        if (file.exists()) {
            file.delete();
        }
        codesMap.remove(code.getCode().toLowerCase());
        return true;
    }

    public CodeModel getCode(String query) {
        if (query == null) return null;
        String lowerQuery = query.toLowerCase();

        // 1. Hledání podle samotného řetězce kódu
        if (codesMap.containsKey(lowerQuery)) {
            return codesMap.get(lowerQuery);
        }

        // 2. Hledání podle displayName (i s/bez barevných kódů)
        for (CodeModel code : codesMap.values()) {
            if (code.getDisplayName().equalsIgnoreCase(query) ||
                ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', code.getDisplayName())).equalsIgnoreCase(query) ||
                code.getCleanFileName().equalsIgnoreCase(query)) {
                return code;
            }
        }

        return null;
    }

    public Collection<CodeModel> getAllCodes() {
        return codesMap.values();
    }

    public String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
