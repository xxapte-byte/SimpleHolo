package pl.simpleholo;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;

/** Osobna klasa, zeby plugin dzialal tez bez PlaceholderAPI. */
final class PapiHook {

    private PapiHook() {
    }

    static String apply(String text) {
        return PlaceholderAPI.setPlaceholders((OfflinePlayer) null, text);
    }
}
