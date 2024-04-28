package io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tropheusj.cichlid_gradle.minecraft.Side;
import io.github.tropheusj.cichlid_gradle.util.Downloadable;
import io.github.tropheusj.cichlid_gradle.util.UriCodec;

public record FullVersion(
		Arguments args, AssetIndex assetIndex, String assets, int complianceLevel,
		Downloads downloads, String id, JavaVersion javaVersion, List<Library> libraries, Logging logging,
		String mainClass, Optional<Integer> minLauncherVersion, String releaseTime, String time, VersionType type) {
	public static final Codec<FullVersion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Arguments.CODEC.fieldOf("arguments").forGetter(FullVersion::args),
			AssetIndex.CODEC.fieldOf("assetIndex").forGetter(FullVersion::assetIndex),
			Codec.STRING.fieldOf("assets").forGetter(FullVersion::assets),
			Codec.INT.fieldOf("complianceLevel").forGetter(FullVersion::complianceLevel),
			Downloads.CODEC.fieldOf("downloads").forGetter(FullVersion::downloads),
			Codec.STRING.fieldOf("id").forGetter(FullVersion::id),
			JavaVersion.CODEC.fieldOf("javaVersion").forGetter(FullVersion::javaVersion),
			Library.CODEC.listOf().fieldOf("libraries").forGetter(FullVersion::libraries),
			Logging.CODEC.fieldOf("logging").forGetter(FullVersion::logging),
			Codec.STRING.fieldOf("mainClass").forGetter(FullVersion::mainClass),
			Codec.INT.optionalFieldOf("minLauncherVersion").forGetter(FullVersion::minLauncherVersion),
			Codec.STRING.fieldOf("releaseTime").forGetter(FullVersion::releaseTime),
			Codec.STRING.fieldOf("time").forGetter(FullVersion::time),
			VersionType.CODEC.fieldOf("type").forGetter(FullVersion::type)
	).apply(instance, FullVersion::new));

	public record Features(Map<String, Boolean> features) {
		public static final Codec<Features> CODEC = Codec.unboundedMap(Codec.STRING, Codec.BOOL).xmap(Features::new, Features::features);
	}

	public record Os(Optional<String> name, Optional<String> arch) {
		public static final Codec<Os> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.optionalFieldOf("name").forGetter(Os::name),
				Codec.STRING.optionalFieldOf("arch").forGetter(Os::arch)
		).apply(instance, Os::new));
	}

	public record Rule(Action action, Optional<Features> features, Optional<Os> os) {
		public static final Codec<Rule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Action.CODEC.fieldOf("action").forGetter(Rule::action),
				Features.CODEC.optionalFieldOf("features").forGetter(Rule::features),
				Os.CODEC.optionalFieldOf("os").forGetter(Rule::os)
		).apply(instance, Rule::new));

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

	public record Arguments(List<Argument> game, List<Argument> jvm) {
		public static final Codec<Arguments> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Argument.CODEC.listOf().fieldOf("game").forGetter(Arguments::game),
				Argument.CODEC.listOf().fieldOf("jvm").forGetter(Arguments::jvm)
		).apply(instance, Arguments::new));
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
				UriCodec.INSTANCE.fieldOf("url").forGetter(AssetIndex::url)
		).apply(instance, AssetIndex::new));
	}

	public record Download(String sha1, int size, URI url) implements Downloadable {
		public static final Codec<Download> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("sha1").forGetter(Download::sha1),
				Codec.INT.fieldOf("size").forGetter(Download::size),
				UriCodec.INSTANCE.fieldOf("url").forGetter(Download::url)
		).apply(instance, Download::new));
	}

	public record Downloads(Download client, Download clientMappings, Download server, Download serverMappings) {
		public static final Codec<Downloads> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Download.CODEC.fieldOf("client").forGetter(Downloads::client),
				Download.CODEC.fieldOf("client_mappings").forGetter(Downloads::clientMappings),
				Download.CODEC.fieldOf("server").forGetter(Downloads::server),
				Download.CODEC.fieldOf("server_mappings").forGetter(Downloads::serverMappings)
		).apply(instance, Downloads::new));

		public Download jar(Side side) {
			return switch (side) {
				case CLIENT -> this.client;
				case SERVER -> this.server;
				case MERGED -> throw new IllegalArgumentException();
			};
		}
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
				UriCodec.INSTANCE.fieldOf("url").forGetter(Artifact::url)
		).apply(instance, Artifact::new));
	}

	public record Classifiers(Map<String, Artifact> map) {
		public static final Codec<Classifiers> CODEC = Codec.unboundedMap(Codec.STRING, Artifact.CODEC).xmap(Classifiers::new, Classifiers::map);
	}

	public record Natives(String linux, String windows, String macos) {
		public static final Codec<Natives> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("linux").forGetter(Natives::linux),
				Codec.STRING.fieldOf("windows").forGetter(Natives::windows),
				Codec.STRING.fieldOf("macos").forGetter(Natives::macos)
		).apply(instance, Natives::new));
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
				UriCodec.INSTANCE.fieldOf("url").forGetter(LoggingFile::url)
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
