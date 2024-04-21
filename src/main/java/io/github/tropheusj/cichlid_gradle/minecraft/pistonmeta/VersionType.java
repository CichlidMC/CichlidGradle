package io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public enum VersionType {
	SNAPSHOT, RELEASE, OLD_ALPHA, OLD_BETA;

	public final String name = this.name().toLowerCase(Locale.ROOT);

	public static final Codec<VersionType> CODEC = Codec.STRING.comapFlatMap(VersionType::of, VersionType::getSerializedName);

	public String getSerializedName() {
		return name;
	}

	public static DataResult<VersionType> of(String value) {
		VersionType type = switch (value) {
			case "snapshot" -> SNAPSHOT;
			case "release" -> RELEASE;
			case "old_alpha" -> OLD_ALPHA;
			case "old_beta" -> OLD_BETA;
			default -> null;
		};
		return type != null ? DataResult.success(type) : DataResult.error(() -> "Invalid type: " + value);
	}
}
