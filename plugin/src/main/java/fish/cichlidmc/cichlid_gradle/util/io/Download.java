package fish.cichlidmc.cichlid_gradle.util.io;

import fish.cichlidmc.cichlid_gradle.util.hash.Encoding;
import fish.cichlidmc.cichlid_gradle.util.hash.HashAlgorithm;
import fish.cichlidmc.pistonmetaparser.util.Downloadable;

import java.nio.channels.Channels;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;

public record Download(Downloadable downloadable, Path dest) implements Runnable {
    @Override
    public void run() {
        try (WorkFile file = WorkFile.claim(this.dest)) {
            MessageDigest digest = HashAlgorithm.SHA1.digest();

            try (DigestInputStream stream = new DigestInputStream(FileUtils.openDownloadStream(this.downloadable), digest)) {
                stream.transferTo(Channels.newOutputStream(file.channel));
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
