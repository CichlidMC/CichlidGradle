package io.github.cichlidmc.cichlid_gradle.pistonmeta;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.util.Downloadable;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.util.MoreCodecs;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.util.SystemInfo;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.util.SystemInfo.Architecture;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.util.SystemInfo.OperatingSystem;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.util.Utils;

public record FullVersion(
		Optional<SplitArguments> splitArgs, Optional<StringArguments> stringArgs, AssetIndex assetIndex, String assets,
		Optional<Integer> complianceLevel, Downloads downloads, String id, Optional<JavaVersion> javaVersion, List<Library> libraries,
		Optional<Logging> logging, String mainClass, Optional<Integer> minimumLauncherVersion, Date releaseTime, Date time,
		VersionType type) {
	public static final Codec<FullVersion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			SplitArguments.CODEC.optionalFieldOf("arguments").forGetter(FullVersion::splitArgs),
			StringArguments.CODEC.optionalFieldOf("minecraftArguments").forGetter(FullVersion::stringArgs),
			AssetIndex.CODEC.fieldOf("assetIndex").forGetter(FullVersion::assetIndex),
			Codec.STRING.fieldOf("assets").forGetter(FullVersion::assets),
			Codec.INT.optionalFieldOf("complianceLevel").forGetter(FullVersion::complianceLevel),
			Downloads.CODEC.fieldOf("downloads").forGetter(FullVersion::downloads),
			Codec.STRING.fieldOf("id").forGetter(FullVersion::id),
			JavaVersion.CODEC.optionalFieldOf("javaVersion").forGetter(FullVersion::javaVersion),
			Library.CODEC.listOf().fieldOf("libraries").forGetter(FullVersion::libraries),
			Logging.CODEC.optionalFieldOf("logging").forGetter(FullVersion::logging),
			Codec.STRING.fieldOf("mainClass").forGetter(FullVersion::mainClass),
			Codec.INT.optionalFieldOf("minimumLauncherVersion").forGetter(FullVersion::minimumLauncherVersion),
			MoreCodecs.ISO_DATE.fieldOf("releaseTime").forGetter(FullVersion::releaseTime),
			MoreCodecs.ISO_DATE.fieldOf("time").forGetter(FullVersion::time),
			VersionType.CODEC.fieldOf("type").forGetter(FullVersion::type)
	).apply(instance, FullVersion::new));

	public record Features(Map<String, Boolean> features) {
		public static final Codec<Features> CODEC = Codec.unboundedMap(Codec.STRING, Codec.BOOL).xmap(Features::new, Features::features);
		public static final Features EMPTY = new Features(Map.of());

		public boolean matches(Features present) {
			for (Entry<String, Boolean> entry : this.features.entrySet()) {
				String key = entry.getKey();
				boolean required = entry.getValue();

				if (!present.features.containsKey(key)) {
					// key is not present, if it's required to be true then fail
					if (required) {
						return false;
					}
				} else {
					// key is present, require that it matches
					boolean actual = present.features.get(key);
					if (required != actual)
						return false;
				}
			}

			return true;
		}
	}

	public record Os(Optional<OperatingSystem> os, Optional<Architecture> arch, Optional<Pattern> version) {
		public static final Codec<Os> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				OperatingSystem.CODEC.optionalFieldOf("name").forGetter(Os::os),
				Architecture.CODEC.optionalFieldOf("arch").forGetter(Os::arch),
				MoreCodecs.REGEX.optionalFieldOf("version").forGetter(Os::version)
		).apply(instance, Os::new));

		public boolean matches() {
			if (this.os.isPresent() && this.os.get() != OperatingSystem.CURRENT)
				return false;
			if (this.arch.isPresent() && this.arch.get() != Architecture.CURRENT)
				return false;
			return this.version.isEmpty() || this.version.get().matcher(SystemInfo.INSTANCE.osVersion()).matches();
		}
	}

	public record Rule(Action action, Optional<Features> features, Optional<Os> os) {
		public static final Codec<Rule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Action.CODEC.fieldOf("action").forGetter(Rule::action),
				Features.CODEC.optionalFieldOf("features").forGetter(Rule::features),
				Os.CODEC.optionalFieldOf("os").forGetter(Rule::os)
		).apply(instance, Rule::new));

		public boolean test(Features features) {
			return this.matches(features) ? this.action == Action.ALLOW : this.action == Action.DISALLOW;
		}

		private boolean matches(Features features) {
			if (this.features.isPresent() && !this.features.get().matches(features))
				return false;
			return this.os.isEmpty() || this.os.get().matches();
		}

		public enum Action {
			ALLOW, DISALLOW;

			public static final Codec<Action> CODEC = Codec.STRING.comapFlatMap(Action::of, Action::toString);

			@Override
			public String toString() {
				return this.name().toLowerCase(Locale.ROOT);
			}

			public static DataResult<Action> of(String s) {
				return switch (s) {
					case "allow" -> DataResult.success(ALLOW);
					case "disallow" -> DataResult.success(DISALLOW);
					default -> DataResult.error(() -> "No action named " + s);
				};
			}
		}
	}

	// >1.12
	public record SplitArguments(List<Argument> game, List<Argument> jvm) {
		public static final Codec<SplitArguments> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Argument.CODEC.listOf().fieldOf("game").forGetter(SplitArguments::game),
				Argument.CODEC.listOf().fieldOf("jvm").forGetter(SplitArguments::jvm)
		).apply(instance, SplitArguments::new));
	}

	// <=1.12
	public record StringArguments(String value) {
		public static final Codec<StringArguments> CODEC = Codec.STRING.xmap(StringArguments::new, StringArguments::value);
	}

	public record Argument(List<Rule> rules, List<String> values) {
		public static final Codec<Argument> CODEC = Codec.either(
				Codec.STRING,
				RecordCodecBuilder.<Argument>create(instance -> instance.group(
						Rule.CODEC.listOf().fieldOf("rules").forGetter(Argument::rules),
						Codec.either(Codec.STRING, Codec.STRING.listOf())
								.xmap(either -> either.map(List::of, Function.identity()), Either::right)
								.fieldOf("value").forGetter(Argument::values)
				).apply(instance, Argument::new))
		).xmap(
				either -> either.map(string -> new Argument(List.of(), List.of(string)), Function.identity()),
				Either::right
		);
	}

	public record AssetIndex(String id, String sha1, int size, int totalSize, URI url) {
		public static final Codec<AssetIndex> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("id").forGetter(AssetIndex::id),
				Codec.STRING.fieldOf("sha1").forGetter(AssetIndex::sha1),
				Codec.INT.fieldOf("size").forGetter(AssetIndex::size),
				Codec.INT.fieldOf("totalSize").forGetter(AssetIndex::totalSize),
				MoreCodecs.URI.fieldOf("url").forGetter(AssetIndex::url)
		).apply(instance, AssetIndex::new));

		public FullAssetIndex expand() {
			HttpRequest request = HttpRequest.newBuilder(this.url).GET().build();
			try {
				HttpResponse<String> response = Utils.CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
				JsonElement json = JsonParser.parseString(response.body());
				return Utils.decode(FullAssetIndex.CODEC, json);
			} catch (Exception e) {
				throw new RuntimeException("Error expanding version " + this.id, e);
			}
		}
	}

	public record Download(String sha1, int size, URI url) implements Downloadable {
		public static final Codec<Download> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("sha1").forGetter(Download::sha1),
				Codec.INT.fieldOf("size").forGetter(Download::size),
				MoreCodecs.URI.fieldOf("url").forGetter(Download::url)
		).apply(instance, Download::new));
	}

	public record Downloads(Download client, Optional<Download> clientMappings, Optional<Download> server,
							Optional<Download> serverMappings, Optional<Download> windowsServer) {
		public static final Codec<Downloads> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Download.CODEC.fieldOf("client").forGetter(Downloads::client),
				Download.CODEC.optionalFieldOf("client_mappings").forGetter(Downloads::clientMappings),
				Download.CODEC.optionalFieldOf("server").forGetter(Downloads::server),
				Download.CODEC.optionalFieldOf("server_mappings").forGetter(Downloads::serverMappings),
				Download.CODEC.optionalFieldOf("windows_server").forGetter(Downloads::windowsServer)
		).apply(instance, Downloads::new));
	}

	public record JavaVersion(String component, int majorVersion) {
		public static final Codec<JavaVersion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("component").forGetter(JavaVersion::component),
				Codec.INT.fieldOf("majorVersion").forGetter(JavaVersion::majorVersion)
		).apply(instance, JavaVersion::new));
	}

	public record Artifact(String path, String sha1, int size, URI url) implements Downloadable {
		public static final Codec<Artifact> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("path").forGetter(Artifact::path),
				Codec.STRING.fieldOf("sha1").forGetter(Artifact::sha1),
				Codec.INT.fieldOf("size").forGetter(Artifact::size),
				MoreCodecs.URI.fieldOf("url").forGetter(Artifact::url)
		).apply(instance, Artifact::new));
	}

	public record Classifiers(Map<String, Artifact> map) {
		public static final Codec<Classifiers> CODEC = Codec.unboundedMap(Codec.STRING, Artifact.CODEC).xmap(Classifiers::new, Classifiers::map);
	}

	public record Natives(Optional<String> linux, Optional<String> windows, Optional<String> osx) {
		public static final Codec<Natives> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.optionalFieldOf("linux").forGetter(Natives::linux),
				Codec.STRING.optionalFieldOf("windows").forGetter(Natives::windows),
				Codec.STRING.optionalFieldOf("osx").forGetter(Natives::osx)
		).apply(instance, Natives::new));

		public String choose() {
			// todo: ${arch}
			return (switch (OperatingSystem.CURRENT) {
				case WINDOWS -> this.windows;
				case LINUX -> this.linux;
				case OSX -> this.osx;
			}).orElse(null);
		}
	}

	public record LibraryDownload(Optional<Artifact> artifact, Optional<Classifiers> classifiers) {
		public static final Codec<LibraryDownload> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Artifact.CODEC.optionalFieldOf("artifact").forGetter(LibraryDownload::artifact),
				Classifiers.CODEC.optionalFieldOf("classifiers").forGetter(LibraryDownload::classifiers)
		).apply(instance, LibraryDownload::new));
	}

	public record Library(LibraryDownload download, String name, List<Rule> rules, Optional<Natives> natives) {
		public static final Codec<Library> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				LibraryDownload.CODEC.fieldOf("downloads").forGetter(Library::download),
				Codec.STRING.fieldOf("name").forGetter(Library::name),
				Rule.CODEC.listOf().optionalFieldOf("rules", List.of()).forGetter(Library::rules),
				Natives.CODEC.optionalFieldOf("natives").forGetter(Library::natives)
		).apply(instance, Library::new));
	}

	public record LoggingFile(String id, String sha1, int size, URI url) {
		public static final Codec<LoggingFile> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("id").forGetter(LoggingFile::id),
				Codec.STRING.fieldOf("sha1").forGetter(LoggingFile::sha1),
				Codec.INT.fieldOf("size").forGetter(LoggingFile::size),
				MoreCodecs.URI.fieldOf("url").forGetter(LoggingFile::url)
		).apply(instance, LoggingFile::new));
	}

	public record ClientLogging(String argument, LoggingFile file, String type) {
		public static final Codec<ClientLogging> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("argument").forGetter(ClientLogging::argument),
				LoggingFile.CODEC.fieldOf("file").forGetter(ClientLogging::file),
				Codec.STRING.fieldOf("type").forGetter(ClientLogging::type)
		).apply(instance, ClientLogging::new));
	}

	public record Logging(ClientLogging clientLogging) {
		public static final Codec<Logging> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				ClientLogging.CODEC.fieldOf("client").forGetter(Logging::clientLogging)
		).apply(instance, Logging::new));
	}
}
