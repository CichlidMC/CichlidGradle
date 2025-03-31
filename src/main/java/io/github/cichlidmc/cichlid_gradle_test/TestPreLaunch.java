package io.github.cichlidmc.cichlid_gradle_test;

import io.github.cichlidmc.cichlid.api.loaded.Mod;
import io.github.cichlidmc.cichlid.api.mod.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestPreLaunch implements PreLaunchEntrypoint {
	public static final Logger LOGGER = LoggerFactory.getLogger(TestPreLaunch.class);

	@Override
	public void preLaunch(Mod mod) {
		LOGGER.info("Works:tm:");
		LOGGER.info("java.home: " + System.getProperty("java.home"));
	}
}
