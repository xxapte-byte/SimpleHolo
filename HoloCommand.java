package pl.simpleholo;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class HoloCommand implements TabExecutor {

    private static final Pattern NAME = Pattern.compile("[a-z0-9_-]{1,32}");
    private static final List<String> SUBS = List.of(
            "create", "delete", "list", "info", "addline", "setline",
            "removeline", "movehere", "tp", "reload", "help");
    private static final List<String> NEED_NAME = List.of(
            "delete", "info", "addline", "setline", "removeline", "movehere", "tp");

    private final SimpleHoloPlugin plugin;
    private final HologramManager m;

    HoloCommand(SimpleHoloPlugin plugin, HologramManager m) {
        this.plugin = plugin;
        this.m = m;
    }

    private static void msg(CommandSender s, String text) {
        s.sendMessage(HologramManager.SER.deserialize("&8[&6Holo&8] &7" + text));
    }

    private static String join(String[] a, int from) {
        return String.join(" ", Arrays.copyOfRange(a, from, a.length));
    }

    private void help(CommandSender s) {
        msg(s, "&e/holo create <nazwa> [tekst] &8- &7nowy hologram w Twojej pozycji");
        msg(s, "&e/holo delete <nazwa>");
        msg(s, "&e/holo list &8| &e/holo info <nazwa>");
        msg(s, "&e/holo addline <nazwa> <tekst>");
        msg(s, "&e/holo setline <nazwa> <nr> <tekst>");
        msg(s, "&e/holo removeline <nazwa> <nr>");
        msg(s, "&e/holo movehere <nazwa> &8| &e/holo tp <nazwa>");
        msg(s, "&e/holo reload");
        msg(s, "&7Kolory: &f&&7 oraz hex &f&#ff8800&7. Placeholdery: &f%nazwa%");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("simpleholo.admin")) {
            msg(sender, "&cBrak uprawnien.");
            return true;
        }
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            help(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("list")) {
            if (m.all().isEmpty()) {
                msg(sender, "Brak hologramow.");
            }
            for (Hologram h : m.all()) {
                msg(sender, "&e" + h.name + " &8- &7" + h.worldName + " "
                        + (int) h.x + ", " + (int) h.y + ", " + (int) h.z
                        + " &8(" + h.lines.size() + " linii)");
            }
            return true;
        }

        if (sub.equals("reload")) {
            plugin.reloadConfig();
            m.reload();
            msg(sender, "&aPrzeladowano.");
            return true;
        }

        if (!SUBS.contains(sub)) {
            help(sender);
            return true;
        }
        if (args.length < 2) {
            msg(sender, "&cPodaj nazwe hologramu.");
            return true;
        }

        String name = args[1].toLowerCase(Locale.ROOT);

        if (sub.equals("create")) {
            if (!(sender instanceof Player p)) {
                msg(sender, "&cTa komende moze uzyc tylko gracz.");
                return true;
            }
            if (!NAME.matcher(name).matches()) {
                msg(sender, "&cNazwa: tylko a-z, 0-9, _ i - (max 32 znaki).");
                return true;
            }
            if (m.get(name) != null) {
                msg(sender, "&cHologram o tej nazwie juz istnieje.");
                return true;
            }
            String text = args.length > 2 ? join(args, 2) : "&eNowy hologram";
            m.create(name, p.getLocation().add(0, 2, 0), text);
            msg(sender, "&aUtworzono hologram &e" + name + "&a.");
            return true;
        }

        Hologram h = m.get(name);
        if (h == null) {
            msg(sender, "&cNie ma hologramu &e" + name + "&c.");
            return true;
        }

        switch (sub) {
            case "delete" -> {
                m.delete(name);
                msg(sender, "&aUsunieto &e" + name + "&a.");
            }
            case "info" -> {
                msg(sender, "&eLinie hologramu " + name + ":");
                for (int i = 0; i < h.lines.size(); i++) {
                    msg(sender, "&8" + (i + 1) + ". &f" + h.lines.get(i).replace("&", "&&"));
                }
            }
            case "addline" -> {
                if (args.length < 3) {
                    msg(sender, "&cUzycie: /holo addline <nazwa> <tekst>");
                    return true;
                }
                h.lines.add(join(args, 2));
                m.changed(h);
                msg(sender, "&aDodano linie nr " + h.lines.size() + ".");
            }
            case "setline" -> {
                if (args.length < 4) {
                    msg(sender, "&cUzycie: /holo setline <nazwa> <nr> <tekst>");
                    return true;
                }
                int n = parseLine(sender, args[2], h);
                if (n < 0) {
                    return true;
                }
                h.lines.set(n, join(args, 3));
                m.changed(h);
                msg(sender, "&aZmieniono linie " + (n + 1) + ".");
            }
            case "removeline" -> {
                if (args.length < 3) {
                    msg(sender, "&cUzycie: /holo removeline <nazwa> <nr>");
                    return true;
                }
                if (h.lines.size() <= 1) {
                    msg(sender, "&cTo ostatnia linia - uzyj /holo delete.");
                    return true;
                }
                int n = parseLine(sender, args[2], h);
                if (n < 0) {
                    return true;
                }
                h.lines.remove(n);
                m.changed(h);
                msg(sender, "&aUsunieto linie " + (n + 1) + ".");
            }
            case "movehere" -> {
                if (!(sender instanceof Player p)) {
                    msg(sender, "&cTylko gracz.");
                    return true;
                }
                m.move(h, p.getLocation().add(0, 2, 0));
                msg(sender, "&aPrzeniesiono hologram.");
            }
            case "tp" -> {
                if (!(sender instanceof Player p)) {
                    msg(sender, "&cTylko gracz.");
                    return true;
                }
                if (h.world() == null) {
                    msg(sender, "&cSwiat tego hologramu nie jest zaladowany.");
                    return true;
                }
                p.teleport(h.location());
            }
            default -> help(sender);
        }
        return true;
    }

    /** Zwraca indeks 0-based albo -1 (i wysyla blad). */
    private int parseLine(CommandSender s, String raw, Hologram h) {
        try {
            int n = Integer.parseInt(raw);
            if (n >= 1 && n <= h.lines.size()) {
                return n - 1;
            }
        } catch (NumberFormatException ignored) {
            // spadamy do komunikatu
        }
        msg(s, "&cNumer linii musi byc od 1 do " + h.lines.size() + ".");
        return -1;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission("simpleholo.admin")) {
            return out;
        }
        if (args.length == 1) {
            for (String s : SUBS) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
        } else if (args.length == 2 && NEED_NAME.contains(args[0].toLowerCase(Locale.ROOT))) {
            for (Hologram h : m.all()) {
                if (h.name.startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(h.name);
                }
            }
        }
        return out;
    }
}
