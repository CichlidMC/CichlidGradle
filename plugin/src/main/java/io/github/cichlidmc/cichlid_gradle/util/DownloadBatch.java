package io.github.cichlidmc.cichlid_gradle.util;

import io.github.cichlidmc.pistonmetaparser.util.Downloadable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public record DownloadBatch(List<Download> downloads) {
    public void execute() {
        CompletableFuture<?>[] futures = this.downloads.stream()
                .map(CompletableFuture::runAsync)
                .toArray(CompletableFuture[]::new);
        CompletableFuture.allOf(futures).join();
    }

    public static final class Builder {
        private final List<Download> downloads = new ArrayList<>();

        public Builder download(Downloadable downloadable, Path dest) {
            this.downloads.add(new Download(downloadable, dest));
            return this;
        }

        public DownloadBatch build() {
            return new DownloadBatch(downloads);
        }
    }
}
