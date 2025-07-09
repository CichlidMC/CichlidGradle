package fish.cichlidmc.cichlid_gradle.cache.task.processor;

import fish.cichlidmc.cichlid_gradle.util.hash.Encoding;
import fish.cichlidmc.cichlid_gradle.util.hash.HashAlgorithm;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A grouping of classes as seen in source code. An outer class and a list of inner ones.
 */
public record ClassGroup(ClassEntry main, Collection<ClassEntry> inner) {
	public ClassGroup(ClassEntry main) {
		this(main, List.of());
	}

	public String hash() throws IOException {
		MessageDigest digest = HashAlgorithm.SHA256.digest();

		List<ClassEntry.Content> contents = this.inner.stream()
				.sorted(Comparator.comparing(ClassEntry::fileName))
				.map(ClassEntry::content)
				// explicitly collect into a mutable list
				.collect(Collectors.toCollection(ArrayList::new));

		contents.addFirst(this.main.content());

		for (ClassEntry.Content content : contents) {
			digest.update(content.bytes());
		}

		return Encoding.BASE_FUNNY.encode(digest.digest());
	}
}
