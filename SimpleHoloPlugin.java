package pl.simpleholo;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class SimpleHoloPlugin extends JavaPlugin {

    private HologramManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new HologramManager(this);
        manager.load();
        manager.startTask();

        HoloCommand handler = new HoloCommand(this, manager);
        PluginCommand cmd = getCommand("holo");
        if (cmd != null) {
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }
        getLogger().info("SimpleHolo wlaczony. PlaceholderAPI: " + (manager.isPapi() ? "TAK" : "NIE"));
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.shutdown();
        }
    }
}
