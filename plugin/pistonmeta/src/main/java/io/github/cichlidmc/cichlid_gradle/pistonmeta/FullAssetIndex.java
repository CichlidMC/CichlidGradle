package io.github.cichlidmc.cichlid_gradle.pistonmeta;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.util.Downloadable;

// note: 'virtual' is only specified in the 'legacy' index, and set to true.
// likewise, 'map_to_resources' is only specified in 'pre-1.6', also true.
// 'virtual' requires that the assets be extracted from the index. The use of 'map_to_resources' is unknown.
public record FullAssetIndex(Optional<Boolean> virtual, Optional<Boolean> mapToResources, Map<String, Asset> objects) {
	public static final Codec<FullAssetIndex> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.BOOL.optionalFieldOf("virtual").forGetter(FullAssetIndex::virtual),
			Codec.BOOL.optionalFieldOf("map_to_resources").forGetter(FullAssetIndex::mapToResources),
			Codec.unboundedMap(Codec.STRING, Asset.CODEC).fieldOf("objects").forGetter(FullAssetIndex::objects)
	).apply(instance, FullAssetIndex::new));

	public record Asset(String sha1, int size) implements Downloadable {
		public static final Codec<Asset> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				Codec.STRING.fieldOf("hash").forGetter(Asset::sha1),
				Codec.INT.fieldOf("size").forGetter(Asset::size)
		).apply(instance, Asset::new));

		public static final String URL_ROOT = "https://resources.download.minecraft.net/";

		public String path() {
			String hashStart = this.sha1.substring(0, 2);
			return hashStart + '/' + this.sha1;
		}

		@Override
		public URI url() {
			return URI.create(URL_ROOT + this.path());
		}
	}
}
