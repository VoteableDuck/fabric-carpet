package net.fabricmc.loader.api;

import net.fabricmc.loader.api.metadata.ModMetadata;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * Fabric ModContainer compatibility view backed by NeoForge's IModInfo.
 */
public final class ModContainer {
    private final ModMetadata metadata;

    ModContainer(IModInfo info) {
        this(info.getModId(), info.getVersion().toString());
    }

    /**
     * Creates the small metadata view Carpet needs before NeoForge has exposed
     * the completed ModList. This is intentionally package-private and only
     * used by the FabricLoader compatibility facade during early bootstrap.
     */
    ModContainer(String modId, String versionString) {
        Version version;
        try {
            version = SemanticVersion.parse(versionString);
        } catch (VersionParsingException e) {
            version = new StringVersion(versionString);
        }
        this.metadata = new ModMetadata(modId, version);
    }

    public ModMetadata getMetadata() {
        return metadata;
    }
}
