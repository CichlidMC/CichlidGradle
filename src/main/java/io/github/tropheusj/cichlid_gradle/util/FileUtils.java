package io.github.tropheusj.cichlid_gradle.util;

import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

public class FileUtils {
    private static final Logger logger = Logging.getLogger(FileUtils.class);
    private static final HttpClient client = HttpClient.newBuilder().build();

    public static void download(Downloadable download, Path dest) {
        HttpRequest request = HttpRequest.newBuilder(download.url()).build();
        try {
            logger.lifecycle("Downloading {}...", dest.getFileName());
            long start = System.currentTimeMillis();
            byte[] data = client.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();
            long end = System.currentTimeMillis();
            String mb = String.format("%.3f", data.length / 1000f / 1000f);
            logger.lifecycle("Downloaded {}mb in {}ms.", mb, end - start);
            // validate
            if (download.size() != data.length) {
                throw new RuntimeException("Downloaded file did not match expected size of " + download.size());
            }
            if (!sha1(data).equals(download.sha1())) {
                throw new RuntimeException("Downloaded file did not match expected hash of " + download.sha1());
            }
            // valid
            Files.createDirectories(dest.getParent());
            Files.createFile(dest);
            Files.write(dest, data);
        } catch (InterruptedException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String sha1(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(data);
            Formatter formatter = new Formatter();
            for (byte b : hash) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String sha1(Path file) {
        try {
            return sha1(Files.readAllBytes(file));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
