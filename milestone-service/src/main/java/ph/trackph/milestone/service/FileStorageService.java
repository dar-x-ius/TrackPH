package ph.trackph.milestone.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class FileStorageService {
    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);
    private static final String SEED_PROOF_PREFIX = "seed-proof-";
    private final Path root;

    public String store(MultipartFile file, String extension) throws IOException {
        String storedFilename = UUID.randomUUID() + "." + extension;
        file.transferTo(root.resolve(storedFilename));
        return storedFilename;
    }

    public byte[] read(String storedFilename) throws IOException {
        // Seeded demo rows (migration V13) point at files that were never uploaded, so they all share one bundled PDF.
        if (storedFilename.startsWith(SEED_PROOF_PREFIX)) {
            try (InputStream in = FileStorageService.class.getResourceAsStream("/seed/placeholder-proof.pdf")) {
                if (in == null) throw new FileNotFoundException("seed/placeholder-proof.pdf is missing from the classpath");
                return in.readAllBytes();
            }
        }
        return Files.readAllBytes(root.resolve(storedFilename));
    }

    /**
     * Unused for now — attachment deletion is soft-delete only, so the normal delete flow
     * must NOT call this: the file has to survive as evidence on the audit trail. Keep this
     * around for a future orphan-cleanup job (files on disk with no matching attachment row
     * at all, e.g. a crash between store() and the DB save) — never for pruning soft-deleted
     * attachments.
     */
    public void delete(String storedFilename) {
        // ponytail: the DB row is the source of truth here, so a file that fails to delete
        // is just an orphan on disk, not a correctness problem. Upgrade to a retry/cleanup
        // job if orphaned files ever become enough to matter.
        try {
            Files.deleteIfExists(root.resolve(storedFilename));
        } catch (IOException e) {
            log.warn("Failed to delete attachment file {}: {}", storedFilename, e.getMessage());
        }
    }

    public FileStorageService(@Value("${trackph.attachments.dir}") final String dir) throws IOException {
        this.root = Path.of(dir);
        Files.createDirectories(this.root);
    }
}
