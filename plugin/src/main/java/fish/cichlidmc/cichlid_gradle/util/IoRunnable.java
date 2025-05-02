package fish.cichlidmc.cichlid_gradle.util;

import java.io.IOException;

public interface IoRunnable {
    void run() throws IOException;

    default Runnable asRunnable() {
        return () -> {
            try {
                this.run();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
