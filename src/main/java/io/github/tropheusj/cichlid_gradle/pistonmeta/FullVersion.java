package io.github.tropheusj.cichlid_gradle.pistonmeta;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tropheusj.cichlid_gradle.util.UriCodec;

public record FullVersion(
		Arguments args, AssetIndex assetIndex, String assets, int complianceLevel,
		List<Download> downloads, String id, JavaVersion javaVersion, List<Library> libraries, Logging logging,
		String mainClass, int minLauncherVersion, String releaseTime, String time, VersionType type) {
	public static final Codec<FullVersion> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			VersionType.CODEC.fieldOf("type").forGetter(FullVersion::type)
	).apply(instance, FullVersion::new));

	public record Features(Map<String, Boolean> features) {
		public static final Codec<Features> CODEC = Codec.unboundedMap(Codec.STRING, Codec.BOOL).xmap(Features::new, Features::features);
	}

	public record Os(String name) {
		public static final Codec<Os> CODEC = Codec.STRING.xmap(Os::new, Os::name);
	}

	public record Rule(String action, Optional<Features> features, Optional<Os> os) {
		public static final Codec<Rule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("action").forGetter(Rule::action),
				Features.CODEC.optionalFieldOf("features").forGetter(Rule::features),
				Os.CODEC.optionalFieldOf("os").forGetter(Rule::os)
		).apply(instance, Rule::new));
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

	public record Download(String sha1, int size, URI url) {
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
				Download.CODEC.fieldOf("server_mappings").forGetter(Downloads::serverMappings),
		).apply(instance, Downloads::new));
	}

	public record JavaVersion() {

	}

	public record Library() {
	}

	public record Logging() {
	}
}
