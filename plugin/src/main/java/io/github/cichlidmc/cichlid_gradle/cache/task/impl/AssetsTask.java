package io.github.cichlidmc.cichlid_gradle.cache.task.impl;

import io.github.cichlidmc.cichlid_gradle.cache.storage.AssetStorage;
import io.github.cichlidmc.cichlid_gradle.cache.storage.LockableStorage;
import io.github.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import io.github.cichlidmc.cichlid_gradle.cache.task.TaskContext;
import io.github.cichlidmc.cichlid_gradle.util.FileUtils;
import io.github.cichlidmc.pistonmetaparser.version.assets.Asset;
import io.github.cichlidmc.pistonmetaparser.version.assets.AssetIndex;
import io.github.cichlidmc.pistonmetaparser.version.assets.FullAssetIndex;
import io.github.cichlidmc.tinyjson.TinyJson;
import io.github.cichlidmc.tinyjson.value.JsonValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class AssetsTask extends CacheTask {
	private final AssetStorage storage;
	private final AssetIndex index;

	private LockableStorage.Lock lock;

	public AssetsTask(TaskContext context, AssetStorage storage, AssetIndex index) {
		super("Assets", "Downloading asset index " + index.id, context);
		this.storage = storage;
		this.index = index;
	}

	@Override
	public void doRun() throws IOException {
		this.lock = this.storage.lock();
		if (this.storage.isComplete(this.index))
			return;

		Path indexFile = this.storage.index(this.index);
		FileUtils.downloadSilently(this.index, indexFile);
		// read downloaded file to avoid downloading again with expand()
		JsonValue indexJson = TinyJson.parse(indexFile);
		FullAssetIndex fullIndex = FullAssetIndex.parse(indexJson);
		long startTime = System.currentTimeMillis();

		for (Asset asset : fullIndex.objects.values()) {
			Path dest = this.storage.object(asset);
			if (this.shouldDownload(asset, dest)) {
				FileUtils.download(asset, dest);
			}
		}

		if (fullIndex.isVirtual()) {
			this.logger.quiet("Extracting virtual assets");
			this.extractVirtualAssets(this.index, fullIndex);
		}

		long endTime = System.currentTimeMillis();
		long seconds = (endTime - startTime) / 1000;
		logger.quiet("Finished downloading assets in {} seconds.", seconds);

		this.storage.markComplete(this.index);
	}

	@Override
	protected void cleanup() throws IOException {
		if (this.lock != null) {
			this.lock.close();
		}
	}

	private void extractVirtualAssets(AssetIndex index, FullAssetIndex fullIndex) throws IOException {
		Path dir = this.storage.extractLocation(index);
		for (Map.Entry<String, Asset> entry : fullIndex.objects.entrySet()) {
			String path = entry.getKey();
			Asset asset = entry.getValue();
			Path dest = dir.resolve(path);
			Path src = this.storage.object(asset);
			Files.createDirectories(dest.getParent());
			Files.copy(src, dest);
		}
	}

	private boolean shouldDownload(Asset asset, Path dest) throws IOException {
		// there can be duplicates under different names, and many indices reference the same objects
		if (!Files.exists(dest))
			return true;

		long existingSize = Files.size(dest);
		if (asset.size != existingSize) {
			this.logger.warn("Found an existing asset that has changed size: {}. Bad download? Overwriting.", asset.path);
			return true;
		}

		String existingHash = FileUtils.sha1(dest);
		if (!asset.hash.endsWith(existingHash)) {
			this.logger.warn("Found an existing asset that has changed hashes: {}. Bad download? Overwriting.", asset.path);
			return true;
		}

		return false;
	}
}
