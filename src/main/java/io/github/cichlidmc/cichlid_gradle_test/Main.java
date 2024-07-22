package io.github.cichlidmc.cichlid_gradle_test;

import java.util.ArrayList;
import java.util.List;

public class Main {
	public static void main(String[] args) {
		try {
			ArrayList<String> list = new ArrayList<>(List.of(args));
			list.add("--accessToken");
			list.add("invalid");
			list.add("--version");
			list.add("1.21");
			net.minecraft.client.main.Main.main(list.toArray(String[]::new));
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
}
