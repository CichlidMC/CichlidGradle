package io.github.tropheusj.cichlid_gradle.util;

import java.net.URI;
import java.net.URISyntaxException;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

public class UriCodec {
	public static final Codec<URI> INSTANCE = Codec.STRING.comapFlatMap(UriCodec::parse, URI::toString);

	private static DataResult<URI> parse(String url) {
		try {
			return DataResult.success(new URI(url));
		} catch (URISyntaxException e) {
			return DataResult.error(e::getMessage);
		}
	}
}
