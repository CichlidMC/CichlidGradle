package io.github.cichlidmc.cichlid_gradle.pistonmeta.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public class MoreCodecs {
	public static final Codec<URI> URI = Codec.STRING.comapFlatMap(
			string -> {
				try {
					return DataResult.success(new URI(string));
				} catch (URISyntaxException e) {
					return DataResult.error(e::getMessage);
				}
			},
			java.net.URI::toString
	);

	public static final Codec<Pattern> REGEX = Codec.STRING.comapFlatMap(
			string -> {
				try {
					return DataResult.success(Pattern.compile(string));
				} catch (PatternSyntaxException e) {
					return DataResult.error(() -> "Invalid regex: " + e.getMessage());
				}
			},
			Pattern::pattern
	);

	public static final Codec<Date> ISO_DATE = Codec.STRING.comapFlatMap(
			string -> {
				try {
					return DataResult.success(Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse(string))));
				} catch (DateTimeException e) {
					return DataResult.error(() -> "Failed to parse date: " + e.getMessage());
				}
			},
			date -> DateTimeFormatter.ISO_INSTANT.format(date.toInstant())
	);
}
