package net.fabricmc.loader.api.metadata.version;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import net.fabricmc.loader.api.SemanticVersion;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

/**
 * Compact implementation of the Fabric VersionPredicate API used by Scarpet's
 * 'requires' declaration. Supports exact versions, comparison operators,
 * whitespace/comma AND, || OR, wildcards, tilde and caret ranges.
 */
public final class VersionPredicate implements Predicate<Version> {
    private final List<List<Predicate<Version>>> alternatives;

    private VersionPredicate(List<List<Predicate<Version>>> alternatives) {
        this.alternatives = alternatives;
    }

    public static VersionPredicate parse(String expression) throws VersionParsingException {
        if (expression == null || expression.isBlank() || expression.trim().equals("*")) {
            return new VersionPredicate(List.of(List.of(version -> true)));
        }

        List<List<Predicate<Version>>> alternatives = new ArrayList<>();
        for (String alternative : expression.split("\\|\\|")) {
            String normalized = alternative.trim().replace(',', ' ');
            if (normalized.isEmpty()) continue;
            List<Predicate<Version>> terms = new ArrayList<>();
            for (String token : normalized.split("\\s+")) {
                if (!token.isBlank()) terms.add(parseTerm(token));
            }
            if (!terms.isEmpty()) alternatives.add(terms);
        }
        if (alternatives.isEmpty()) {
            throw new VersionParsingException("Invalid version predicate: " + expression);
        }
        return new VersionPredicate(alternatives);
    }

    private static Predicate<Version> parseTerm(String token) throws VersionParsingException {
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

        if (raw.contains("*") || raw.contains("x") || raw.contains("X")) {
            SemanticVersion pattern = SemanticVersion.parse(raw);
            return version -> wildcardMatches(version, pattern);
        }

        SemanticVersion target = SemanticVersion.parse(raw);
        return switch (operator) {
            case ">=" -> version -> version.compareTo(target) >= 0;
            case "<=" -> version -> version.compareTo(target) <= 0;
            case ">" -> version -> version.compareTo(target) > 0;
            case "<" -> version -> version.compareTo(target) < 0;
            case "~" -> bounded(target, tildeUpperBound(target));
            case "^" -> bounded(target, caretUpperBound(target));
            case "=", "==" -> version -> version.compareTo(target) == 0;
            default -> throw new VersionParsingException("Unsupported version predicate operator: " + operator);
        };
    }

    private static Predicate<Version> bounded(SemanticVersion lower, SemanticVersion upper) {
        return version -> version.compareTo(lower) >= 0 && version.compareTo(upper) < 0;
    }

    private static SemanticVersion tildeUpperBound(SemanticVersion target) throws VersionParsingException {
        int major = target.getVersionComponent(0);
        int minor = target.getVersionComponentCount() > 1 ? target.getVersionComponent(1) : 0;
        if (target.getVersionComponentCount() <= 1) return SemanticVersion.parse((major + 1) + ".0.0");
        return SemanticVersion.parse(major + "." + (minor + 1) + ".0");
    }

    private static SemanticVersion caretUpperBound(SemanticVersion target) throws VersionParsingException {
        int major = target.getVersionComponent(0);
        int minor = target.getVersionComponentCount() > 1 ? target.getVersionComponent(1) : 0;
        int patch = target.getVersionComponentCount() > 2 ? target.getVersionComponent(2) : 0;
        if (major > 0) return SemanticVersion.parse((major + 1) + ".0.0");
        if (minor > 0) return SemanticVersion.parse("0." + (minor + 1) + ".0");
        return SemanticVersion.parse("0.0." + (patch + 1));
    }

    private static boolean wildcardMatches(Version version, SemanticVersion pattern) {
        if (!(version instanceof SemanticVersion semantic)) return false;
        int count = pattern.getVersionComponentCount();
        for (int i = 0; i < count; i++) {
            int expected = pattern.getVersionComponent(i);
            if (expected == SemanticVersion.COMPONENT_WILDCARD) return true;
            if (semantic.getVersionComponent(i) != expected) return false;
        }
        return true;
    }

    @Override
    public boolean test(Version version) {
        for (List<Predicate<Version>> alternative : alternatives) {
            boolean matches = true;
            for (Predicate<Version> predicate : alternative) {
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
