package ph.trackph.milestone.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileStorageServiceTest {
    @Test
    void seededProofReadsTheBundledPlaceholder(@TempDir Path dir) throws IOException {
        byte[] bytes = new FileStorageService(dir.toString()).read("seed-proof-42.pdf");

        assertEquals("%PDF", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
    }

    @Test
    void missingRealFileStillFails(@TempDir Path dir) throws IOException {
        FileStorageService storage = new FileStorageService(dir.toString());

        assertThrows(IOException.class, () -> storage.read("nope.pdf"));
    }
}
