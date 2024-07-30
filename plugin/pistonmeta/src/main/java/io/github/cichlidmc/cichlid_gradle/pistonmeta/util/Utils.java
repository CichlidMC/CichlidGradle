package io.github.cichlidmc.cichlid_gradle.pistonmeta.util;

import java.net.http.HttpClient;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.test.AccessTrackingJsonObject;
import io.github.cichlidmc.cichlid_gradle.pistonmeta.test.AccessTrackingJsonOps;

public class Utils {
	public static final boolean VALIDATE_DECODE = true;
	public static final HttpClient CLIENT = HttpClient.newBuilder().build();
	public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	public static <T> T decode(Codec<T> codec, JsonElement json) {
		JsonOps ops = JsonOps.INSTANCE;
		if (VALIDATE_DECODE && json instanceof JsonObject object) {
			json = new AccessTrackingJsonObject(object);
			ops = AccessTrackingJsonOps.INSTANCE;
		}

		T result = codec.decode(ops, json).getOrThrow().getFirst();

		if (json instanceof AccessTrackingJsonObject tracked) {
			Set<String> unused = tracked.collectUnused();
			if (!unused.isEmpty()) {
				throw new IllegalStateException("Unparsed fields: " + unused);
			}
		}

		return result;
	}
}
