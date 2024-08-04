package io.github.cichlidmc.cichlid_gradle.extension;

import java.util.Objects;

import javax.inject.Inject;

import io.github.cichlidmc.cichlid_gradle.cache.CichlidCache;
import io.github.cichlidmc.cichlid_gradle.extension.minecraft.MinecraftExtension;
import io.github.cichlidmc.cichlid_gradle.run.RunConfiguration;
import io.github.cichlidmc.cichlid_gradle.run.RunConfigurationImpl;
import org.gradle.api.NamedDomainObjectContainer;
import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.jetbrains.annotations.NotNull;

public abstract class CichlidExtensionImpl implements CichlidExtension {
	private final NamedDomainObjectContainer<RunConfiguration> runs;
	private final MinecraftExtension mc;

	@Inject
	public CichlidExtensionImpl(Project project) {
		RunConfigFactory factory = new RunConfigFactory(project.getObjects());
		this.runs = project.getObjects().domainObjectContainer(RunConfiguration.class, factory);
		factory.container = this.runs;
		this.addDefaultRuns();

		this.mc = this.getExtensions().create("minecraft", MinecraftExtension.class);

		project.afterEvaluate(this::apply);
	}

	@Override
	public NamedDomainObjectContainer<RunConfiguration> getRuns() {
		return this.runs;
	}

	private void addDefaultRuns() {
		this.runs.register("client", config -> {
			config.getJvmArgs().add("-Xmx2G");
		});
		this.runs.register("server", config -> {
			config.getJvmArgs().add("-Xmx1G");
			config.getProgramArgs().add("nogui");
		});
	}

	private void apply(Project project) {
		String mcVer = this.mc.getVersion().get();
		CichlidCache.get(project).maven.ensureVersionDownloaded(mcVer);
		String mcDist = this.mc.getDistribution().get();
		project.getDependencies().add("implementation", "net.minecraft:minecraft-" + mcDist + ':' + mcVer);
	}

	private static class RunConfigFactory implements NamedDomainObjectFactory<RunConfiguration> {
		private final ObjectFactory objects;
		private NamedDomainObjectContainer<RunConfiguration> container;

		private RunConfigFactory(ObjectFactory objects) {
			this.objects = objects;
		}

		@Override
		@NotNull
		public RunConfiguration create(@NotNull String name) {
			Objects.requireNonNull(this.container, "container");
			RunConfigurationImpl config = this.objects.newInstance(RunConfigurationImpl.class, name);
			config.setContainer(this.container);
			return config;
		}
	}
}
