package fish.cichlidmc.cichlid_gradle.util.io;

import java.io.FilterOutputStream;
import java.io.OutputStream;

public final class NonCloseableOutputStream extends FilterOutputStream {
	public NonCloseableOutputStream(OutputStream out) {
		super(out);
	}

	@Override
	public void close() {
	}
}
