package io.github.cichlidmc.cichlid_gradle.cache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import io.github.cichlidmc.cichlid_gradle.util.FileUtils;
import io.github.cichlidmc.pistonmetaparser.FullVersion;
import io.github.cichlidmc.pistonmetaparser.version.assets.Asset;
import io.github.cichlidmc.pistonmetaparser.version.assets.FullAssetIndex;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

/**
 * Global cache of Minecraft assets and indices.
 */
public class AssetStorage {
	private static final Logger logger = Logging.getLogger(AssetStorage.class);

	private final Path root;

	AssetStorage(Path root) {
		this.root = root;
	}

	void downloadAssets(FullVersion version) throws IOException {
		String indexId = version.assetIndex.id;
		Path indexFile = this.index(indexId);
		// many versions share the same indices, check if it's been downloaded
		if (Files.exists(indexFile))
			return;

		logger.quiet("Asset index '{}' not cached, downloading for the first time. This could take a while.", indexId);
		long startTime = System.currentTimeMillis();

		FileUtils.download(version.assetIndex, indexFile);
		FullAssetIndex index = version.assetIndex.expand();

		index.objects.values().forEach(asset -> {
			Path dest = this.object(asset);
			// there can be duplicates under different names
			// not parallel because it can cause a race condition here
			if (!Files.exists(dest)) {
				FileUtils.downloadSilently(asset, dest);
			}
		});

		if (index.isVirtual()) {
			logger.quiet("Extracting virtual assets");
			this.extractVirtualAssets(indexId, index);
		}

		long endTime = System.currentTimeMillis();
		long seconds = (endTime - startTime) / 1000;
		logger.quiet("Finished downloading assets in {} seconds.", seconds);
	}

	private void extractVirtualAssets(String id, FullAssetIndex index) throws IOException {
		Path dir = this.root.resolve("virtual").resolve(id);
		for (Map.Entry<String, Asset> entry : index.objects.entrySet()) {
			String path = entry.getKey();
			Asset asset = entry.getValue();
			Path dest = dir.resolve(path);
			Path src = this.object(asset);
			Files.createDirectories(dest.getParent());
			Files.copy(src, dest);
		}
	}

	private Path index(String id) {
		return this.root.resolve("indexes").resolve(id + ".json");
	}

	private Path object(Asset asset) {
		return this.root.resolve("objects").resolve(asset.path);
	}
}
