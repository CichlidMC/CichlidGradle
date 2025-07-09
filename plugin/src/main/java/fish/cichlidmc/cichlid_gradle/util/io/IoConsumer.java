package fish.cichlidmc.cichlid_gradle.util.io;

import java.io.IOException;

public interface IoConsumer<T> {
	void accept(T value) throws IOException;
}
