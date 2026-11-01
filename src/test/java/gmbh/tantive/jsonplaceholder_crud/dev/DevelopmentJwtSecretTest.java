package gmbh.tantive.jsonplaceholder_crud.dev;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DevelopmentJwtSecretTest {

    @TempDir
    Path tempDirectory;

    @Test
    void createsAndReusesARandomLocalSecret() throws Exception {
        Path secretFile = tempDirectory.resolve(".jwt-secret");

        String generated = DevelopmentJwtSecret.resolve(secretFile, null);

        assertEquals(32, Base64.getDecoder().decode(generated).length);
        assertEquals(generated, Files.readString(secretFile).strip());
        assertEquals(generated, DevelopmentJwtSecret.resolve(secretFile, null));
    }

    @Test
    void usesEnvironmentSecretWithoutPersistingIt() throws Exception {
        String environmentSecret = Base64.getEncoder().encodeToString(new byte[32]);
        Path secretFile = tempDirectory.resolve(".jwt-secret");

        assertEquals(environmentSecret, DevelopmentJwtSecret.resolve(secretFile, environmentSecret));
        assertFalse(Files.exists(secretFile));
    }

    @Test
    void rejectsAWeakExistingSecret() throws Exception {
        Path secretFile = tempDirectory.resolve(".jwt-secret");
        Files.writeString(secretFile, Base64.getEncoder().encodeToString(new byte[16]));

        assertThrows(IllegalStateException.class, () -> DevelopmentJwtSecret.resolve(secretFile, null));
    }
}