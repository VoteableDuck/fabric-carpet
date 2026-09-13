package carpet.neoforge;

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
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;

/**
 * NeoForge-native platform services used by Carpet. This deliberately
 * lives in Carpet's namespace: shipping fake net.fabricmc classes can
 * hijack the real Fabric Loader ABI used by compatibility-layer mods.
 */
public final class CarpetPlatform {
    private static final String CARPET_MOD_ID = "carpet";
    private static final String MINECRAFT_MOD_ID = "minecraft";
    private static final String MINECRAFT_VERSION = "1.21.11";
    private static final String CARPET_BASE_VERSION = "1.4.194";
    private static final Pattern MOD_ID_LINE = Pattern.compile("(?m)^\\s*modId\\s*=\\s*\"([^\"]+)\"\\s*$");
    private static final Pattern MOD_VERSION_LINE = Pattern.compile("(?m)^\\s*version\\s*=\\s*\"([^\"]+)\"\\s*$");
    private static volatile String earlyCarpetVersion;

    private CarpetPlatform() {
    }

    public record ModInfo(String id, String version) {
    }

    public static boolean isClient() {
        return FMLEnvironment.getDist() == Dist.CLIENT;
    }

    public static boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.isProduction();
    }

    public static Path getConfigDir() {
        return FMLPaths.CONFIGDIR.get();
    }

    public static Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    public static Optional<ModInfo> getMod(String modId) {
        try {
            ModList list = ModList.get();
            if (list != null) {
                Optional<ModInfo> loaded = list.getMods().stream()
                        .filter(info -> info.getModId().equals(modId))
                        .findFirst()
                        .map(info -> new ModInfo(info.getModId(), info.getVersion().toString()));
                if (loaded.isPresent()) return loaded;
            }
        } catch (IllegalStateException ignored) {
            // ModList may not be available during very early bootstrap.
        }

        if (MINECRAFT_MOD_ID.equals(modId)) {
            return Optional.of(new ModInfo(MINECRAFT_MOD_ID, MINECRAFT_VERSION));
        }
        if (CARPET_MOD_ID.equals(modId)) {
            return Optional.of(new ModInfo(CARPET_MOD_ID, getEarlyCarpetVersion()));
        }
        return Optional.empty();
    }

    public static Collection<ModInfo> getAllMods() {
        try {
            ModList list = ModList.get();
            if (list != null) {
                return list.getMods().stream()
                        .map(info -> new ModInfo(info.getModId(), info.getVersion().toString()))
                        .toList();
            }
        } catch (IllegalStateException ignored) {
        }
        return List.of();
    }

    public static String getCarpetVersion() {
        return getMod(CARPET_MOD_ID).map(ModInfo::version).orElse(CARPET_BASE_VERSION);
    }

    public static int[] getMinecraftReleaseTarget() {
        try {
            SemanticVersion version = SemanticVersion.parse(
                    getMod(MINECRAFT_MOD_ID).map(ModInfo::version).orElse(MINECRAFT_VERSION));
            return new int[] { version.component(1), version.component(2) };
        } catch (VersionParsingException ignored) {
            return new int[] { 21, 11 };
        }
    }

    public static boolean modVersionMatches(String modId, String expression) throws VersionParsingException {
        VersionPredicate predicate = VersionPredicate.parse(expression);
        Optional<ModInfo> mod = getMod(modId);
        if (mod.isEmpty()) return false;

        VersionValue present = VersionValue.parse(mod.get().version());
        return predicate.test(present)
                || (isDevelopmentEnvironment() && !(present instanceof SemanticVersion));
    }

    public static String[] getLaunchArguments(boolean sanitize) {
        String command = System.getProperty("sun.java.command", "");
        if (command.isBlank()) return new String[0];
        List<String> tokens = tokenize(command);
        if (!tokens.isEmpty()) tokens.remove(0);
        if (sanitize) sanitize(tokens);
        return tokens.toArray(String[]::new);
    }

    private static String getEarlyCarpetVersion() {
        String cached = earlyCarpetVersion;
        if (cached != null) return cached;
        String resolved = readCarpetVersionFromMetadata().orElse(CARPET_BASE_VERSION);
        earlyCarpetVersion = resolved;
        return resolved;
    }

    private static Optional<String> readCarpetVersionFromMetadata() {
        ClassLoader loader = CarpetPlatform.class.getClassLoader();
        try {
            Enumeration<URL> resources = loader.getResources("META-INF/neoforge.mods.toml");
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                try (InputStream input = resource.openStream();
                     BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
                    StringBuilder toml = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) toml.append(line).append('\n');

                    String[] modSections = toml.toString().split("\\[\\[mods]]");
                    for (int i = 1; i < modSections.length; i++) {
                        String section = modSections[i];
                        int nextTable = section.indexOf("[[");
                        if (nextTable >= 0) section = section.substring(0, nextTable);
                        Matcher idMatcher = MOD_ID_LINE.matcher(section);
                        if (!idMatcher.find() || !CARPET_MOD_ID.equals(idMatcher.group(1).trim())) continue;
                        Matcher versionMatcher = MOD_VERSION_LINE.matcher(section);
                        if (versionMatcher.find()) {
                            String version = versionMatcher.group(1).trim();
                            if (!version.isEmpty() && !version.contains("${")) return Optional.of(version);
                        }
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return Optional.empty();
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

    public static final class VersionParsingException extends Exception {
        public VersionParsingException(String message) { super(message); }
        public VersionParsingException(String message, Throwable cause) { super(message, cause); }
    }

    private interface VersionValue extends Comparable<VersionValue> {
        String friendlyString();

        static VersionValue parse(String value) throws VersionParsingException {
            try {
                return SemanticVersion.parse(value);
            } catch (VersionParsingException ignored) {
                if (value == null || value.isBlank()) throw new VersionParsingException("Version cannot be blank");
                return new StringVersion(value);
            }
        }
    }

    private record StringVersion(String value) implements VersionValue {
        @Override public String friendlyString() { return value; }
        @Override public int compareTo(VersionValue other) { return value.compareTo(other.friendlyString()); }
    }

    private static final class SemanticVersion implements VersionValue {
        private static final int WILDCARD = Integer.MIN_VALUE;
        private static final Pattern UNSIGNED_INTEGER = Pattern.compile("0|[1-9][0-9]*");
        private final String original;
        private final int[] components;
        private final String prerelease;
        private final boolean wildcard;

        private SemanticVersion(String original, int[] components, String prerelease, boolean wildcard) {
            this.original = original;
            this.components = components;
            this.prerelease = prerelease;
            this.wildcard = wildcard;
        }

        static SemanticVersion parse(String value) throws VersionParsingException {
            if (value == null || value.isBlank()) throw new VersionParsingException("Version cannot be blank");
            String original = value.trim();
            String core = original;
            String prerelease = null;
            int plus = core.indexOf('+');
            if (plus >= 0) core = core.substring(0, plus);
            int dash = core.indexOf('-');
            if (dash >= 0) {
                prerelease = core.substring(dash + 1);
                core = core.substring(0, dash);
            }
            if (core.isBlank()) throw new VersionParsingException("Missing semantic version core: " + value);
            String[] pieces = core.split("\\.");
            int[] components = new int[pieces.length];
            boolean wildcard = false;
            for (int i = 0; i < pieces.length; i++) {
                String piece = pieces[i];
                if (piece.equals("x") || piece.equals("X") || piece.equals("*")) {
                    components[i] = WILDCARD;
                    wildcard = true;
                } else {
                    try {
                        components[i] = Integer.parseInt(piece);
                    } catch (NumberFormatException e) {
                        throw new VersionParsingException("Invalid semantic version component '" + piece + "' in " + value, e);
                    }
                }
            }
            return new SemanticVersion(original, components, prerelease, wildcard);
        }

        int componentCount() { return components.length; }
        int component(int pos) { return pos < components.length ? components[pos] : (wildcard ? WILDCARD : 0); }
        @Override public String friendlyString() { return original; }

        @Override
        public int compareTo(VersionValue other) {
            if (!(other instanceof SemanticVersion semantic)) return original.compareTo(other.friendlyString());
            int count = Math.max(componentCount(), semantic.componentCount());
            for (int i = 0; i < count; i++) {
                int left = component(i) == WILDCARD ? 0 : component(i);
                int right = semantic.component(i) == WILDCARD ? 0 : semantic.component(i);
                int cmp = Integer.compare(left, right);
                if (cmp != 0) return cmp;
            }
            if (prerelease == null && semantic.prerelease == null) return 0;
            if (prerelease == null) return 1;
            if (semantic.prerelease == null) return -1;
            return comparePrerelease(prerelease, semantic.prerelease);
        }

        private static int comparePrerelease(String left, String right) {
            String[] l = left.split("\\.");
            String[] r = right.split("\\.");
            for (int i = 0; i < Math.max(l.length, r.length); i++) {
                if (i >= l.length) return -1;
                if (i >= r.length) return 1;
                String a = l[i];
                String b = r[i];
                boolean aNumeric = UNSIGNED_INTEGER.matcher(a).matches();
                boolean bNumeric = UNSIGNED_INTEGER.matcher(b).matches();
                if (aNumeric && bNumeric) {
                    int cmp = Integer.compare(a.length(), b.length());
                    if (cmp != 0) return cmp;
                } else if (aNumeric != bNumeric) {
                    return aNumeric ? -1 : 1;
                }
                int cmp = a.compareTo(b);
                if (cmp != 0) return cmp;
            }
            return 0;
        }
    }

    private static final class VersionPredicate implements Predicate<VersionValue> {
        private final List<List<Predicate<VersionValue>>> alternatives;
        private VersionPredicate(List<List<Predicate<VersionValue>>> alternatives) { this.alternatives = alternatives; }

        static VersionPredicate parse(String expression) throws VersionParsingException {
            if (expression == null || expression.isBlank() || expression.trim().equals("*")) {
                return new VersionPredicate(List.of(List.of(version -> true)));
            }
            List<List<Predicate<VersionValue>>> alternatives = new ArrayList<>();
            for (String alternative : expression.split("\\|\\|")) {
                String normalized = alternative.trim().replace(',', ' ');
                if (normalized.isEmpty()) continue;
                List<Predicate<VersionValue>> terms = new ArrayList<>();
                for (String token : normalized.split("\\s+")) if (!token.isBlank()) terms.add(parseTerm(token));
                if (!terms.isEmpty()) alternatives.add(terms);
            }
            if (alternatives.isEmpty()) throw new VersionParsingException("Invalid version predicate: " + expression);
            return new VersionPredicate(alternatives);
        }

        private static Predicate<VersionValue> parseTerm(String token) throws VersionParsingException {
            if (token.equals("*")) return version -> true;
            String operator = "=";
            String raw = token;
            for (String candidate : List.of(">=", "<=", "==", ">", "<", "~", "^", "=")) {
                if (token.startsWith(candidate)) {
                    operator = candidate;
                    raw = token.substring(candidate.length());
                    break;
                }
            }
            if (raw.isBlank()) throw new VersionParsingException("Missing version in predicate term: " + token);

            if (hasWildcardComponent(raw)) {
                if (!operator.equals("=") && !operator.equals("==")) {
                    throw new VersionParsingException(
                            "Version ranges with wildcards require equality/no operator: " + token);
                }
                SemanticVersion pattern = parseWildcardPattern(raw);
                return version -> wildcardMatches(version, pattern);
            }

            VersionValue target = VersionValue.parse(raw);
            if (!(target instanceof SemanticVersion semanticTarget)) {
                if (operator.equals(">") || operator.equals("<")) {
                    throw new VersionParsingException(
                            "Exclusive version ranges require semantic versions: " + token);
                }
                // Fabric Loader reduces inclusive comparisons against a non-semver
                // value to exact friendly-string equality.
                return version -> version.friendlyString().equals(target.friendlyString());
            }

            return switch (operator) {
                case ">=" -> version -> version.compareTo(semanticTarget) >= 0;
                case "<=" -> version -> version.compareTo(semanticTarget) <= 0;
                case ">" -> version -> version.compareTo(semanticTarget) > 0;
                case "<" -> version -> version.compareTo(semanticTarget) < 0;
                case "~" -> bounded(semanticTarget, tildeUpperBound(semanticTarget));
                case "^" -> bounded(semanticTarget, caretUpperBound(semanticTarget));
                case "=", "==" -> version -> version.compareTo(semanticTarget) == 0;
                default -> throw new VersionParsingException("Unsupported version predicate operator: " + operator);
            };
        }

        private static boolean hasWildcardComponent(String raw) {
            String core = raw;
            int plus = core.indexOf('+');
            if (plus >= 0) core = core.substring(0, plus);
            int dash = core.indexOf('-');
            if (dash >= 0) core = core.substring(0, dash);
            for (String component : core.split("\\.", -1)) {
                if (isWildcardComponent(component)) return true;
            }
            return false;
        }

        private static SemanticVersion parseWildcardPattern(String raw) throws VersionParsingException {
            String core = raw;
            int plus = core.indexOf('+');
            if (plus >= 0) core = core.substring(0, plus);
            int dash = core.indexOf('-');
            if (dash >= 0) {
                throw new VersionParsingException("Pre-release versions cannot use wildcard ranges: " + raw);
            }
            String[] components = core.split("\\.", -1);
            int firstWildcard = -1;
            for (int i = 0; i < components.length; i++) {
                if (isWildcardComponent(components[i])) {
                    if (firstWildcard < 0) firstWildcard = i;
                } else if (firstWildcard >= 0) {
                    throw new VersionParsingException("Interjacent wildcard ranges are not allowed: " + raw);
                }
            }
            if (firstWildcard <= 0) {
                throw new VersionParsingException("Wildcard range must follow a numeric component: " + raw);
            }
            return SemanticVersion.parse(raw);
        }

        private static boolean isWildcardComponent(String component) {
            return component.equals("*") || component.equals("x") || component.equals("X");
        }

        private static Predicate<VersionValue> bounded(SemanticVersion lower, SemanticVersion upper) {
            return version -> version.compareTo(lower) >= 0 && version.compareTo(upper) < 0;
        }

        private static SemanticVersion tildeUpperBound(SemanticVersion target) throws VersionParsingException {
            int major = target.component(0);
            int minor = target.component(1);
            // Fabric uses an empty prerelease key on the exclusive upper bound,
            // so e.g. ~1.2 excludes 1.3.0-alpha rather than only 1.3.0 itself.
            return SemanticVersion.parse(major + "." + (minor + 1) + ".0-");
        }

        private static SemanticVersion caretUpperBound(SemanticVersion target) throws VersionParsingException {
            int major = target.component(0);
            // Same Fabric sentinel here: ^1.2.3 must exclude 2.0.0 prereleases.
            return SemanticVersion.parse((major + 1) + ".0.0-");
        }

        private static boolean wildcardMatches(VersionValue version, SemanticVersion pattern) {
            if (!(version instanceof SemanticVersion semantic)) return false;
            for (int i = 0; i < pattern.componentCount(); i++) {
                int expected = pattern.component(i);
                if (expected == SemanticVersion.WILDCARD) return true;
                if (semantic.component(i) != expected) return false;
            }
            return true;
        }

        @Override
        public boolean test(VersionValue version) {
            for (List<Predicate<VersionValue>> alternative : alternatives) {
                boolean matches = true;
                for (Predicate<VersionValue> predicate : alternative) {
                    if (!predicate.test(version)) {
                        matches = false;
                        break;
                    }
                }
                if (matches) return true;
            }
            return false;
        }
    }
}
