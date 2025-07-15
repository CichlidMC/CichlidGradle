package fish.cichlidmc.cichlid_gradle.util.io;

import java.io.IOException;

public interface IoFunction<A, B> {
	B apply(A input) throws IOException;
}
