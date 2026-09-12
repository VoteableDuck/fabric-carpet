package net.fabricmc.loader.api;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import net.fabricmc.api.EnvType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

/**
 * Small compatibility facade that maps the Fabric Loader calls used by Carpet
 * onto NeoForge/FML. This is not Fabric Loader and is not intended as a general
 * Fabric compatibility layer.
 */
public final class FabricLoader {
    private static final FabricLoader INSTANCE = new FabricLoader();

    private FabricLoader() {
    }

    public static FabricLoader getInstance() {
        return INSTANCE;
    }

    public Optional<ModContainer> getModContainer(String modId) {
        ModList list = ModList.get();
        if (list == null) return Optional.empty();
        return list.getMods().stream()
                .filter(info -> info.getModId().equals(modId))
                .findFirst()
                .map(ModContainer::new);
    }

    public Collection<ModContainer> getAllMods() {
        ModList list = ModList.get();
        if (list == null) return List.of();
        return list.getMods().stream().map(ModContainer::new).toList();
    }

    public EnvType getEnvironmentType() {
        return FMLEnvironment.getDist() == Dist.CLIENT ? EnvType.CLIENT : EnvType.SERVER;
    }

    public boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.isProduction();
    }

    public Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    /**
     * Carpet only uses launch arguments for its optional rule-dump CLI. FML does
     * not expose Fabric's exact API, so recover the Java command line while
     * preserving quoted arguments.
     */
    public String[] getLaunchArguments(boolean sanitize) {
        String command = System.getProperty("sun.java.command", "");
        if (command.isBlank()) return new String[0];
        List<String> tokens = tokenize(command);
        if (!tokens.isEmpty()) tokens.removeFirst();
        if (sanitize) sanitize(tokens);
        return tokens.toArray(String[]::new);
    }

    private static List<String> tokenize(String value) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        char quote = 0;
        boolean escaped = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (escaped) {
                current.append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (quoted) {
                if (c == quote) quoted = false;
                else current.append(c);
            } else if (c == '\'' || c == '"') {
                quoted = true;
                quote = c;
            } else if (Character.isWhitespace(c)) {
                if (!current.isEmpty()) {
                    result.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (!current.isEmpty()) result.add(current.toString());
        return result;
    }

    private static void sanitize(List<String> args) {
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.equals("--accessToken") || arg.equals("--clientId") || arg.equals("--uuid") || arg.equals("--xuid")) {
                if (i + 1 < args.size()) args.set(++i, "<redacted>");
            }
        }
    }
}
