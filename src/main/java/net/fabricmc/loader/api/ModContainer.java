package net.fabricmc.loader.api;

import net.fabricmc.loader.api.metadata.ModMetadata;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * Fabric ModContainer compatibility view backed by NeoForge's IModInfo.
 */
public final class ModContainer {
    private final ModMetadata metadata;

    ModContainer(IModInfo info) {
        Version version;
        try {
            version = SemanticVersion.parse(info.getVersion().toString());
        } catch (VersionParsingException e) {
            version = new StringVersion(info.getVersion().toString());
        }
        this.metadata = new ModMetadata(info.getModId(), version);
    }

    public ModMetadata getMetadata() {
        return metadata;
    }
}
