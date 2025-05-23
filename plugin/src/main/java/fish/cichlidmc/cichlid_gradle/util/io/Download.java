package fish.cichlidmc.cichlid_gradle.util.io;

import fish.cichlidmc.cichlid_gradle.util.hash.Encoding;
import fish.cichlidmc.cichlid_gradle.util.hash.HashAlgorithm;
import fish.cichlidmc.pistonmetaparser.util.Downloadable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public record Download(Downloadable downloadable, Path dest) implements Runnable {
    @Override
    public void run() {
        try (AtomicFile file = new AtomicFile(this.dest)) {
            MessageDigest digest = HashAlgorithm.SHA1.digest();

            try (DigestInputStream stream = new DigestInputStream(FileUtils.openDownloadStream(this.downloadable), digest)) {
                Files.copy(stream, file.temp(), StandardCopyOption.REPLACE_EXISTING);
            }

            if (!Encoding.HEX.encode(digest.digest()).equals(this.downloadable.sha1())) {
                throw new RuntimeException("Downloaded file did not match expected hash of " + this.downloadable.sha1());
            }

            file.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
