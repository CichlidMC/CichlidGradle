package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.storage.AssetStorage;
import fish.cichlidmc.cichlid_gradle.cache.storage.LockableStorage;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;
import fish.cichlidmc.cichlid_gradle.util.hash.Encoding;
import fish.cichlidmc.cichlid_gradle.util.hash.HashAlgorithm;
import fish.cichlidmc.cichlid_gradle.util.io.Download;
import fish.cichlidmc.cichlid_gradle.util.io.DownloadBatch;
import fish.cichlidmc.cichlid_gradle.util.io.FileUtils;
import fish.cichlidmc.pistonmetaparser.version.assets.Asset;
import fish.cichlidmc.pistonmetaparser.version.assets.AssetIndex;
import fish.cichlidmc.pistonmetaparser.version.assets.FullAssetIndex;
import fish.cichlidmc.tinyjson.TinyJson;
import fish.cichlidmc.tinyjson.value.JsonValue;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class AssetsTask extends CacheTask {
	private static final Logger logger = Logging.getLogger(AssetsTask.class);

	private LockableStorage.Lock lock;

	public AssetsTask(CacheTaskEnvironment env) {
		super("Download asset index " + env.version.assets, env);
	}

	@Override
	public String run() throws IOException {
		AssetStorage assets = this.env.cache.assets;
		this.lock = assets.lockLoudly();
		// see if it's done now
		AssetIndex index = this.env.version.assetIndex;
		if (assets.isComplete(index)) {
			return "Asset index " + index.id + " is now cached.";
		}

		Path indexFile = assets.index(index);
		new Download(index, indexFile).run();
		// read downloaded file to avoid downloading again with expand()
		JsonValue indexJson = TinyJson.parse(indexFile);
		FullAssetIndex fullIndex = FullAssetIndex.parse(indexJson);

		DownloadBatch.Builder builder = new DownloadBatch.Builder();
		for (Asset asset : fullIndex.objects.values()) {
			Path dest = assets.object(asset);
			if (this.shouldDownload(asset, dest)) {
				builder.download(asset, dest);
			}
		}

		logger.quiet("Downloading {} asset object(s)...", builder.size());

		builder.build().execute();

		if (fullIndex.isVirtual()) {
			logger.quiet("Extracting virtual assets...");
			this.extractVirtualAssets(index, fullIndex);
		}

		assets.markComplete(index);
		return "Index contained " + fullIndex.objects.size() + " object(s), " + builder.size() + " of which were missing.";
	}

	@Override
	protected void cleanup() throws IOException {
		// unlock here in case run throws
		if (this.lock != null) {
			this.lock.close();
		}
	}

	private void extractVirtualAssets(AssetIndex index, FullAssetIndex fullIndex) throws IOException {
		Path dir = this.env.cache.assets.extractLocation(index);
		for (Map.Entry<String, Asset> entry : fullIndex.objects.entrySet()) {
			String path = entry.getKey();
			Asset asset = entry.getValue();
			Path dest = dir.resolve(path);
			Path src = this.env.cache.assets.object(asset);
			FileUtils.copy(src, dest);
		}
	}

	private boolean shouldDownload(Asset asset, Path dest) throws IOException {
		// there can be duplicates under different names, and many indices reference the same objects
		if (!Files.exists(dest))
			return true;

		long existingSize = Files.size(dest);
		if (asset.size != existingSize) {
			logger.warn("Found an existing asset that has changed size: {}. Bad download? Overwriting.", asset.path);
			return true;
		}

		String existingHash = Encoding.HEX.encode(HashAlgorithm.SHA1.hash(dest));
		if (!asset.hash.equals(existingHash)) {
			logger.warn("Found an existing asset that has changed hashes: {}. Bad download? Overwriting.", asset.path);
			return true;
		}

		return false;
	}
}
