package pl.simpleholo;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HologramManager {

    static final LegacyComponentSerializer SER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private final SimpleHoloPlugin plugin;
    private final Map<String, Hologram> holograms = new LinkedHashMap<>();
    private final File file;
    private final boolean papi;
    private BukkitTask task;

    HologramManager(SimpleHoloPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "holograms.yml");
        this.papi = Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI");
    }

    boolean isPapi() {
        return papi;
    }

    Hologram get(String name) {
        return holograms.get(name);
    }

    Collection<Hologram> all() {
        return holograms.values();
    }

    // ---------- zapis / odczyt ----------

    void load() {
        holograms.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = y.getConfigurationSection("holograms");
        if (sec == null) {
            return;
        }
        for (String name : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(name);
            if (s == null) {
                continue;
            }
            Hologram h = new Hologram(name, s.getString("world", "world"),
                    s.getDouble("x"), s.getDouble("y"), s.getDouble("z"));
            h.lines.addAll(s.getStringList("lines"));
            if (h.lines.isEmpty()) {
                h.lines.add("&7(pusty hologram)");
            }
            holograms.put(name, h);
        }
    }

    void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Hologram h : holograms.values()) {
            String p = "holograms." + h.name + ".";
            y.set(p + "world", h.worldName);
            y.set(p + "x", h.x);
            y.set(p + "y", h.y);
            y.set(p + "z", h.z);
            y.set(p + "lines", h.lines);
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Nie mozna zapisac holograms.yml: " + e.getMessage());
        }
    }

    // ---------- operacje ----------

    Hologram create(String name, Location loc, String firstLine) {
        Hologram h = new Hologram(name, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());
        h.lines.add(firstLine);
        holograms.put(name, h);
        refresh(h);
        save();
        return h;
    }

    void delete(String name) {
        Hologram h = holograms.remove(name);
        if (h != null) {
            despawn(h);
            save();
        }
    }

    /** Wywolaj po zmianie linii. */
    void changed(Hologram h) {
        h.lastText = null;
        refresh(h);
        save();
    }

    void move(Hologram h, Location loc) {
        h.setLocation(loc);
        if (h.entity != null && h.entity.isValid()) {
            h.entity.teleport(loc);
        }
        save();
    }

    void reload() {
        shutdown();
        load();
        startTask();
    }

    // ---------- odswiezanie ----------

    void startTask() {
        if (task != null) {
            task.cancel();
        }
        long interval = Math.max(5L, plugin.getConfig().getLong("refresh-ticks", 40L));
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Hologram h : holograms.values()) {
                refresh(h);
            }
        }, 20L, interval);
    }

    void shutdown() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (Hologram h : holograms.values()) {
            despawn(h);
        }
    }

    private void refresh(Hologram h) {
        World w = h.world();
        if (w == null) {
            return;
        }
        if (h.entity == null || !h.entity.isValid()) {
            h.entity = null;
            int cx = ((int) Math.floor(h.x)) >> 4;
            int cz = ((int) Math.floor(h.z)) >> 4;
            if (!w.isChunkLoaded(cx, cz)) {
                return; // chunk wylaczony - odtworzymy, gdy sie zaladuje
            }
            spawn(h, w);
            return;
        }
        String text = render(h);
        if (!text.equals(h.lastText)) {
            h.entity.text(SER.deserialize(text));
            h.lastText = text;
        }
    }

    private String render(Hologram h) {
        String text = String.join("\n", h.lines);
        if (papi) {
            text = PapiHook.apply(text);
        }
        return text.replace('\u00A7', '&');
    }

    private void spawn(Hologram h, World w) {
        String text = render(h);
        boolean transparent = plugin.getConfig().getBoolean("transparent-background", false);
        Component component = SER.deserialize(text);

        h.entity = w.spawn(h.location(), TextDisplay.class, e -> {
            e.setPersistent(false);
            e.setBillboard(Display.Billboard.CENTER);
            e.setAlignment(TextDisplay.TextAlignment.CENTER);
            e.setLineWidth(1000);
            e.setShadowed(false);
            e.setSeeThrough(false);
            if (transparent) {
                e.setDefaultBackground(false);
                e.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            }
            e.text(component);
        });
        h.lastText = text;
    }

    private void despawn(Hologram h) {
        if (h.entity != null) {
            h.entity.remove();
            h.entity = null;
        }
        h.lastText = null;
    }
}
