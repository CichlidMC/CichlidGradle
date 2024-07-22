package io.github.cichlidmc.cichlid_gradle.pistonmeta.test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.internal.LinkedTreeMap;

public class AccessTrackingJsonObject extends JsonElement {
	private final LinkedTreeMap<String, JsonElement> members = new LinkedTreeMap<>(false);
	private final Set<String> accessed = new HashSet<>();

	@SuppressWarnings("deprecation") // superclass constructor
	public AccessTrackingJsonObject() {
	}

	@SuppressWarnings("deprecation") // superclass constructor
	public AccessTrackingJsonObject(JsonObject json) {
		this.members.putAll(json.asMap());
	}

	@Override
	public AccessTrackingJsonObject deepCopy() {
		throw new UnsupportedOperationException();
	}

	public void add(String property, JsonElement value) {
		members.put(property, value == null ? JsonNull.INSTANCE : value);
	}

	public Set<Map.Entry<String, JsonElement>> entrySet() {
		return members.entrySet();
	}

	public JsonElement get(String memberName) {
		this.accessed.add(memberName);
		return members.get(memberName);
	}

	public Set<String> collectUnused() {
		Set<String> set = new HashSet<>();
		this.collectUnused(set);
		return set;
	}

	private void collectUnused(Set<String> unused) {
		this.members.forEach((key, element) -> {
			if (element instanceof AccessTrackingJsonObject tracking)
				tracking.collectUnused(unused);

			if (!this.accessed.contains(key))
				unused.add(key);
		});
	}
}
