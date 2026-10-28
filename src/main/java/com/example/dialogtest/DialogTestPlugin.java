package com.example.dialogtest;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class DialogTestPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        // Save default config.yml if it doesn't already exist
        saveDefaultConfig();

        final PluginCommand taskCommand = getCommand("task");
        if (taskCommand != null) {
            taskCommand.setExecutor(new TaskCommand(this));
        } else {
            getLogger().severe("Failed to register /task command: missing from plugin.yml");
        }
        getLogger().info("PaperDialogTest plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        getLogger().info("PaperDialogTest plugin disabled.");
    }
}
