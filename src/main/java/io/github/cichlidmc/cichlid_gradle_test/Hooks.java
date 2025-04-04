package io.github.cichlidmc.cichlid_gradle_test;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SplashManager;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.lang.reflect.Field;
import java.util.List;

public final class Hooks {
	public static void fish() {
		SplashManager manager = Minecraft.getInstance().getSplashManager();
		try {
			Field field = manager.getClass().getDeclaredField("splashes");
			field.setAccessible(true);
			field.set(manager, List.of("You know what that means! FISH!"));
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException("skill issue?", e);
		}
	}

	public static void registerItems() {
		ResourceKey<Item> id = ResourceKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("h", "the_doohickey"));
		Registry.register(BuiltInRegistries.ITEM, id, new Item(new Item.Properties().setId(id).stacksTo(2)));
	}
}
