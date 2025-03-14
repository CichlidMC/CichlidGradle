package io.github.cichlidmc.cichlid_gradle.cache;

import io.github.cichlidmc.cichlid_gradle.run.Placeholders;
import io.github.cichlidmc.cichlid_gradle.util.Pair;
import io.github.cichlidmc.pistonmetaparser.FullVersion;
import io.github.cichlidmc.pistonmetaparser.rule.Features;
import io.github.cichlidmc.pistonmetaparser.rule.Rule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ClientRunGenerator {
    public static final Set<String> GAME_ARG_WHITELIST = Set.of(
            "--gameDir", "--workDir", "--tweakClass",
            "--assetsDir", "--assetIndex",
            "--version", "--versionType",
            "--accessToken"
    );
    public static final Set<String> JVM_ARG_WHITELIST = Set.of(
            "-XstartOnFirstThread", "-XX:HeapDumpPath",
            "-Djava.library.path", "-Djna.tmpdir", "-Dorg.lwjgl.system.SharedLibraryExtractPath", "-Dio.netty.native.workdir",
            "-Dminecraft.launcher.brand", "-Dminecraft.launcher.version",
            "-Dos.name", "-Dos.version"
    );

    public static final String XSS = "-Xss";
    public static final String FLAG = "true";

    static RunsStorage.DefaultRunConfig generate(FullVersion version) throws IOException {
        Map<String, String> gameArgs = getGameArgs(version);
        assertPlaceholdersAllowed(gameArgs);
        List<String> gameArgsList = recombineArgs(gameArgs);
        Map<String, String> jvmArgs = getJvmArgs(version);
        assertPlaceholdersAllowed(jvmArgs);
        List<String> jvmArgsList = recombineArgs(jvmArgs);
        return new RunsStorage.DefaultRunConfig(version.mainClass, gameArgsList, jvmArgsList);
    }

    private static Map<String, String> getGameArgs(FullVersion version) {
        Map<String, String> map = new HashMap<>();
        version.arguments.ifLeft(args -> {
            List<String> usedArgs = args.game.stream()
                    .filter(arg -> Rule.test(arg.rules, Features.EMPTY))
                    .flatMap(arg -> arg.values.stream())
                    .toList();
            parseArgs(usedArgs, map);
        });
        version.arguments.ifRight(string -> {
            List<String> split = List.of(string.value.split(" "));
            parseArgs(split, map);
        });

        Placeholders.fillConstant(version, map);
        return map;
    }

    private static void parseArgs(List<String> args, Map<String, String> map) {
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (!GAME_ARG_WHITELIST.contains(arg))
                continue;

            if (arg.startsWith("-")) {
                if (i + 1 < args.size()) {
                    String next = args.get(i + 1);
                    if (!next.startsWith("-")) {
                        map.put(arg, next);
                        i++; // skip past next
                        continue;
                    }
                }

                // if any of those conditions fail, it's a boolean flag
                map.put(arg, FLAG);
            } else {
                // value with no key
                throw new IllegalArgumentException("Weird args: " + args);
            }
        }
    }

    private static Map<String, String> getJvmArgs(FullVersion version) {
        // TODO: surely old versions need args too, they're not in the meta though
        if (version.arguments.isRight())
            return Map.of();

        Set<String> args = version.arguments.left().jvm.stream()
                .filter(arg -> Rule.test(arg.rules, Features.EMPTY))
                .flatMap(arg -> arg.values.stream())
                .collect(Collectors.toSet());
        
        Map<String, String> parsed = new HashMap<>();
        for (String arg : args) {
             if (arg.startsWith("-X") || arg.startsWith("-D")) {
                 if (arg.startsWith(XSS)) {
                     // special case, parameter is part of the key
                     parsed.put(XSS, FLAG);
                     continue;
                 }

                Pair<String, String> split = splitArg(arg);
                 if (JVM_ARG_WHITELIST.contains(split.left())) {
                     parsed.put(split.left(), split.right());
                 }
            }
        }
        return parsed;
    }

    private static Pair<String, String> splitArg(String arg) {
        String[] split = arg.split("=");
        return switch (split.length) {
            case 1 -> new Pair<>(split[0], FLAG);
            case 2 -> new Pair<>(split[0], split[1]);
            default -> throw new IllegalArgumentException("Weird argument: " + arg);
        };
    }

    private static List<String> recombineArgs(Map<String, String> args) {
        List<String> list = new ArrayList<>();
        args.forEach((key, value) -> {
            list.add(key);
            if (!value.equals(FLAG)) {
                list.add(value);
            }
        });
        return list;
    }

    private static void assertPlaceholdersAllowed(Map<String, String> args) {
        for (String value : args.values()) {
            if (Placeholders.isDisallowedPlaceholder(value)) {
                throw new IllegalArgumentException("Disallowed placeholder: " + value);
            }
        }
    }
}
