package net.codessystem.model;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class CodeModel {
    private String code;
    private String stableCode;
    private String displayName;
    private String permission;
    private String command;
    private int usesSingle;
    private int usesGlobal;
    private int currentGlobalUses;
    private boolean refresh;
    private String iconMaterial;
    private List<String> usedPlayers;
    private List<String> usedIps;

    public CodeModel(String code, String displayName, String permission, String command, int usesGlobal) {
        this.code = code;
        this.stableCode = code;
        this.displayName = displayName != null ? displayName : code;
        this.permission = permission != null ? permission : "codes.use.default";
        this.command = command != null ? command : "[console] give %player% diamond 1";
        this.usesSingle = 1;
        this.usesGlobal = usesGlobal;
        this.currentGlobalUses = 0;
        this.refresh = false;
        this.iconMaterial = "PAPER";
        this.usedPlayers = new ArrayList<>();
        this.usedIps = new ArrayList<>();
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getStableCode() { return stableCode != null ? stableCode : code; }
    public void setStableCode(String stableCode) { this.stableCode = stableCode; }

    public String getActiveCode() {
        return refresh ? code : getStableCode();
    }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public int getUsesSingle() { return usesSingle; }
    public void setUsesSingle(int usesSingle) { this.usesSingle = usesSingle; }

    public int getUsesGlobal() { return usesGlobal; }
    public void setUsesGlobal(int usesGlobal) { this.usesGlobal = usesGlobal; }

    public int getCurrentGlobalUses() { return currentGlobalUses; }
    public void setCurrentGlobalUses(int currentGlobalUses) { this.currentGlobalUses = currentGlobalUses; }

    public boolean isRefresh() { return refresh; }
    public void setRefresh(boolean refresh) { this.refresh = refresh; }

    public String getIconMaterial() { return iconMaterial != null ? iconMaterial : "PAPER"; }
    public void setIconMaterial(String iconMaterial) { this.iconMaterial = iconMaterial; }

    public Material getMaterial() {
        try {
            Material mat = Material.matchMaterial(iconMaterial);
            return mat != null ? mat : Material.PAPER;
        } catch (Exception e) {
            return Material.PAPER;
        }
    }

    public List<String> getUsedPlayers() { return usedPlayers; }
    public void setUsedPlayers(List<String> usedPlayers) { this.usedPlayers = usedPlayers; }

    public List<String> getUsedIps() { return usedIps; }
    public void setUsedIps(List<String> usedIps) { this.usedIps = usedIps; }

    public String getCleanFileName() {
        return displayName.replaceAll("(?i)&[0-9a-fk-or]", "").replaceAll("[^a-zA-Z0-9_-]", "");
    }
}
