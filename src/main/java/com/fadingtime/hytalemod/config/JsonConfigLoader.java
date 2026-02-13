/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bson.BsonDocument
 *  org.bson.BsonInvalidOperationException
 *  org.bson.BsonSerializationException
 *  org.bson.json.JsonParseException
 */
package com.fadingtime.hytalemod.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bson.BsonDocument;
import org.bson.BsonInvalidOperationException;
import org.bson.BsonSerializationException;
import org.bson.json.JsonParseException;

public final class JsonConfigLoader {
    private JsonConfigLoader() {
    }

    public static BsonDocument loadDocument(Path path, String string, String string2, Logger logger) {
        Path path2 = JsonConfigLoader.resolveExternalPath(path, string);
        String string3 = JsonConfigLoader.readBundledJson(string2, logger);
        String string4 = JsonConfigLoader.readExternalJson(path2, logger);
        if (string4 != null) {
            BsonDocument bsonDocument;
            BsonDocument bsonDocument2 = JsonConfigLoader.tryParse(string4, "external config " + String.valueOf(path2), logger);
            if (bsonDocument2 != null) {
                return bsonDocument2;
            }
            if (string3 != null && (bsonDocument = JsonConfigLoader.tryParse(string3, "bundled config " + string2, logger)) != null) {
                return bsonDocument;
            }
            return new BsonDocument();
        }
        if (string3 != null) {
            JsonConfigLoader.tryWriteDefaultExternal(path2, string3, logger);
            BsonDocument bsonDocument = JsonConfigLoader.tryParse(string3, "bundled config " + string2, logger);
            if (bsonDocument != null) {
                return bsonDocument;
            }
        }
        return new BsonDocument();
    }

    private static Path resolveExternalPath(Path path, String string) {
        Path path2 = path != null ? path.getParent() : null;
        Path path3 = path2 != null ? path2 : Path.of(".", new String[0]);
        return path3.resolve("config").resolve(string);
    }

    private static String readExternalJson(Path path, Logger logger) {
        if (!Files.isRegularFile(path, new LinkOption[0])) {
            return null;
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        catch (IOException iOException) {
            logger.log(Level.WARNING, "Failed to read config file " + String.valueOf(path), iOException);
            return null;
        }
    }

    /*
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static String readBundledJson(String string, Logger logger) {
        try (InputStream inputStream = JsonConfigLoader.class.getClassLoader().getResourceAsStream(string);){
            if (inputStream == null) {
                logger.log(Level.WARNING, "Bundled config not found at " + string);
                String string3 = null;
                return string3;
            }
            String string2 = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            return string2;
        }
        catch (IOException iOException) {
            logger.log(Level.WARNING, "Failed to read bundled config " + string, iOException);
            return null;
        }
    }

    private static void tryWriteDefaultExternal(Path path, String string, Logger logger) {
        try {
            Path path2 = path.getParent();
            if (path2 != null) {
                Files.createDirectories(path2, new FileAttribute[0]);
            }
            Files.writeString(path, (CharSequence)string, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        }
        catch (IOException iOException) {
        }
    }

    private static BsonDocument tryParse(String string, String string2, Logger logger) {
        try {
            return BsonDocument.parse((String)string);
        }
        catch (BsonInvalidOperationException | BsonSerializationException | JsonParseException throwable) {
            logger.log(Level.WARNING, "Failed to parse " + string2, throwable);
            return null;
        }
    }
}
