package fish.cichlidmc.cichlid_gradle.cache.storage;

import fish.cichlidmc.cichlid_gradle.cache.task.TaskContext;
import fish.cichlidmc.cichlid_gradle.cache.task.impl.AssetsTask;
import fish.cichlidmc.cichlid_gradle.util.FileUtils;
import fish.cichlidmc.pistonmetaparser.version.assets.Asset;
import fish.cichlidmc.pistonmetaparser.version.assets.AssetIndex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Global cache of Minecraft assets and indices.
 */
public class AssetStorage extends LockableStorage {
	public AssetStorage(Path root) {
		super(root);
	}

	public void submitInitialTasks(AssetIndex index, TaskContext context) {
		AssetsTask task = new AssetsTask(context, this, index);
		context.submitSilently(task);
	}

	public Path index(AssetIndex index) {
		return this.root.resolve("indexes").resolve(index.id + ".json");
	}

	public Path object(Asset asset) {
		return this.root.resolve("objects").resolve(asset.path);
	}

	public Path extractLocation(AssetIndex index) {
		return this.root.resolve("virtual").resolve(index.id);
	}

	public boolean isComplete(AssetIndex index) {
		return Files.exists(this.completionMarker(index));
	}

	public void markComplete(AssetIndex index) throws IOException {
		FileUtils.ensureCreated(this.completionMarker(index));
	}

	private Path completionMarker(AssetIndex index) {
		return this.root.resolve("indexes").resolve(index.id + ".complete");
	}
}
