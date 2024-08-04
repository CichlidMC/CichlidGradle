package io.github.cichlidmc.cichlid_gradle.extension.minecraft;

import org.gradle.api.provider.Property;

public interface MinecraftExtension {
	Property<String> getVersion();
	Property<String> getDistribution();
}
