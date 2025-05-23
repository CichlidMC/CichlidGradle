package fish.cichlidmc.cichlid_gradle.cache.mcmaven;

import fish.cichlidmc.cichlid_gradle.cache.Artifact;
import fish.cichlidmc.cichlid_gradle.cache.CichlidCache;
import fish.cichlidmc.cichlid_gradle.cache.ManifestCache;
import fish.cichlidmc.cichlid_gradle.cache.Transformers;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;
import fish.cichlidmc.cichlid_gradle.cache.task.impl.AssetsTask;
import fish.cichlidmc.cichlid_gradle.cache.task.impl.DecompileTask;
import fish.cichlidmc.cichlid_gradle.cache.task.impl.ReassembleTask;
import fish.cichlidmc.cichlid_gradle.extension.def.MinecraftDefinition;
import fish.cichlidmc.cichlid_gradle.extension.def.MinecraftDefinitionImpl;
import fish.cichlidmc.cichlid_gradle.util.Distribution;
import fish.cichlidmc.pistonmetaparser.FullVersion;
import fish.cichlidmc.pistonmetaparser.manifest.Version;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Intercepts requests for Minecraft files from a simulated maven repo.
 * Provides Minecraft jars and pom files for each version.
 */
public final class MinecraftMaven {
	public static final String GROUP = "net.minecraft";
	public static final String MODULE = "minecraft";

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

	@Nullable
	public InputStream get(URI uri) throws IOException {
		Request request = extractRequest(uri);
		if (request == null)
			return null;

		MinecraftDefinitionImpl def = (MinecraftDefinitionImpl) this.defs.findByName(request.def);
		if (def == null) {
			throw new InvalidUserDataException("Minecraft definition '" + request.def + "' does not exist");
		}

		String version = def.getVersionOrThrow();

		Iterable<File> transformerFiles = def.resolvableTransformers().getIncoming().getFiles();
		Transformers transformers = new Transformers(transformerFiles, request.hash);

		return this.getArtifact(version, def.dist(), transformers, request);
	}

	@Nullable
	private InputStream getArtifact(String versionId, Distribution dist, Transformers transformers, Request request) throws IOException {
		// see if this version actually exists
		Version version = ManifestCache.getVersion(versionId);
		if (version == null)
			return null;

		FullVersion fullVersion = ManifestCache.expand(version);
		// and the dist. all versions have a client, check for server
		if (dist != Distribution.CLIENT && fullVersion.downloads.server.isEmpty())
			return null;

		if (fullVersion.downloads.clientMappings.isEmpty()) {
			throw new InvalidUserDataException("Versions pre-mojmap are not currently supported!");
		}

		if (request.artifact == Artifact.POM) {
			return this.getPom(fullVersion, dist, request);
		}

		boolean needsAssets = dist.needsAssets() && !this.cache.assets.isComplete(fullVersion.assetIndex);
		Path jar = this.cache.reassembledJars.get(versionId, transformers.hash(), dist, request.artifact);
		boolean needsJar = !Files.exists(jar);

		if (!needsAssets && !needsJar) {
			return Files.newInputStream(jar);
		}

		CacheTaskEnvironment.Builder builder = new CacheTaskEnvironment.Builder(fullVersion, this.cache, dist, transformers);

		if (needsAssets) {
			builder.add(AssetsTask::new);
		}

		if (needsJar) {
			builder.add(env -> new ReassembleTask(env, request.artifact == Artifact.SOURCES));
		}

		builder.start().report();

		// all tasks are done. If an exception wasn't thrown by report, everything was successful.
		return Files.newInputStream(jar);
	}

	private InputStream getPom(FullVersion version, Distribution dist, Request request) throws IOException {
		Path template = this.cache.pomTemplates.get(version.id, dist);
		if (!Files.exists(template)) {
			PomGenerator.generate(version, template);
		}

		// file should now exist
		String content = Files.readString(template);
		String filled = content.replace(PomGenerator.VERSION_PLACEHOLDER, request.gradleRequestedVersion());
		return new ByteArrayInputStream(filled.getBytes(StandardCharsets.UTF_8));
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

		// version is defName$hash, extract the name
		int separator = version1.indexOf('$');
		if (separator == -1)
			return null;

		String defName = version1.substring(0, separator);
		String hash = version1.substring(separator + 1);

		Artifact artifact = switch (matcher.group(3)) {
			case "jar" -> sources ? Artifact.SOURCES : Artifact.JAR;
			case "pom" -> Artifact.POM;
			default -> null;
		};

		if (artifact == null)
			return null;

		logger.quiet("Intercepted request for Minecraft definition {}, {}", defName, artifact);

		return new Request(defName, hash, artifact);
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

	private record Request(String def, String hash, Artifact artifact) {
		// the version specified in the pom needs to match what gradle requested exactly (defName$hash) or it'll be rejected
		private String gradleRequestedVersion() {
			return this.def + '$' + this.hash;
		}
	}
}
