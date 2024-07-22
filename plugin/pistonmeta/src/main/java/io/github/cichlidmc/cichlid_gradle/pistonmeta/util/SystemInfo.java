package io.github.cichlidmc.cichlid_gradle.pistonmeta.util;

import java.util.Locale;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public record SystemInfo(OperatingSystem os, Architecture arch, String osVersion) {
	public static final SystemInfo INSTANCE = new SystemInfo(OperatingSystem.CURRENT, Architecture.CURRENT, System.getProperty("os.version"));

	public enum OperatingSystem {
		WINDOWS, OSX, LINUX;

		public static final Codec<OperatingSystem> CODEC = Codec.STRING.comapFlatMap(OperatingSystem::byName, os -> os.name);
		public static final OperatingSystem CURRENT = findCurrent();

		public final String name = this.name().toLowerCase(Locale.ROOT);

		private static OperatingSystem findCurrent() {
			String string = System.getProperty("os.name").toLowerCase(Locale.ROOT);
			if (string.contains("win")) {
				return WINDOWS;
			} else if (string.contains("mac")) {
				return OSX;
			} else {
				return LINUX; // best effort guess
			}
		}

		private static DataResult<OperatingSystem> byName(String name) {
			return switch (name) {
				case "windows" -> DataResult.success(WINDOWS);
				case "osx" -> DataResult.success(OSX);
				case "linux" -> DataResult.success(LINUX);
				default -> DataResult.error(() -> "No OS named " + name);
			};
		}
	}

	public enum Architecture {
		X64, X86, ARM64, ARM32;

		public static final Codec<Architecture> CODEC = Codec.STRING.comapFlatMap(Architecture::byName, arch -> arch.name);
		public static final Architecture CURRENT = findCurrent();

		public final String name = this.name().toLowerCase(Locale.ROOT);

		private static Architecture findCurrent() {
			String arch = System.getProperty("os.arch");
			boolean is64Bit = arch.contains("64") || arch.startsWith("armv8");

			if (arch.startsWith("arm") || arch.startsWith("aarch64")) {
				return is64Bit ? ARM64 : ARM32;
			} else {
				return is64Bit ? X64 : X86;
			}
		}

		private static DataResult<Architecture> byName(String name) {
			return switch (name) {
				case "x64" -> DataResult.success(X64);
				case "x86" -> DataResult.success(X86);
				case "arm64" -> DataResult.success(ARM64);
				case "arm32" -> DataResult.success(ARM32);
				default -> DataResult.error(() -> "No arch named " + name);
			};
		}
	}
}
