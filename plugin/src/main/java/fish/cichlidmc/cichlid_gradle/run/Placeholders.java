package fish.cichlidmc.cichlid_gradle.run;

import fish.cichlidmc.cichlid_gradle.CichlidGradlePlugin;
import fish.cichlidmc.pistonmetaparser.FullVersion;
import fish.cichlidmc.pistonmetaparser.VersionType;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class Placeholders {
	// constant
	public static final String ASSETS_INDEX_NAME = "${assets_index_name}";
	public static final String VERSION_NAME = "${version_name}";
	public static final String VERSION_TYPE = "${version_type}";
	public static final String LAUNCHER_NAME = "${launcher_name}";
	public static final String ACCESS_TOKEN = "${auth_access_token}";
	// dynamic
	public static final String GAME_DIRECTORY = "${game_directory}";
	public static final String NATIVES_DIRECTORY = "${natives_directory}";
	public static final String GAME_ASSETS = "${game_assets}";
	public static final String ASSETS_ROOT = "${assets_root}";
	public static final String LAUNCHER_VERSION = "${launcher_version}";

	public static final Set<String> KNOWN = Set.of(
			// constant
			ASSETS_INDEX_NAME, VERSION_NAME, VERSION_TYPE, LAUNCHER_NAME, ACCESS_TOKEN,
			// dynamic
			GAME_DIRECTORY, NATIVES_DIRECTORY, GAME_ASSETS, ASSETS_ROOT, LAUNCHER_VERSION
	);

	public static final Pattern REGEX = Pattern.compile("\\$\\{(.+)}");

	public static boolean isDisallowedPlaceholder(String string) {
		return REGEX.matcher(string).matches() && !KNOWN.contains(string);
	}

	public static void fillConstant(FullVersion version, Map<String, String> args) {
		for (Map.Entry<String, String> entry : args.entrySet()) {
			String replacement = switch (entry.getValue()) {
				case ASSETS_INDEX_NAME -> version.assets;
				case VERSION_TYPE -> version.type == VersionType.RELEASE ? "release" : "snapshot";
				case LAUNCHER_NAME -> CichlidGradlePlugin.NAME;
				case ACCESS_TOKEN -> "DUMMY";
				default -> null;
			};

			if (replacement != null) {
				entry.setValue(replacement);
			}
		}
	}

	public static void fillDynamic(DynamicContext ctx, List<String> args) {
		for (int i = 0; i < args.size(); i++) {
			String arg = args.get(i);

			String replacement = switch (arg) {
				case GAME_DIRECTORY -> ctx.runDir;
				case NATIVES_DIRECTORY -> ctx.natives;
				case GAME_ASSETS, ASSETS_ROOT -> ctx.assets;
				case LAUNCHER_VERSION -> CichlidGradlePlugin.VERSION;
				// technically not what this field is for, but it's just visual, and Loom touches it too
				case VERSION_NAME -> CichlidGradlePlugin.NAME + ' ' + CichlidGradlePlugin.VERSION;
				default -> null;
			};

			if (replacement != null) {
				args.set(i, replacement);
			}
		}
	}

	public record DynamicContext(String runDir, String natives, String assets) {
		public DynamicContext(Path runDir, Path natives, Path assets) {
			this(toString(runDir), toString(natives), toString(assets));
		}

		private static String toString(Path path) {
			return path.toAbsolutePath().toString();
		}
	}
}
