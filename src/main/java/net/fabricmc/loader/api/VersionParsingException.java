package net.fabricmc.loader.api;

/**
 * Minimal Fabric Loader API compatibility exception used by Carpet's version
 * checks. This is intentionally source-compatible with the API shape Carpet uses.
 */
public class VersionParsingException extends Exception {
    public VersionParsingException(String message) {
        super(message);
    }

    public VersionParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
