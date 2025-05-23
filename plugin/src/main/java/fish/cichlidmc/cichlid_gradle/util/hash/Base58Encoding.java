/*
 * Copyright 2011 Google Inc.
 * Copyright 2018 Andreas Schildbach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fish.cichlidmc.cichlid_gradle.util.hash;

import java.util.Arrays;

/**
 * This class is a modified copy of the base58 class located
 * <a href="https://github.com/Anujraval24/Base58Encoding/blob/master/base58.java">here</a>.
 * It's licensed under Apache 2.0, which can be found <a href="https://www.apache.org/licenses/LICENSE-2.0">here</a>.
 * <p>
 * The following changes have been made:
 * <ul>
 *     <li>The class has been renamed to {@code Base58Encoding} to match code style.</li>
 *     <li>This javadoc has been added. Existing javadoc has been removed.</li>
 *     <li>The class has been stripped down to only do plain encoding. All unnecessary code has been removed.</li>
 *     <li>
 *         The encode method has been modified to accept a custom alphabet.
 *         The {@code ALPHABET} constant has been renamed to reflect this.
 *     </li>
 * </ul>
 */
public class Base58Encoding {
	public static final char[] DEFAULT_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();

	static String encode(byte[] input) {
		return encode(input, DEFAULT_ALPHABET);
	}

	public static String encode(byte[] input, char[] ALPHABET) {
		// this block of code has been added by CichlidGradle.
		// -----------------------------
		if (ALPHABET.length != 58) {
			throw new IllegalArgumentException("Alphabet must have 58 characters: " + new String(ALPHABET));
		}

		char ENCODED_ZERO = ALPHABET[0];
		// -----------------------------

		if (input.length == 0) {
			return "";
		}
		// Count leading zeros.
		int zeros = 0;
		while (zeros < input.length && input[zeros] == 0) {
			++zeros;
		}
		// Convert base-256 digits to base-58 digits (plus conversion to ASCII characters)
		input = Arrays.copyOf(input, input.length); // since we modify it in-place
		char[] encoded = new char[input.length * 2]; // upper bound
		int outputStart = encoded.length;
		for (int inputStart = zeros; inputStart < input.length; ) {
			encoded[--outputStart] = ALPHABET[divmod(input, inputStart, 256, 58)];
			if (input[inputStart] == 0) {
				++inputStart; // optimization - skip leading zeros
			}
		}
		// Preserve exactly as many leading encoded zeros in output as there were leading zeros in input.
		while (outputStart < encoded.length && encoded[outputStart] == ENCODED_ZERO) {
			++outputStart;
		}
		while (--zeros >= 0) {
			encoded[--outputStart] = ENCODED_ZERO;
		}
		// Return encoded string (including encoded leading zeros).
		return new String(encoded, outputStart, encoded.length - outputStart);
	}

	private static byte divmod(byte[] number, int firstDigit, int base, int divisor) {
		// this is just long division which accounts for the base of the input digits
		int remainder = 0;
		for (int i = firstDigit; i < number.length; i++) {
			int digit = (int) number[i] & 0xFF;
			int temp = remainder * base + digit;
			number[i] = (byte) (temp / divisor);
			remainder = temp % divisor;
		}
		return (byte) remainder;
	}
}
