package fish.cichlidmc.cichlid_gradle.util;

import fish.cichlidmc.pistonmetaparser.util.Downloadable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public record Download(Downloadable downloadable, Path dest) implements Runnable {
    @Override
    public void run() {
        MessageDigest messageDigest = Hashes.createDigest("SHA-1");
        try (AtomicFile file = new AtomicFile(dest)) {
            try (DigestInputStream stream = new DigestInputStream(FileUtils.openDownloadStream(downloadable), messageDigest)) {
                Files.copy(stream, file.temp(), StandardCopyOption.REPLACE_EXISTING);
            }

            if (!Hashes.format(messageDigest.digest()).equals(downloadable.sha1())) {
                throw new RuntimeException("Downloaded file did not match expected hash of " + downloadable.sha1());
            }

            file.commit();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
