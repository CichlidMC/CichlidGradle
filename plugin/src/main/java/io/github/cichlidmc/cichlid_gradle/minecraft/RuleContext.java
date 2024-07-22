package io.github.cichlidmc.cichlid_gradle.minecraft;

import java.util.List;
import java.util.Map;

import io.github.cichlidmc.cichlid_gradle.minecraft.pistonmeta.FullVersion.Os;
import io.github.cichlidmc.cichlid_gradle.minecraft.pistonmeta.FullVersion.Rule;

public record RuleContext(Map<String, Boolean> features, Os os) {
	public boolean matches(Rule rule) {
		if (rule.features().isPresent()) {
			Map<String, Boolean> required = rule.features().get().features();
			if (!required.equals(this.features))
				return false;
		}
		if (rule.os().isPresent()) {
			return rule.os().get().equals(this.os);
		}
		return true;
	}

	public boolean matches(List<Rule> rules) {
		return rules.stream().allMatch(this::matches);
	}
}
