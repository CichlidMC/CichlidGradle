package io.github.cichlidmc.cichlid_gradle_test;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SplashManager;

import java.lang.reflect.Field;
import java.util.List;

public final class SplashReplacer {
	public static void hook() {
		SplashManager manager = Minecraft.getInstance().getSplashManager();
		try {
			Field field = manager.getClass().getDeclaredField("splashes");
			field.setAccessible(true);
			field.set(manager, List.of("You know what that means! FISH!"));
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("skill issue?", e);
		}
	}
}
