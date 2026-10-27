package pl.simpleholo;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.TextDisplay;

import java.util.ArrayList;
import java.util.List;

public final class Hologram {

    final String name;
    String worldName;
    double x, y, z;
    final List<String> lines = new ArrayList<>();

    TextDisplay entity;
    String lastText;

    Hologram(String name, String worldName, double x, double y, double z) {
        this.name = name;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    World world() {
        return Bukkit.getWorld(worldName);
    }

    Location location() {
        return new Location(world(), x, y, z);
    }

    void setLocation(Location l) {
        this.worldName = l.getWorld().getName();
        this.x = l.getX();
        this.y = l.getY();
        this.z = l.getZ();
    }
}
