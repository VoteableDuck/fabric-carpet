package net.fabricmc.api;

/**
 * Compatibility surface retained so CarpetRulePrinter can stay identical to
 * upstream Carpet. NeoForge invokes it from CarpetNeoForge on dedicated servers.
 */
@FunctionalInterface
public interface DedicatedServerModInitializer {
    void onInitializeServer();
}
