package net.fabricmc.loader.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String CARPET_MOD_ID = "carpet";
    private static final String MINECRAFT_MOD_ID = "minecraft";
    private static final String MINECRAFT_VERSION = "1.21.11";
    private static final String CARPET_BASE_VERSION = "1.4.194";
    private static final Pattern MOD_ID_LINE = Pattern.compile("(?m)^\\s*modId\\s*=\\s*\"([^\"]+)\"\\s*$");
    private static final Pattern MOD_VERSION_LINE = Pattern.compile("(?m)^\\s*version\\s*=\\s*\"([^\"]+)\"\\s*$");

    private volatile String earlyCarpetVersion;

    private FabricLoader() {
    }

    public static FabricLoader getInstance() {
        return INSTANCE;
    }

    public Optional<ModContainer> getModContainer(String modId) {
        // During client bootstrap Minecraft validates commands before NeoForge has
        // necessarily populated ModList. CarpetSettings is pulled in from those
        // command mixins, so Fabric Carpet's eager metadata lookups must not depend
        // on NeoForge mod construction having completed already.
        try {
            ModList list = ModList.get();
            if (list != null) {
                Optional<ModContainer> loaded = list.getMods().stream()
                        .filter(info -> info.getModId().equals(modId))
                        .findFirst()
                        .map(ModContainer::new);
                if (loaded.isPresent()) return loaded;
            }
        } catch (IllegalStateException ignored) {
            // FML may reject ModList access during very early bootstrap. Fall
            // through to the metadata that is safe to resolve at this stage.
        }

        return getEarlyBootstrapContainer(modId);
    }

    private Optional<ModContainer> getEarlyBootstrapContainer(String modId) {
        if (MINECRAFT_MOD_ID.equals(modId)) {
            return Optional.of(new ModContainer(MINECRAFT_MOD_ID, MINECRAFT_VERSION));
        }
        if (CARPET_MOD_ID.equals(modId)) {
            return Optional.of(new ModContainer(CARPET_MOD_ID, getEarlyCarpetVersion()));
        }
        return Optional.empty();
    }

    private String getEarlyCarpetVersion() {
        String cached = earlyCarpetVersion;
        if (cached != null) return cached;

        String resolved = readCarpetVersionFromMetadata().orElse(CARPET_BASE_VERSION);
        earlyCarpetVersion = resolved;
        return resolved;
    }

    /**
     * processResources expands ${version} in neoforge.mods.toml for both the
     * development run directory and the distributable JAR. A NeoForge classloader
     * can expose several resources with that same path, so inspect all of them and
     * only accept the [[mods]] block whose own modId is Carpet. Dependency blocks
     * mentioning Carpet must not be mistaken for Carpet's metadata.
     */
    private static Optional<String> readCarpetVersionFromMetadata() {
        ClassLoader loader = FabricLoader.class.getClassLoader();
        try {
            Enumeration<URL> resources = loader.getResources("META-INF/neoforge.mods.toml");
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                try (InputStream input = resource.openStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    StringBuilder toml = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        toml.append(line).append('\n');
                    }

                    String[] modSections = toml.toString().split("\\[\\[mods]]");
                    for (int i = 1; i < modSections.length; i++) {
                        String section = modSections[i];
                        int nextTable = section.indexOf("[[");
                        if (nextTable >= 0) section = section.substring(0, nextTable);

                        Matcher idMatcher = MOD_ID_LINE.matcher(section);
                        if (!idMatcher.find() || !CARPET_MOD_ID.equals(idMatcher.group(1).trim())) {
                            continue;
                        }

                        Matcher versionMatcher = MOD_VERSION_LINE.matcher(section);
                        if (versionMatcher.find()) {
                            String version = versionMatcher.group(1).trim();
                            if (!version.isEmpty() && !version.contains("${")) {
                                return Optional.of(version);
                            }
                        }
                    }
                }
            }
        } catch (IOException ignored) {
            // The compatibility facade must remain safe during crash/bootstrap
            // reporting. Falling back to Carpet's base version is preferable to
            // turning unavailable metadata into a bootstrap failure.
        }
        return Optional.empty();
    }

    public Collection<ModContainer> getAllMods() {
        try {
            ModList list = ModList.get();
            if (list != null) return list.getMods().stream().map(ModContainer::new).toList();
        } catch (IllegalStateException ignored) {
        }
        return List.of();
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
