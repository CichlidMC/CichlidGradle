package io.github.tropheusj.cichlid_gradle.minecraft.mcmaven;

import org.gradle.internal.resource.connector.ResourceConnectorFactory;
import org.gradle.internal.service.ServiceRegistration;
import org.gradle.internal.service.scopes.AbstractPluginServiceRegistry;

public class McMavenPluginServiceRegistry extends AbstractPluginServiceRegistry {
	@Override
	public void registerGlobalServices(ServiceRegistration registration) {
		System.out.println("!!!!!!!!!!!");
		registration.addProvider(new GlobalScopeServices());
	}

	private static class GlobalScopeServices {
		@SuppressWarnings("unused") // magic
		ResourceConnectorFactory createMcMavenConnectorFactory() {
			return new McMavenConnectorFactory();
		}
	}
}
