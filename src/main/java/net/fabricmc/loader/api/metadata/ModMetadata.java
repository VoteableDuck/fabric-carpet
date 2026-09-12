package net.fabricmc.loader.api.metadata;

import net.fabricmc.loader.api.Version;

/**
 * Minimal metadata view required by Carpet's existing loader integration.
 */
public final class ModMetadata {
    private final String id;
    private final Version version;

    public ModMetadata(String id, Version version) {
        this.id = id;
        this.version = version;
    }

    public String getId() {
        return id;
    }

    public Version getVersion() {
        return version;
    }
}
