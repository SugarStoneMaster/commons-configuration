/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.configuration2.benchmarks;

import org.apache.commons.configuration2.ex.ConfigurationRuntimeException;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;



@Fork(1)
@Threads(1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class UnescapeJavaBenchmark {

    @Param({
            "This string contains \\n newlines, \\t tabs, and \\u0041 some unicode.",

            "Line1\\nLine2\\nLine3\\nLine4\\nLine5\\n"
                    + "Escapes: \\t\\t\\t More escapes: \\u0020\\u0021\\u0022 ",


            "Repeated Unicode: " +
                    "\\u0041\\u0042\\u0043\\u0044\\u0045\\u0046 " +
                    "\\u00A9\\u00AE\\u00F1 " +
                    // repeated chunk
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text " +
                    "\\n\\t \\u0031 Some text "
    })
    private String input;

    @Param({"true", "false"})
    private boolean jupCompatible;


    @Benchmark
    public String benchmarkUnescapeJavaOriginal() {
        return unescapeJavaOriginal(input, jupCompatible);
    }

    @Benchmark
    public String benchmarkUnescapeJava() {
        return unescapeJava(input, jupCompatible);
    }


    private static String unescapeJavaOriginal(final String str, final boolean jupCompatible) {
        if (str == null) {
            return null;
        }
        final int sz = str.length();
        final StringBuilder out = new StringBuilder(sz);
        final StringBuilder unicode = new StringBuilder(4);
        boolean hadSlash = false;
        boolean inUnicode = false;
        for (int i = 0; i < sz; i++) {
            final char ch = str.charAt(i);
            if (inUnicode) {
                // if in unicode, then we're reading unicode
                // values in somehow
                unicode.append(ch);
                if (unicode.length() == 4) {
                    // unicode now contains the four hex digits
                    // which represents our unicode character
                    try {
                        final int value = Integer.parseInt(unicode.toString(), 16);
                        out.append((char) value);
                        unicode.setLength(0);
                        inUnicode = false;
                        hadSlash = false;
                    } catch (final NumberFormatException nfe) {
                        throw new ConfigurationRuntimeException("Unable to parse unicode value: " + unicode, nfe);
                    }
                }
                continue;
            }

            if (hadSlash) {
                // handle an escaped value
                hadSlash = false;

                switch (ch) {
                    case 'r':
                        out.append('\r');
                        break;
                    case 'f':
                        out.append('\f');
                        break;
                    case 't':
                        out.append('\t');
                        break;
                    case 'n':
                        out.append('\n');
                        break;
                    default:
                        if (!jupCompatible && ch == 'b') {
                            out.append('\b');
                        } else if (ch == 'u') {
                            // uh-oh, we're in unicode country....
                            inUnicode = true;
                        } else {
                            // JUP simply throws away the \ of unknown escape sequences
                            if (!needsUnescape(ch) && !jupCompatible) {
                                out.append('\\');
                            }
                            out.append(ch);
                        }
                        break;
                }

                continue;
            }
            if (ch == '\\') {
                hadSlash = true;
                continue;
            }
            out.append(ch);
        }

        if (hadSlash) {
            // then we're in the weird case of a \ at the end of the
            // string, let's output it anyway.
            out.append('\\');
        }

        return out.toString();
    }

    private static String unescapeJava(final String str, final boolean jupCompatible) {
        if (str == null) {
            return null;
        }
        final int sz = str.length();
        final StringBuilder out = new StringBuilder(sz);
        final StringBuilder unicode = new StringBuilder(4);
        boolean hadSlash = false;
        boolean inUnicode = false;

        for (int i = 0; i < sz; i++) {
            final char ch = str.charAt(i);

            // 1) If currently reading a Unicode escape, process that first
            if (inUnicode) {
                // If handleUnicodeChar() returns true, it means
                // we finished processing the 4 hex digits
                if (handleUnicodeChar(ch, unicode, out)) {
                    inUnicode = false;
                    hadSlash = false;
                }
                continue;
            }

            // 2) If we had a preceding backslash, handle the escape sequence
            if (hadSlash) {
                // If handleEscapedChar() returns true, that means we have '\\u'
                // so we need to start reading a Unicode sequence
                if (handleEscapedChar(ch, jupCompatible, out)) {
                    inUnicode = true;
                }
                hadSlash = false;
                continue;
            }

            // 3) Check if the current character is a backslash, which may start an escape
            if (ch == '\\') {
                hadSlash = true;
                continue;
            }

            // 4) Otherwise, it's a normal character
            out.append(ch);
        }

        // If the very last character in the string was a lone backslash, append it
        if (hadSlash) {
            out.append('\\');
        }

        return out.toString();
    }


    private static boolean handleUnicodeChar(final char ch,
                                             final StringBuilder unicode,
                                             final StringBuilder out) {
        unicode.append(ch);
        if (unicode.length() == 4) {
            // We have 4 hex digits in 'unicode'
            try {
                final int value = Integer.parseInt(unicode.toString(), 16);
                out.append((char) value);
                unicode.setLength(0); // reset for any subsequent escapes
                return true; // signals "done with inUnicode"
            } catch (final NumberFormatException nfe) {
                throw new ConfigurationRuntimeException(
                        "Unable to parse unicode value: " + unicode,
                        nfe
                );
            }
        }
        return false;
    }


    private static boolean handleEscapedChar(final char ch,
                                             final boolean jupCompatible,
                                             final StringBuilder out) {
        switch (ch) {
            case 'r':
                out.append('\r');
                break;
            case 'f':
                out.append('\f');
                break;
            case 't':
                out.append('\t');
                break;
            case 'n':
                out.append('\n');
                break;
            default:
                if (!jupCompatible && ch == 'b') {
                    out.append('\b');
                } else if (ch == 'u') {
                    // Next characters will be the 4 hex digits for a Unicode escape
                    return true; // signals "start inUnicode"
                } else {
                    // If it's something like '\x' or an unknown escape,
                    // JUP behavior is to discard the backslash,
                    // classic Java might keep it
                    if (!needsUnescape(ch) && !jupCompatible) {
                        // Keep the backslash in output
                        out.append('\\');
                    }
                    out.append(ch);
                }
                break;
        }
        return false;
    }

    private static boolean needsUnescape(final char ch) {
        return ":#=!\\\'\"".indexOf(ch) >= 0;
    }


}