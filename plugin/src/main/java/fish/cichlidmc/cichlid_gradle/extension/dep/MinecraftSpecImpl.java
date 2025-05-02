package fish.cichlidmc.cichlid_gradle.extension.dep;

import fish.cichlidmc.cichlid_gradle.util.Distribution;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.dsl.DependencyFactory;

public final class MinecraftSpecImpl implements MinecraftSpec {
	private Distribution dist;
	private String version;

	@Override
	public MinecraftSpec distribution(Distribution dist) {
		this.dist = dist;
		return this;
	}

	@Override
	public MinecraftSpec version(String version) {
		this.version = version;
		return this;
	}

	public ExternalModuleDependency createDependencyOrThrow(DependencyFactory factory) {
		if (this.dist == null) {
			throw new InvalidUserDataException("Minecraft distribution is not specified.");
		} else if (this.version == null) {
			throw new InvalidUserDataException("Minecraft version is not specified.");
		}

		return factory.create("net.minecraft", "minecraft-" + this.dist.name, this.version);
	}

	@Override
	public MinecraftSpec client() {
		this.distribution(Distribution.CLIENT);
		return this;
	}

	@Override
	public MinecraftSpec server() {
		this.distribution(Distribution.SERVER);
		return this;
	}

	@Override
	public MinecraftSpec merged() {
		this.distribution(Distribution.MERGED);
		return this;
	}

	@Override
	public MinecraftSpec bundler() {
		this.distribution(Distribution.BUNDLER);
		return this;
	}
}
