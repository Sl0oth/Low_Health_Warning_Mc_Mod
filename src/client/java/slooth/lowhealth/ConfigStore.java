package slooth.lowhealth;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Compatible with the published 1.2.0 JSON schema, with atomic writes and broken-file backups. */
public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private ConfigStore() {}

    public static WarningConfig load(Path path) throws IOException {
        if (!Files.exists(path)) return new WarningConfig();
        try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            WarningConfig result = GSON.fromJson(reader, WarningConfig.class);
            if (result == null) throw new JsonParseException("Empty config");
            return result.normalized();
        } catch (JsonParseException | IllegalStateException e) {
            Path backup = Files.createTempFile(path.toAbsolutePath().getParent(), "lowhealth_config.broken-", ".json");
            Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            throw new IOException("Invalid config; original backed up to " + backup, e);
        }
    }

    public static void save(Path path, WarningConfig config) throws IOException {
        Path target = path.toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path temp = Files.createTempFile(target.getParent(), "lowhealth-", ".tmp");
        try {
            Files.writeString(temp, GSON.toJson(config.copy().normalized()), StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
