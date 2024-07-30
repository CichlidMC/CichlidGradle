package io.github.cichlidmc.cichlid_gradle.util;

import io.github.cichlidmc.cichlid_gradle.pistonmeta.util.Downloadable;
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

    public static void downloadSilently(Downloadable downloadable, Path dest) {
        download(downloadable, dest, false);
    }

    public static void download(Downloadable downloadable, Path dest) {
        download(downloadable, dest, true);
    }

    private static void download(Downloadable downloadable, Path dest, boolean loud) {
        HttpRequest request = HttpRequest.newBuilder(downloadable.url()).build();
        try {
            if (loud) {
                logger.lifecycle("Downloading {}...", dest.getFileName());
            }

            long start = System.currentTimeMillis();
            byte[] data = client.send(request, HttpResponse.BodyHandlers.ofByteArray()).body();

            if (loud) {
                long end = System.currentTimeMillis();
                String mb = String.format("%.3f", data.length / 1000f / 1000f);
                logger.lifecycle("Downloaded {}mb in {}ms.", mb, end - start);
            }

            // validate
            if (downloadable.size() != data.length) {
                throw new RuntimeException("Downloaded file did not match expected size of " + downloadable.size());
            }
            if (!sha1(data).equals(downloadable.sha1())) {
                throw new RuntimeException("Downloaded file did not match expected hash of " + downloadable.sha1());
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

    public static void ensureCreated(Path file) throws IOException {
        if (!Files.exists(file)) {
            // can't use open option, gradle breaks it
            Files.createDirectories(file.getParent());
            Files.createFile(file);
        }
    }
}
