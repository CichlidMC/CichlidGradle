package io.github.cichlidmc.cichlid_gradle.cache.mcmaven;

import io.github.cichlidmc.cichlid_gradle.cache.CichlidCache;
import io.github.cichlidmc.cichlid_gradle.cache.storage.JarsStorage;
import io.github.cichlidmc.cichlid_gradle.util.Distribution;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.Nullable;

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
	public static final String PROTOCOL = "mcmaven";
	public static final URI ROOT = URI.create(PROTOCOL + ":///");
	/**
	 * Regex for files that could possibly be provided.
	 */
	public static final Pattern VALID_FILE = Pattern.compile(
			// groups:				  |-           1              -| |2 |           |-           3              -| |4 |   |-  5  -|
			"/net/minecraft/minecraft-(client|server|merged|bundler)/(.+)/minecraft-(client|server|merged|bundler)-(.+)\\.(pom|jar)"
	);

	private static final Logger logger = Logging.getLogger(MinecraftMaven.class);

	private final CichlidCache cache;

	public MinecraftMaven(CichlidCache cache) {
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

		this.cache.ensureVersionIsCached(request.version);
		JarsStorage storage = this.cache.getVersion(request.version).jars;
		Path path = switch (request.type) {
			case JAR -> storage.path(request.dist);
			case SOURCES -> storage.sources(request.dist);
			case POM -> storage.metadata(request.dist);
		};

		return Files.exists(path) ? path : null;
	}

	@Nullable
	private static Request extractRequest(URI uri) {
		Matcher matcher = VALID_FILE.matcher(uri.getPath());
		if (!matcher.matches())
			return null;

		// check that both distributions are the same
		String dist1 = matcher.group(1);
		String dist2 = matcher.group(3);
		if (!dist1.equals(dist2))
			return null;

		Distribution dist = Distribution.ofName(dist1).orElseThrow();

		// make sure the versions match too
		String version1 = matcher.group(2);
		String version2 = matcher.group(4);

		// -sources will get caught in group 4 if present, check for it
		boolean sources = false;
		if (version2.endsWith("-sources")) {
			sources = true;
			version2 = version2.substring(0, version2.length() - "-sources".length());
		}

		if (!version1.equals(version2))
			return null;

		Request.Type type = switch (matcher.group(5)) {
			case "jar" -> sources ? Request.Type.SOURCES : Request.Type.JAR;
			case "pom" -> Request.Type.POM;
			default -> throw new RuntimeException("Invalid Type");
		};

		logger.debug("Intercepted request for Minecraft {} {}, {}", dist, version1, type);

		return new Request(dist, version1, type);
	}

	private record Request(Distribution dist, String version, Type type) {
		private enum Type {
			JAR, SOURCES, POM
		}
	}
}
