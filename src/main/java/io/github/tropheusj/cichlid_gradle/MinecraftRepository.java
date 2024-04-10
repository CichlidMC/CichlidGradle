package io.github.tropheusj.cichlid_gradle;

import org.gradle.api.Action;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.RepositoryContentDescriptor;

public class MinecraftRepository implements ArtifactRepository {
	@Override
	public String getName() {
		return null;
	}

	@Override
	public void setName(String name) {

	}

	@Override
	public void content(Action<? super RepositoryContentDescriptor> configureAction) {

	}
}
