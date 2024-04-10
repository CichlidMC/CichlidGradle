package io.github.tropheusj.cichlid_gradle.pistonmeta;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public enum VersionType {
	SNAPSHOT, RELEASE;

	public final String name = this.name().toLowerCase(Locale.ROOT);

	public static final Codec<VersionType> CODEC = Codec.STRING.comapFlatMap(VersionType::of, VersionType::getSerializedName);

	public String getSerializedName() {
		return name;
	}

	public static DataResult<VersionType> of(String value) {
		VersionType type = switch (value) {
			case "snapshot" -> SNAPSHOT;
			case "release" -> RELEASE;
			default -> null;
		};
		return type != null ? DataResult.success(type) : DataResult.error(() -> "Invalid type: " + value);
	}
}
