package io.github.cichlidmc.cichlid_gradle.extension.dep;

import io.github.cichlidmc.cichlid_gradle.util.Distribution;

// TODO: add a way to specify the natives to use. Requires changes to PistonMetaParser.
public interface MinecraftSpec {
	MinecraftSpec distribution(Distribution dist);

	MinecraftSpec version(String version);

	// shortcuts

	MinecraftSpec client();
	MinecraftSpec server();
	MinecraftSpec merged();
	MinecraftSpec bundler();
}
