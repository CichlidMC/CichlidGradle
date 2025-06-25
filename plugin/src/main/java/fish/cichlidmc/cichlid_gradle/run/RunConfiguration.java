package fish.cichlidmc.cichlid_gradle.run;

import fish.cichlidmc.cichlid_gradle.util.Distribution;
import fish.cichlidmc.cichlid_gradle.util.ListPatch;
import org.gradle.api.Named;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.SourceSet;

import java.util.Locale;

public interface RunConfiguration extends Named {
	Property<Type> getType();
	Property<String> getMainClass();
	DirectoryProperty getRunDir();
	Property<String> getSourceSet();
	Property<ListPatch<String>> getProgramArgs();
	Property<ListPatch<String>> getJvmArgs();

	void client();

	void server();

	void jvmArg(String arg);

	void programArg(String arg);

	void sourceSet(SourceSet sourceSet);

	enum Type {
		CLIENT(Distribution.CLIENT),
		SERVER(Distribution.SERVER);

		public final String name;
		public final Distribution asDist;

		Type(Distribution asDist) {
			this.name = this.name().toLowerCase(Locale.ROOT);
			this.asDist = asDist;
		}

		public boolean isCompatibleWith(Distribution dist) {
			return this.asDist == dist || dist == Distribution.MERGED;
		}

		@Override
		public String toString() {
			return this.name;
		}
	}
}
