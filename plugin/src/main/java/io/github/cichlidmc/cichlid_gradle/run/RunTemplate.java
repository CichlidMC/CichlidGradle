package io.github.cichlidmc.cichlid_gradle.run;

import io.github.cichlidmc.tinyjson.value.JsonValue;
import io.github.cichlidmc.tinyjson.value.composite.JsonArray;
import io.github.cichlidmc.tinyjson.value.composite.JsonObject;
import io.github.cichlidmc.tinyjson.value.primitive.JsonString;

import java.util.List;

public record RunTemplate(String mainClass, List<String> programArgs, List<String> jvmArgs) {
	public JsonObject encode() {
		JsonObject json = new JsonObject();
		json.put("main_class", this.mainClass);
		json.put("program_args", JsonArray.of(this.programArgs.stream().map(JsonString::new).toArray(JsonValue[]::new)));
		json.put("jvm_args", JsonArray.of(this.jvmArgs.stream().map(JsonString::new).toArray(JsonValue[]::new)));
		return json;
	}

	public static RunTemplate parse(JsonValue value) {
		JsonObject json = value.asObject();

		String mainClass = json.get("main_class").asString().value();
		List<String> programArgs = json.get("program_args").asArray().stream()
				.map(arg -> arg.asString().value())
				.toList();
		List<String> jvmArgs = json.get("jvm_args").asArray().stream()
				.map(arg -> arg.asString().value())
				.toList();

		return new RunTemplate(mainClass, programArgs, jvmArgs);
	}
}
