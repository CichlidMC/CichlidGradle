package io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.tropheusj.cichlid_gradle.util.UriCodec;

public record VersionManifest(LatestVersions latest, List<Version> versions) {
	public static final URI URL = URI.create("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json");

	private static final HttpClient client = HttpClient.newBuilder().build();
	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public static final Codec<VersionManifest> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			LatestVersions.CODEC.fieldOf("latest").forGetter(VersionManifest::latest),
			Version.CODEC.listOf().fieldOf("versions").forGetter(VersionManifest::versions)
	).apply(instance, VersionManifest::new));

	public Map<String, Version> mapVersions() {
		Map<String, Version> map = new HashMap<>();
		this.versions.forEach(version -> map.put(version.id(), version));
		return map;
	}

	public void save(Path path) {
		JsonElement json = CODEC.encodeStart(JsonOps.INSTANCE, this).getOrThrow();
        try {
            Files.writeString(path, gson.toJson(json));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

	public static VersionManifest fetch() {
		HttpRequest request = HttpRequest.newBuilder(URL).GET().build();
		try {
			HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
			JsonElement json = JsonParser.parseString(response.body());
			return CODEC.decode(JsonOps.INSTANCE, json).getOrThrow().getFirst();
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	public static VersionManifest ofFile(Path path) {
		try (BufferedReader reader = Files.newBufferedReader(path)) {
			JsonElement json = JsonParser.parseReader(reader);
			return CODEC.decode(JsonOps.INSTANCE, json).getOrThrow().getFirst();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public record LatestVersions(String release, String snapshot) {
		public static final Codec<LatestVersions> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("release").forGetter(LatestVersions::release),
				Codec.STRING.fieldOf("snapshot").forGetter(LatestVersions::snapshot)
		).apply(instance, LatestVersions::new));
	}

	public record Version(String id, VersionType type, URI url, String time, String releaseTime, String sha1, int complianceLevel) {
		public static final Codec<Version> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("id").forGetter(Version::id),
				VersionType.CODEC.fieldOf("type").forGetter(Version::type),
				UriCodec.INSTANCE.fieldOf("url").forGetter(Version::url),
				Codec.STRING.fieldOf("time").forGetter(Version::time),
				Codec.STRING.fieldOf("releaseTime").forGetter(Version::releaseTime),
				Codec.STRING.fieldOf("sha1").forGetter(Version::sha1),
				Codec.INT.fieldOf("complianceLevel").forGetter(Version::complianceLevel)
		).apply(instance, Version::new));

		public FullVersion expand() {
			HttpRequest request = HttpRequest.newBuilder(this.url).GET().build();
			try {
				HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
				JsonElement json = JsonParser.parseString(response.body());
				return FullVersion.CODEC.decode(JsonOps.INSTANCE, json).getOrThrow().getFirst();
			} catch (IOException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
