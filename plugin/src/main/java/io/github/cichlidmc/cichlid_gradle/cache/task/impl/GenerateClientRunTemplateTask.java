package io.github.cichlidmc.cichlid_gradle.cache.task.impl;

import io.github.cichlidmc.cichlid_gradle.cache.storage.RunTemplateStorage;
import io.github.cichlidmc.cichlid_gradle.cache.task.CacheTask;
import io.github.cichlidmc.cichlid_gradle.cache.task.TaskContext;
import io.github.cichlidmc.cichlid_gradle.run.ClientRunTemplateGenerator;
import io.github.cichlidmc.cichlid_gradle.run.RunTemplate;
import io.github.cichlidmc.pistonmetaparser.FullVersion;

import java.io.IOException;

public class GenerateClientRunTemplateTask extends CacheTask {
	private final RunTemplateStorage storage;
	private final FullVersion version;

	protected GenerateClientRunTemplateTask(TaskContext context, RunTemplateStorage storage, FullVersion version) {
		super("ClientRunTemplate", "Generate the client run config template", context);
		this.storage = storage;
		this.version = version;
	}

	@Override
	protected void doRun() throws IOException {
		RunTemplate template = ClientRunTemplateGenerator.generate(this.version);
		this.storage.writeRun(this.version.id, "client", template);
	}
}
