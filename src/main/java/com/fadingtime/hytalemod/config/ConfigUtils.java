package com.fadingtime.hytalemod.config;

import org.bson.BsonDocument;
import org.bson.BsonValue;

/**
 * Shared config reading utilities used by all BSON-based config loaders.
 *
 * JSONC SUPPORT: stripJsonComments() removes // line comments and block comments
 * from JSON text before parsing, since neither BsonDocument.parse() nor Gson
 * support comments natively. This lets config files include inline documentation
 * for server operators.
 *
 * BSON HELPERS: The readInt/readFloat/readLong/readDouble/readString methods were
 * duplicated identically in LifeEssenceConfig and ProjectileConfig. Extracting
 * them here follows DRY — one implementation to maintain.
 */
public final class ConfigUtils {
    private ConfigUtils() {
    }

    /**
     * Strips JSONC-style comments from JSON text.
     * Handles // line comments and block comments.
     * Comments inside quoted strings are preserved.
     */
    public static String stripJsonComments(String jsonc) {
        if (jsonc == null || jsonc.isEmpty()) {
            return jsonc;
        }
        StringBuilder result = new StringBuilder(jsonc.length());
        int len = jsonc.length();
        boolean inString = false;
        for (int i = 0; i < len; i++) {
            char c = jsonc.charAt(i);
            if (inString) {
                result.append(c);
                if (c == '\\' && i + 1 < len) {
                    result.append(jsonc.charAt(++i));
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                    result.append(c);
                } else if (c == '/' && i + 1 < len) {
                    char next = jsonc.charAt(i + 1);
                    if (next == '/') {
                        i = skipToEndOfLine(jsonc, i + 2);
                    } else if (next == '*') {
                        i = skipBlockComment(jsonc, i + 2);
                    } else {
                        result.append(c);
                    }
                } else {
                    result.append(c);
                }
            }
        }
        return result.toString();
    }

    private static int skipToEndOfLine(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            if (s.charAt(i) == '\n') {
                return i;
            }
        }
        return s.length() - 1;
    }

    private static int skipBlockComment(String s, int from) {
        for (int i = from; i < s.length() - 1; i++) {
            if (s.charAt(i) == '*' && s.charAt(i + 1) == '/') {
                return i + 1;
            }
        }
        return s.length() - 1;
    }

    /**
     * Returns a nested sub-document, or an empty document if the key is missing
     * or not a document. This lets callers read from nested config groups without
     * null checks — missing groups simply return defaults for every field.
     */
    public static BsonDocument getSubDocument(BsonDocument doc, String key) {
        BsonValue value = doc.get(key);
        if (value != null && value.isDocument()) {
            return value.asDocument();
        }
        return new BsonDocument();
    }

    public static int readInt(BsonDocument doc, String key, int defaultValue, int min) {
        BsonValue value = doc.get(key);
        if (value == null || !value.isNumber()) {
            return defaultValue;
        }
        return Math.max(min, value.asNumber().intValue());
    }

    public static long readLong(BsonDocument doc, String key, long defaultValue, long min) {
        BsonValue value = doc.get(key);
        if (value == null || !value.isNumber()) {
            return defaultValue;
        }
        return Math.max(min, value.asNumber().longValue());
    }

    public static float readFloat(BsonDocument doc, String key, float defaultValue, float min) {
        BsonValue value = doc.get(key);
        if (value == null || !value.isNumber()) {
            return defaultValue;
        }
        float parsed = (float) value.asNumber().doubleValue();
        if (!Float.isFinite(parsed)) {
            return defaultValue;
        }
        return Math.max(min, parsed);
    }

    public static float readFloatUnbounded(BsonDocument doc, String key, float defaultValue) {
        BsonValue value = doc.get(key);
        if (value == null || !value.isNumber()) {
            return defaultValue;
        }
        float parsed = (float) value.asNumber().doubleValue();
        if (!Float.isFinite(parsed)) {
            return defaultValue;
        }
        return parsed;
    }

    public static double readDouble(BsonDocument doc, String key, double defaultValue, double min) {
        BsonValue value = doc.get(key);
        if (value == null || !value.isNumber()) {
            return defaultValue;
        }
        double parsed = value.asNumber().doubleValue();
        if (!Double.isFinite(parsed)) {
            return defaultValue;
        }
        return Math.max(min, parsed);
    }

    public static String readString(BsonDocument doc, String key, String defaultValue) {
        BsonValue value = doc.get(key);
        if (value == null || !value.isString()) {
            return defaultValue;
        }
        String parsed = value.asString().getValue();
        if (parsed == null || parsed.isBlank()) {
            return defaultValue;
        }
        return parsed.trim();
    }
}
