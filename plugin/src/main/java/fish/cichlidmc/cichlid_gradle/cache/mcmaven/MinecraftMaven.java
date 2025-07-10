package fish.cichlidmc.cichlid_gradle.cache.mcmaven;

import fish.cichlidmc.cichlid_gradle.cache.Artifact;
import fish.cichlidmc.cichlid_gradle.cache.CichlidCache;
import fish.cichlidmc.cichlid_gradle.cache.ManifestCache;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;
import fish.cichlidmc.cichlid_gradle.cache.task.impl.AssetsTask;
import fish.cichlidmc.cichlid_gradle.cache.task.impl.ReassembleBinaryTask;
import fish.cichlidmc.cichlid_gradle.cache.task.impl.ReassembleSourcesTask;
import fish.cichlidmc.cichlid_gradle.extension.def.MinecraftDefinition;
import fish.cichlidmc.cichlid_gradle.extension.def.MinecraftDefinitionImpl;
import fish.cichlidmc.cichlid_gradle.extension.def.TransformersImpl;
import fish.cichlidmc.cichlid_gradle.util.Distribution;
import fish.cichlidmc.cichlid_gradle.util.hash.Encoding;
import fish.cichlidmc.cichlid_gradle.util.hash.HashAlgorithm;
import fish.cichlidmc.pistonmetaparser.FullVersion;
import fish.cichlidmc.pistonmetaparser.manifest.Version;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intercepts requests for Minecraft files from a simulated maven repo.
 * Provides Minecraft jars and pom files for each version.
 */
public final class MinecraftMaven {
	public static final String GROUP = "net.minecraft";
	public static final Collection<String> MODULES = Arrays.stream(Distribution.values())
			.map(dist -> "minecraft-" + dist.name)
			.toList();

	/**
	 * Regex for files that could possibly be provided.
	 */
	public static final Pattern VALID_FILE = Pattern.compile(
			// groups:				  |-         1        -| |2 |           |-         3        -| |4 |   |-  5  -||-           6            -|
			"/net/minecraft/minecraft-(client|server|merged)/(.+)/minecraft-(client|server|merged)-(.+)\\.(pom|jar)(.md5|.sha1|.sha256|.sha512)?"
	);

	private static final Logger logger = Logging.getLogger(MinecraftMaven.class);

	private final NamedDomainObjectContainer<MinecraftDefinition> defs;
	private final CichlidCache cache;

	public MinecraftMaven(NamedDomainObjectContainer<MinecraftDefinition> defs, CichlidCache cache) {
		this.defs = defs;
		this.cache = cache;
	}

	@Nullable
	public InputStream get(URI uri) throws IOException {
		Request request = extractRequest(uri);
		if (request == null)
			return null;

		MinecraftDefinitionImpl def = (MinecraftDefinitionImpl) this.defs.findByName(request.def);
		if (def == null) {
			throw new IllegalStateException("Minecraft definition '" + request.def + "' does not exist");
		}

		String version = def.version();
		Distribution dist = def.dist();

		// validate that the request matches
		if (request.dist != dist || !request.version.equals(version)) {
			throw new IllegalStateException("Malformed request for Minecraft definition " + request.def);
		}


		InputStream stream = this.getArtifact(version, dist, def.getTransformers(), request);
		if (stream == null || request.hashAlgorithm.isEmpty())
			return stream;

		HashAlgorithm hashAlgorithm = request.hashAlgorithm.get();
		String hash = Encoding.HEX.encode(hashAlgorithm.hash(stream.readAllBytes()));
		return new ByteArrayInputStream(hash.getBytes(StandardCharsets.UTF_8));
	}

