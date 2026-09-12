package net.fabricmc.loader.api;

/**
 * Minimal Fabric Loader Version API used by Carpet.
 */
public interface Version extends Comparable<Version> {
    String getFriendlyString();

    static Version parse(String value) throws VersionParsingException {
        try {
            return SemanticVersion.parse(value);
        } catch (VersionParsingException ignored) {
            if (value == null || value.isBlank()) {
                throw new VersionParsingException("Version cannot be blank");
            }
            return new StringVersion(value);
        }
    }
}

final class StringVersion implements Version {
    private final String value;

    StringVersion(String value) {
        this.value = value;
    }

    @Override
    public String getFriendlyString() {
        return value;
    }

    @Override
    public int compareTo(Version other) {
        return value.compareToIgnoreCase(other.getFriendlyString());
    }

    @Override
    public String toString() {
        return value;
    }
}
