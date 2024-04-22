package io.github.tropheusj.cichlid_gradle.extension;

import io.github.tropheusj.cichlid_gradle.minecraft.GlobalMinecraftCache;
import io.github.tropheusj.cichlid_gradle.minecraft.RuleContext;
import io.github.tropheusj.cichlid_gradle.minecraft.Side;
import io.github.tropheusj.cichlid_gradle.minecraft.pistonmeta.FullVersion.Os;
import org.gradle.api.Project;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.file.ConfigurableFileCollection;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class CichlidExtensionImpl implements CichlidExtension {
    @Inject
    protected abstract Project getProject();

    private String minecraftVersion;

    @Override
    public void setMinecraft(String version) {
        if (this.minecraftVersion != null) {
            throw new IllegalStateException(
                    "Cannot set Minecraft version more than once; tried to override %s with %s"
                            .formatted(this.minecraftVersion, version)
            );
        }
        this.minecraftVersion = version;
        Project project = this.getProject();
        Path home = project.getGradle().getGradleUserHomeDir().toPath();
        GlobalMinecraftCache cache = GlobalMinecraftCache.get(home);
        Path jar = cache.getJar(version, Side.CLIENT);
        Set<Path> libs = cache.getLibraries(version, new RuleContext(Map.of(), Os.CURRENT));

        ConfigurableFileCollection fileCollection = project.files(jar, libs);
        FileCollectionDependency dependency = project.getDependencyFactory().create(fileCollection);
        project.getDependencies().add("implementation", dependency);
    }

    @Override
    public String getMinecraft() {
        return this.minecraftVersion;
    }
}
