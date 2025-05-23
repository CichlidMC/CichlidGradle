package fish.cichlidmc.cichlid_gradle.cache;

import java.io.File;

public record Transformers(Iterable<File> files, String hash) {
}
