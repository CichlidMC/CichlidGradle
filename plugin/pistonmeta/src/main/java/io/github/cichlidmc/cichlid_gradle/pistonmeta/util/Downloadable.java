package io.github.cichlidmc.cichlid_gradle.pistonmeta.util;

import java.net.URI;

public interface Downloadable {
    URI url();
    int size();
    String sha1();
}