	@Nullable
	private InputStream getArtifact(String versionId, Distribution dist, TransformersImpl transformers, Request request) throws IOException {
		// see if this version actually exists
		Version version = ManifestCache.getVersion(versionId);
		if (version == null) {
			logger.warn("Minecraft version does not exist: {}", versionId);
			return null;
		}

		FullVersion fullVersion = ManifestCache.expand(version);
		// and the dist. all versions have a client, check for server
		if (dist != Distribution.CLIENT && fullVersion.downloads.server.isEmpty()) {
			logger.warn("Minecraft version doesn't have a server, but it was requested: {}", versionId);
			return null;
		}

		if (fullVersion.downloads.clientMappings.isEmpty()) {
			throw new InvalidUserDataException("Versions pre-mojmap are not currently supported!");
		}

		if (request.artifact == Artifact.POM) {
			return this.getPom(fullVersion, dist, request);
		}

		boolean needsAssets = dist.needsAssets() && !this.cache.assets.isComplete(fullVersion.assetIndex);
		Path jar = this.cache.reassembledJars.get(versionId, request.hash, dist, request.artifact);
		boolean needsJar = !Files.exists(jar);

		if (!needsAssets && !needsJar) {
			return Files.newInputStream(jar);
		}

		CacheTaskEnvironment.Builder builder = new CacheTaskEnvironment.Builder(request.hash, fullVersion, this.cache, dist, transformers);

		if (needsAssets) {
			builder.add(AssetsTask::new);
		}

		if (needsJar) {
			switch (request.artifact) {
				case JAR -> builder.add(ReassembleBinaryTask::new);
				case SOURCES -> builder.add(ReassembleSourcesTask::new);
			}
		}

		builder.start().report();

		// all tasks are done. If an exception wasn't thrown by report, everything was successful.
		return Files.newInputStream(jar);
	}

	private InputStream getPom(FullVersion version, Distribution dist, Request request) throws IOException {
		Path template = this.cache.pomTemplates.get(version.id, dist);
		if (!Files.exists(template)) {
			PomGenerator.generate(version, dist, template);
		}

		// file should now exist
		String content = Files.readString(template);
		// of course & has special meaning in xml
		String escaped = request.gradleRequestedVersion().replace("&", "&amp;");
		String filled = content.replace(PomGenerator.VERSION_PLACEHOLDER, escaped);
		return new ByteArrayInputStream(filled.getBytes(StandardCharsets.UTF_8));
	}

	@Nullable
	private static Request extractRequest(URI uri) {
		Matcher matcher = VALID_FILE.matcher(uri.getPath());
		if (!matcher.matches())
			return null;

		// extract and verify dist
		String dist1 = matcher.group(1);
		String dist2 = matcher.group(3);
		if (!dist1.equals(dist2))
			return null;

		Distribution dist = Distribution.ofName(dist1).orElseThrow();

		// make sure both versions are the same
		String version1 = matcher.group(2);
		String version2 = matcher.group(4);

		// -sources will get caught in group 2 if present, check for it
		boolean sources = false;
		if (version2.endsWith("-sources")) {
			sources = true;
			version2 = version2.substring(0, version2.length() - "-sources".length());
		}

		if (!version1.equals(version2))
			return null;

		// version is version$def$hash, extract the name
		String[] split = version1.split("\\$");
		if (split.length != 3)
			return null;

		String version = split[0];
		String defName = split[1];
		String hash = split[2];

		Artifact artifact = switch (matcher.group(5)) {
			case "jar" -> sources ? Artifact.SOURCES : Artifact.JAR;
			case "pom" -> Artifact.POM;
			default -> null;
		};

		if (artifact == null)
			return null;

		HashAlgorithm hashAlgorithm;
		switch (matcher.group(6)) {
			case ".md5" -> hashAlgorithm = HashAlgorithm.MD5;
			case ".sha1" -> hashAlgorithm = HashAlgorithm.SHA1;
			case ".sha256" -> hashAlgorithm = HashAlgorithm.SHA256;
			case ".sha512" -> hashAlgorithm = HashAlgorithm.SHA512;
			case null -> hashAlgorithm = null;
			default -> {
				// when invalid, don't process it at all
				return null;
			}
		}

		logger.quiet("Intercepted request for Minecraft definition {}, {}, {}", defName, artifact, hashAlgorithm);

		return new Request(dist, version1, version, defName, hash, artifact, Optional.ofNullable(hashAlgorithm));
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

	private record Request(Distribution dist, String gradleRequestedVersion, String version, String def,
						   String hash, Artifact artifact, Optional<HashAlgorithm> hashAlgorithm) {
	}
}
