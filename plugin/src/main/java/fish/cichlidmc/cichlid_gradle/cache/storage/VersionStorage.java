package fish.cichlidmc.cichlid_gradle.cache.storage;

import java.nio.file.Path;

public class VersionStorage {

	public final MappingsStorage mappings;
	public final JarsStorage jars;
	public final Path natives;
	public final RunTemplateStorage runs;

	public VersionStorage(Path root) {
		this.mappings = new MappingsStorage(root.resolve("mappings"));
		this.jars = new JarsStorage(root.resolve("jars"));
		this.natives = root.resolve("natives");
		this.runs = new RunTemplateStorage(root.resolve("runs"));
	}
}
