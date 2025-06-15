package fish.cichlidmc.cichlid_gradle.cache.task.impl;

import fish.cichlidmc.cichlid_gradle.cache.storage.RunTemplateStorage;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import fish.cichlidmc.cichlid_gradle.cache.task.CacheTaskEnvironment;
import fish.cichlidmc.cichlid_gradle.run.ClientRunTemplateGenerator;
import fish.cichlidmc.cichlid_gradle.run.RunTemplate;
import fish.cichlidmc.pistonmetaparser.FullVersion;

import java.io.IOException;

// public class GenerateClientRunTemplateTask extends CacheTask {
// 	private final RunTemplateStorage storage;
// 	private final FullVersion version;
//
// 	protected GenerateClientRunTemplateTask(CacheTaskEnvironment env) {
// 		super("ClientRunTemplate", env);
// 		this.storage = storage;
// 		this.version = version;
// 	}
//
// 	@Override
// 	protected void doRun() throws IOException {
// 		RunTemplate template = ClientRunTemplateGenerator.generate(this.version);
// 		this.storage.writeRun(this.version.id, "client", template);
// 	}
// }
