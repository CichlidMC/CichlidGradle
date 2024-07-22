package io.github.cichlidmc.cichlid_gradle.pistonmeta.test;

import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapLike;

public class AccessTrackingJsonOps extends JsonOps {
	public static final AccessTrackingJsonOps INSTANCE = new AccessTrackingJsonOps(false);

	protected AccessTrackingJsonOps(boolean compressed) {
		super(compressed);
	}

	@Override
	public JsonElement createMap(Stream<Pair<JsonElement, JsonElement>> map) {
		AccessTrackingJsonObject result = new AccessTrackingJsonObject();
		map.forEach(p -> result.add(p.getFirst().getAsString(), p.getSecond()));
		return result;
	}

	@Override
	public DataResult<MapLike<JsonElement>> getMap(final JsonElement input) {
		if (!(input instanceof AccessTrackingJsonObject object)) {
			return super.getMap(input);
		}
		return DataResult.success(new MapLike<>() {
			@Nullable
			@Override
			public JsonElement get(final JsonElement key) {
				final JsonElement element = object.get(key.getAsString());
				if (element instanceof JsonNull) {
					return null;
				}
				return element;
			}

			@Nullable
			@Override
			public JsonElement get(final String key) {
				final JsonElement element = object.get(key);
				if (element instanceof JsonNull) {
					return null;
				}
				return element;
			}

			@Override
			public Stream<Pair<JsonElement, JsonElement>> entries() {
				return object.entrySet().stream().map(e -> Pair.of(new JsonPrimitive(e.getKey()), e.getValue()));
			}

			@Override
			public String toString() {
				return "MapLike[" + object + "]";
			}
		});
	}
}
