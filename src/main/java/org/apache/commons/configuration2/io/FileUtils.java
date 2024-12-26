/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.configuration2.io;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * This class is a subset of org.apache.commons.io.FileUtils, git-svn-id:
 * https://svn.apache.org/repos/asf/commons/proper/io/trunk@1423916 13f79535-47bb-0310-9956-ffa450edef68. The subset is
 * determined by {@link FileLocatorUtils}. The copied constants and methods are <em>literally</em> copied.<br />
 *
 * See CONFIGURATION-521 for a discussion.
 */
final class FileUtils {
    /**
     * The UTF-8 character set, used to decode octets in URLs.
     */
    private static final Charset UTF8 = StandardCharsets.UTF_8;

    /**
     * Decodes the specified URL as per RFC 3986, i.e. transforms percent-encoded octets
     * to characters by decoding with the UTF-8 character set. This function is primarily
     * intended for usage with {@link java.net.URL}, which unfortunately does not enforce
     * proper URLs. As such, this method will leniently accept invalid characters or
     * malformed percent-encoded octets and simply pass them literally through to
     * the result string. Except for rare edge cases, this will make unencoded
     * URLs pass through unaltered.
     *
     * @param url The URL to decode, may be {@code null}.
     * @return The decoded URL or {@code null} if the input was {@code null}.
     */
    static String decodeUrl(final String url) {
        if (url == null) {
            return null;
        }
        // If no '%' is found, there's nothing to decode
        if (url.indexOf('%') < 0) {
            return url;
        }
        return decodeWithPercent(url);
    }

    /**
     * Handles the main loop for URLs known to contain at least one '%'.
     */
    private static String decodeWithPercent(final String url) {
        final int n = url.length();
        final StringBuilder buffer = new StringBuilder();
        final ByteBuffer bytes = ByteBuffer.allocate(n);

        for (int i = 0; i < n; ) {
            if (url.charAt(i) == '%') {
                // Process one or more consecutive '%xx' sequences
                i = processPercentSequence(url, i, n, buffer, bytes);
            } else {
                buffer.append(url.charAt(i++));
            }
        }
        return buffer.toString();
    }

    /**
     * Processes one or more consecutive '%xx' sequences from the current index.
     * Uses the existing decodePercentSegment(...) and flushBytes(...).
     *
     * @param url    the original URL string
     * @param start  the current index (pointing at a '%')
     * @param length the length of the URL string
     * @param buffer where we accumulate decoded characters
     * @param bytes  temporary ByteBuffer for decoding
     * @return the new index (after we've processed the '%xx' blocks)
     */
    private static int processPercentSequence(final String url,
                                              int start,
                                              final int length,
                                              final StringBuilder buffer,
                                              final ByteBuffer bytes) {
        try {
            do {
                start = decodePercentSegment(url, start, bytes);
            }
            while (start < length && url.charAt(start) == '%');
        } catch (final RuntimeException ignored) {
            // Malformed percent-encoded octet => just flush and fall back
        } finally {
            flushBytes(buffer, bytes);
        }
        return start;
    }

    /**
     * Reads the two hex characters following a '%' in the given URL substring,
     * parses them as a byte, and puts them into the ByteBuffer.
     *
     * @param url   the URL string
     * @param index the current index (pointing at '%')
     * @param bytes the ByteBuffer used for decoding
     * @return the updated index in the URL string (just after the '%xx')
     */
    private static int decodePercentSegment(final String url,
                                            final int index,
                                            final ByteBuffer bytes) {
        // parse the two hex digits right after '%'
        final byte octet = (byte) Integer.parseInt(url.substring(index + 1, index + 3), 16);
        bytes.put(octet);
        // move past '%xx'
        return index + 3;
    }

    /**
     * If the ByteBuffer has accumulated any bytes, decode them as UTF-8
     * and append the result to 'buffer'. Then reset the ByteBuffer.
     */
    private static void flushBytes(final StringBuilder buffer, final ByteBuffer bytes) {
        if (bytes.position() > 0) {
            bytes.flip();
            buffer.append(UTF8.decode(bytes));
            bytes.clear();
        }
    }

    /**
     * Convert from a {@code URL} to a {@code File}.
     * <p>
     * From version 1.1 this method will decode the URL. Syntax such as {@code file:///my%20docs/file.txt} will be correctly
     * decoded to {@code /my docs/file.txt}. Starting with version 1.5, this method uses UTF-8 to decode percent-encoded
     * octets to characters. Additionally, malformed percent-encoded octets are handled leniently by passing them through
     * literally.
     *
     * @param url the file URL to convert, {@code null} returns {@code null}
     * @return the equivalent {@code File} object, or {@code null} if the URL's protocol is not {@code file}
     */
    public static File toFile(final URL url) {
        if (url == null || !"file".equalsIgnoreCase(url.getProtocol())) {
            return null;
        }
        String fileName = url.getFile().replace('/', File.separatorChar);
        fileName = decodeUrl(fileName);
        return new File(fileName);
    }

}
