package fish.cichlidmc.cichlid_gradle.cache.mcmaven;

import fish.cichlidmc.cichlid_gradle.cache.CichlidCache;
import fish.cichlidmc.cichlid_gradle.cache.storage.JarsStorage;
import fish.cichlidmc.cichlid_gradle.extension.def.MinecraftDefinition;
import fish.cichlidmc.cichlid_gradle.extension.def.MinecraftDefinitionImpl;
import fish.cichlidmc.cichlid_gradle.util.Distribution;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intercepts requests for Minecraft files from a simulated maven repo.
 * Provides Minecraft jars and pom files for each version.
 */
public final class MinecraftMaven {
	/**
	 * Regex for files that could possibly be provided.
	 */
	public static final Pattern VALID_FILE = Pattern.compile(
			// groups:				  |1 |           |2 |   |-  3  -|
			"/net/minecraft/minecraft/(.+)/minecraft-(.+)\\.(pom|jar)"
	);

	private static final Logger logger = Logging.getLogger(MinecraftMaven.class);

	private final NamedDomainObjectContainer<MinecraftDefinition> defs;
	private final CichlidCache cache;

	public MinecraftMaven(NamedDomainObjectContainer<MinecraftDefinition> defs, CichlidCache cache) {
		this.defs = defs;
		this.cache = cache;
	}

	/**
	 * Find a path corresponding to the file at the requested location.
	 * Returns null if one does not exist.
	 */
	@Nullable
	public Path getFile(URI uri) {
		Request request = extractRequest(uri);
		if (request == null)
			return null;

		MinecraftDefinitionImpl def = (MinecraftDefinitionImpl) this.defs.findByName(request.def);
		if (def == null)
			return null;

		String version = def.getVersionOrThrow();

		System.out.println("resolving transformers");

		for (File file : def.resolvableTransformers().getIncoming().getFiles()) {
			logger.quiet("Transformer: {}", file);
		}

		this.cache.ensureVersionIsCached(version);
		JarsStorage storage = this.cache.getVersion(version).jars;

		Distribution dist = def.getDistribution().get();

		Path path = switch (request.type) {
			case JAR -> storage.path(dist);
			case SOURCES -> storage.sources(dist);
			case POM -> storage.metadata(dist);
		};

		return Files.exists(path) ? path : null;
	}

	@Nullable
	private static Request extractRequest(URI uri) {
		Matcher matcher = VALID_FILE.matcher(uri.getPath());
		if (!matcher.matches())
			return null;

		// make sure both versions are the same
		String version1 = matcher.group(1);
		String version2 = matcher.group(2);

		// -sources will get caught in group 2 if present, check for it
		boolean sources = false;
		if (version2.endsWith("-sources")) {
			sources = true;
			version2 = version2.substring(0, version2.length() - "-sources".length());
		}

		if (!version1.equals(version2))
			return null;

		// version is defName_hash, extract the name
		int underscore = version1.indexOf('_');
		if (underscore == -1)
			return null;

		String defName = version1.substring(0, underscore);

		Request.Type type = switch (matcher.group(3)) {
			case "jar" -> sources ? Request.Type.SOURCES : Request.Type.JAR;
			case "pom" -> Request.Type.POM;
			default -> throw new RuntimeException("Invalid Type");
		};

		logger.quiet("Intercepted request for Minecraft definition {}, {}", defName, type);

		return new Request(defName, type);
	}

	public static String createProtocol(Project project) {
		if (project.getParent() == null)
			return "mcmaven";

		StringBuilder builder = new StringBuilder("mcmaven-");
		// :path:to:project
		String path = project.getPath();
		// start at 1 to skip leading :
		for (int i = 1; i < path.length(); i++) {
			builder.append(filterChar(path.charAt(i)));
		}

		return builder.toString();
	}

	private static char filterChar(char c) {
		//     a-z                       A-Z                       0-9                       special cases: + - .
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '+' || c == '-' || c == '.') ? c : '-';
	}

	private record Request(String def, Type type) {
		private enum Type {
			JAR, SOURCES, POM
		}
	}
}
