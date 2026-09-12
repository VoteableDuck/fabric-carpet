package net.fabricmc.loader.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Small semantic-version implementation matching the Fabric Loader API surface
 * Carpet uses. It accepts ordinary dotted numeric versions plus prerelease,
 * build metadata, and x/X/* wildcards.
 */
public interface SemanticVersion extends Version {
    int COMPONENT_WILDCARD = Integer.MIN_VALUE;

    int getVersionComponentCount();
    int getVersionComponent(int pos);
    Optional<String> getPrereleaseKey();
    Optional<String> getBuildKey();
    boolean hasWildcard();

    @Deprecated
    default int compareTo(SemanticVersion other) {
        return compareTo((Version) other);
    }

    static SemanticVersion parse(String value) throws VersionParsingException {
        return SimpleSemanticVersion.parse(value);
    }
}

final class SimpleSemanticVersion implements SemanticVersion {
    private final String original;
    private final int[] components;
    private final String prerelease;
    private final String build;
    private final boolean wildcard;

    private SimpleSemanticVersion(String original, int[] components, String prerelease, String build, boolean wildcard) {
        this.original = original;
        this.components = components;
        this.prerelease = prerelease;
        this.build = build;
        this.wildcard = wildcard;
    }

    static SimpleSemanticVersion parse(String value) throws VersionParsingException {
        if (value == null || value.isBlank()) {
            throw new VersionParsingException("Version cannot be blank");
        }

        String original = value.trim();
        String core = original;
        String build = null;
        String prerelease = null;

        int plus = core.indexOf('+');
        if (plus >= 0) {
            build = core.substring(plus + 1);
            core = core.substring(0, plus);
        }
        int dash = core.indexOf('-');
        if (dash >= 0) {
            prerelease = core.substring(dash + 1);
            core = core.substring(0, dash);
        }

        if (core.isBlank()) {
            throw new VersionParsingException("Missing semantic version core: " + value);
        }

        String[] pieces = core.split("\\.");
        List<Integer> parsed = new ArrayList<>(pieces.length);
        boolean wildcard = false;
        for (String piece : pieces) {
            if (piece.equals("x") || piece.equals("X") || piece.equals("*")) {
                parsed.add(COMPONENT_WILDCARD);
                wildcard = true;
                continue;
            }
            try {
                parsed.add(Integer.parseInt(piece));
            } catch (NumberFormatException e) {
                throw new VersionParsingException("Invalid semantic version component '" + piece + "' in " + value, e);
            }
        }

        int[] components = parsed.stream().mapToInt(Integer::intValue).toArray();
        return new SimpleSemanticVersion(original, components, prerelease, build, wildcard);
    }

    @Override
    public int getVersionComponentCount() {
        return components.length;
    }

    @Override
    public int getVersionComponent(int pos) {
        if (pos < components.length) return components[pos];
        return wildcard ? COMPONENT_WILDCARD : 0;
    }

    @Override
    public Optional<String> getPrereleaseKey() {
        return Optional.ofNullable(prerelease);
    }

    @Override
    public Optional<String> getBuildKey() {
        return Optional.ofNullable(build);
    }

    @Override
    public boolean hasWildcard() {
        return wildcard;
    }

    @Override
    public String getFriendlyString() {
        return original;
    }

    @Override
    public int compareTo(Version other) {
        if (!(other instanceof SemanticVersion semantic)) {
            return original.compareToIgnoreCase(other.getFriendlyString());
        }

        int count = Math.max(getVersionComponentCount(), semantic.getVersionComponentCount());
        for (int i = 0; i < count; i++) {
            int left = normalizeComponent(getVersionComponent(i));
            int right = normalizeComponent(semantic.getVersionComponent(i));
            int cmp = Integer.compare(left, right);
            if (cmp != 0) return cmp;
        }

        Optional<String> rightPre = semantic.getPrereleaseKey();
        if (prerelease == null && rightPre.isEmpty()) return 0;
        if (prerelease == null) return 1;
        if (rightPre.isEmpty()) return -1;
        return comparePrerelease(prerelease, rightPre.get());
    }

    private static int normalizeComponent(int component) {
        return component == COMPONENT_WILDCARD ? 0 : component;
    }

    private static int comparePrerelease(String left, String right) {
        String[] l = left.split("\\.");
        String[] r = right.split("\\.");
        for (int i = 0; i < Math.max(l.length, r.length); i++) {
            if (i >= l.length) return -1;
            if (i >= r.length) return 1;
            String a = l[i];
            String b = r[i];
            boolean aNumeric = a.chars().allMatch(Character::isDigit);
            boolean bNumeric = b.chars().allMatch(Character::isDigit);
            int cmp;
            if (aNumeric && bNumeric) cmp = Integer.compare(Integer.parseInt(a), Integer.parseInt(b));
            else if (aNumeric != bNumeric) cmp = aNumeric ? -1 : 1;
            else cmp = a.compareToIgnoreCase(b);
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    @Override
    public String toString() {
        return original;
    }
}
