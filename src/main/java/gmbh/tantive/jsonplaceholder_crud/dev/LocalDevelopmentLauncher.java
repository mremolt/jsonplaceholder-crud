package gmbh.tantive.jsonplaceholder_crud.dev;

import java.io.IOException;
import java.nio.file.Path;

import gmbh.tantive.jsonplaceholder_crud.JsonplaceholderCrudApplication;

public final class LocalDevelopmentLauncher {

    private LocalDevelopmentLauncher() {
    }

    public static void main(String[] args) throws IOException {
        String configuredSecret = System.getenv("JWT_SECRET");
        if (configuredSecret == null || configuredSecret.isBlank()) {
            configuredSecret = System.getProperty("JWT_SECRET");
        }
        String secret = DevelopmentJwtSecret.resolve(Path.of(".jwt-secret"), configuredSecret);
        System.setProperty("JWT_SECRET", secret);
        JsonplaceholderCrudApplication.main(args);
    }
}