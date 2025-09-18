package org.yusaki.lib.gui;

import java.util.List;

/**
 * Configuration for GUI item command execution
 */
public class GUICommandConfig {
    private final List<String> commands;
    private final String executeAs;
    private final String permission;
    private final boolean closeOnClick;
    private final String sound;
    private final int cooldown;
    
    public GUICommandConfig(List<String> commands, String executeAs, String permission,
                           boolean closeOnClick, String sound, int cooldown) {
        this.commands = commands;
        this.executeAs = executeAs != null ? executeAs : "player";
        this.permission = permission;
        this.closeOnClick = closeOnClick;
        this.sound = sound;
        this.cooldown = cooldown;
    }
    
    public List<String> getCommands() {
        return commands;
    }
    
    public String getExecuteAs() {
        return executeAs;
    }
    
    public String getPermission() {
        return permission;
    }
    
    public boolean shouldCloseOnClick() {
        return closeOnClick;
    }
    
    public String getSound() {
        return sound;
    }
    
    public int getCooldown() {
        return cooldown;
    }
    
    public boolean isConsoleExecution() {
        return "console".equalsIgnoreCase(executeAs);
    }
}