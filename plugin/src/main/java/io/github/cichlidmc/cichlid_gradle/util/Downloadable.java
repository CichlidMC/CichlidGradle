package io.github.cichlidmc.cichlid_gradle.util;

import java.net.URI;

public interface Downloadable {
    URI url();
    int size();
    String sha1();
}
