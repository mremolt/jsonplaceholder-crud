package gmbh.tantive.jsonplaceholder_crud.dev;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.Base64;

public final class DevelopmentJwtSecret {

    private static final int SECRET_LENGTH_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private DevelopmentJwtSecret() {
    }

    public static String resolve(Path secretFile, String environmentSecret) throws IOException {
        if (environmentSecret != null && !environmentSecret.isBlank()) {
            return validate(environmentSecret.strip());
        }

        Path absoluteSecretFile = secretFile.toAbsolutePath().normalize();
        if (Files.exists(absoluteSecretFile)) {
            return validate(Files.readString(absoluteSecretFile, StandardCharsets.UTF_8).strip());
        }

        Files.createDirectories(absoluteSecretFile.getParent());
        byte[] randomBytes = new byte[SECRET_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        String generatedSecret = Base64.getEncoder().encodeToString(randomBytes);

        try {
            createOwnerOnlyFile(absoluteSecretFile);
        } catch (FileAlreadyExistsException exception) {
            return validate(Files.readString(absoluteSecretFile, StandardCharsets.UTF_8).strip());
        }
        try {
            Files.writeString(absoluteSecretFile, generatedSecret + System.lineSeparator(), StandardCharsets.UTF_8);
            return generatedSecret;
        } catch (IOException exception) {
            Files.deleteIfExists(absoluteSecretFile);
            throw exception;
        }
    }

    private static String validate(String secret) {
        final byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET must be Base64 encoded", exception);
        }
        if (decoded.length < SECRET_LENGTH_BYTES) {
            throw new IllegalStateException("JWT_SECRET must contain at least 256 bits");
        }
        return secret;
    }

    private static void createOwnerOnlyFile(Path path) throws IOException {
        try {
            Files.createFile(path, PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString("rw-------")));
        } catch (UnsupportedOperationException exception) {
            Files.createFile(path);
            File file = path.toFile();
            file.setReadable(false, false);
            file.setWritable(false, false);
            if (!file.setReadable(true, true) || !file.setWritable(true, true)) {
                Files.deleteIfExists(path);
                throw new IOException("Could not restrict permissions on local JWT secret file");
            }
        }
    }
}