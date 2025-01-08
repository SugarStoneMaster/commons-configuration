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

import org.apache.commons.configuration2.convert.DefaultConversionHandler;
import org.apache.commons.configuration2.ex.ConversionException;
import org.openjdk.jmh.annotations.*;

import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.apache.commons.configuration2.convert.PropertyConverter.*;

@Fork(1)
@Threads(1)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.Throughput)
@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class PropertyConverterBenchmark {

    /** The target class for the conversion. */
    @Param({
            "java.lang.Integer",
            "java.lang.Long",
            "java.math.BigInteger",
            "java.math.BigDecimal"
    })
    private String targetClassName;

    /** The value to be converted as an Object. */
    @Param({
            "42",
            "2147483647",
            "314159265",
            "0"
    })
    private String inputValue;


    /** The resolved target Class<?>. */
    private Class<?> targetClass;


    /** The conversion handler. */
    private DefaultConversionHandler conversionHandler;

    @Setup(Level.Trial)
    public void setup() throws ClassNotFoundException {
        // Resolve the target class from its name
        targetClass = Class.forName(targetClassName);

        // Initialize the conversion handler
        conversionHandler = new DefaultConversionHandler();
    }

    @Benchmark
    public Object benchmarkToOriginal() {
        try {
            return toOriginal(targetClass, inputValue, conversionHandler);
        } catch (Exception e) {
            return null; // Handle exception for invalid conversions
        }
    }

    @Benchmark
    public Object benchmarkTo() {
        try {
            return to(targetClass, inputValue, conversionHandler);
        } catch (Exception e) {
            return null; // Handle exception for invalid conversions
        }
    }



    private static Object toOriginal(final Class<?> cls, final Object value, final DefaultConversionHandler convHandler) throws ConversionException {
        if (cls.isInstance(value)) {
            return value; // no conversion needed
        }

        if (String.class.equals(cls)) {
            return String.valueOf(value);
        }
        if (Boolean.class.equals(cls) || Boolean.TYPE.equals(cls)) {
            return toBoolean(value);
        }
        if (Character.class.equals(cls) || Character.TYPE.equals(cls)) {
            return toCharacter(value);
        }
        if (Number.class.isAssignableFrom(cls) || cls.isPrimitive()) {
            if (Integer.class.equals(cls) || Integer.TYPE.equals(cls)) {
                return toInteger(value);
            }
            if (Long.class.equals(cls) || Long.TYPE.equals(cls)) {
                return toLong(value);
            }
            if (Byte.class.equals(cls) || Byte.TYPE.equals(cls)) {
                return toByte(value);
            }
            if (Short.class.equals(cls) || Short.TYPE.equals(cls)) {
                return toShort(value);
            }
            if (Float.class.equals(cls) || Float.TYPE.equals(cls)) {
                return toFloat(value);
            }
            if (Double.class.equals(cls) || Double.TYPE.equals(cls)) {
                return toDouble(value);
            }
            if (BigInteger.class.equals(cls)) {
                return toBigInteger(value);
            }
            if (BigDecimal.class.equals(cls)) {
                return toBigDecimal(value);
            }
            return toNumber(value, cls);
        } else if (Date.class.equals(cls)) {
            return toDate(value, convHandler.getDateFormat());
        } else if (Calendar.class.equals(cls)) {
            return toCalendar(value, convHandler.getDateFormat());
        } else if (File.class.equals(cls)) {
            return toFile(value);
        } else if (Path.class.equals(cls)) {
            return toPath(value);
        } else if (URI.class.equals(cls)) {
            return toURI(value);
        } else if (URL.class.equals(cls)) {
            return toURL(value);
        } else if (Pattern.class.equals(cls)) {
            return toPattern(value);
        } else if (Locale.class.equals(cls)) {
            return toLocale(value);
        } else if (cls.isEnum()) {
            return convertToEnum(cls, value);
        } else if (Color.class.equals(cls)) {
            return toColor(value);
        } else if (cls.getName().equals(INTERNET_ADDRESS_CLASSNAME_JAVAX)) {
            // javamail-1.* With javax.mail.* namespace.
            return toInternetAddress(value, INTERNET_ADDRESS_CLASSNAME_JAVAX);
        } else if (cls.getName().equals(INTERNET_ADDRESS_CLASSNAME_JAKARTA)) {
            // javamail-2.0+, with jakarta.mail.* namespace.
            return toInternetAddress(value, INTERNET_ADDRESS_CLASSNAME_JAKARTA);
        } else if (InetAddress.class.isAssignableFrom(cls)) {
            return toInetAddress(value);
        } else if (Duration.class.equals(cls)) {
            return toDuration(value);
        }

        throw new ConversionException("The value '" + value + "' (" + value.getClass() + ")" + CANT_BE_CONVERT + cls.getName() + " object");
    }


    private static Object to(final Class<?> cls, final Object value, final DefaultConversionHandler convHandler)
            throws ConversionException {

        // 1) If the value is already of type cls, no conversion needed
        if (cls.isInstance(value)) {
            return value;
        }

        // 2) Handle simple (non-numeric) cases
        if (String.class.equals(cls)) {
            return String.valueOf(value);
        }
        if (isBooleanType(cls)) {
            return toBoolean(value);
        }
        if (isCharacterType(cls)) {
            return toCharacter(value);
        }

        // 3) Handle numeric types
        if (isNumericType(cls)) {
            return handleNumericType(cls, value);
        }

        // 4) Delegate the remaining types to a helper that groups them by category
        final Object result = handleOtherTypes(cls, value, convHandler);
        if (result != null) {
            return result;
        }

        // 5) If none of the groups returned a result, throw an exception
        throw new ConversionException(
                "The value '" + value + "' (" + value.getClass() + ") "
                        + CANT_BE_CONVERT + cls.getName() + " object"
        );
    }

    /**
     * Attempts conversions for all remaining types (date/time, file/network, patterns, etc.).
     * Returns {@code null} if none matched, signaling we should throw.
     */
    private static Object handleOtherTypes(final Class<?> cls, final Object value, final DefaultConversionHandler convHandler) {
        // A) Date/time types
        final Object dateTime = handleDateTimeType(cls, value, convHandler);
        if (dateTime != null) {
            return dateTime;
        }

        // B) File/network types
        final Object fileNet = handleFileNetworkType(cls, value);
        if (fileNet != null) {
            return fileNet;
        }

        // C) Misc. special-case types (Regex, Locale, Enum, Color, etc.)
        final Object special = handleSpecialCaseType(cls, value);
        if (special != null) {
            return special;
        }

        // None of the above matched => return null so the caller can throw
        return null;
    }

    /**
     * Handles Date/Calendar conversions, returns null if cls does not match.
     */
    private static Object handleDateTimeType(final Class<?> cls, final Object value, final DefaultConversionHandler convHandler) {
        if (Date.class.equals(cls)) {
            return toDate(value, convHandler.getDateFormat());
        }
        if (Calendar.class.equals(cls)) {
            return toCalendar(value, convHandler.getDateFormat());
        }
        return null;
    }

    /**
     * Handles File/Path/URI/URL/InetAddress conversions, returns null if cls does not match.
     */
    private static Object handleFileNetworkType(final Class<?> cls, final Object value) {
        if (File.class.equals(cls)) {
            return toFile(value);
        }
        if (Path.class.equals(cls)) {
            return toPath(value);
        }
        if (URI.class.equals(cls)) {
            return toURI(value);
        }
        if (URL.class.equals(cls)) {
            return toURL(value);
        }
        if (InetAddress.class.isAssignableFrom(cls)) {
            return toInetAddress(value);
        }
        return null;
    }

    /**
     * Handles Patterns, Locales, Enums, Color, mail InternetAddress, Duration, etc.
     * Returns null if cls does not match any known special case.
     */
    private static Object handleSpecialCaseType(final Class<?> cls, final Object value) {
        if (Pattern.class.equals(cls)) {
            return toPattern(value);
        }
        if (Locale.class.equals(cls)) {
            return toLocale(value);
        }
        if (cls.isEnum()) {
            return convertToEnum(cls, value);
        }
        if (Color.class.equals(cls)) {
            return toColor(value);
        }
        if (cls.getName().equals(INTERNET_ADDRESS_CLASSNAME_JAVAX)) {
            return toInternetAddress(value, INTERNET_ADDRESS_CLASSNAME_JAVAX);
        }
        if (cls.getName().equals(INTERNET_ADDRESS_CLASSNAME_JAKARTA)) {
            return toInternetAddress(value, INTERNET_ADDRESS_CLASSNAME_JAKARTA);
        }
        if (Duration.class.equals(cls)) {
            return toDuration(value);
        }
        return null;
    }

    /** True if cls is either Boolean.class or boolean.class. */
    private static boolean isBooleanType(final Class<?> cls) {
        return Boolean.class.equals(cls) || Boolean.TYPE.equals(cls);
    }

    /** True if cls is either Character.class or char.class. */
    private static boolean isCharacterType(final Class<?> cls) {
        return Character.class.equals(cls) || Character.TYPE.equals(cls);
    }

    /** True if cls is a numeric wrapper (extends Number) or a numeric primitive. */
    private static boolean isNumericType(final Class<?> cls) {
        return Number.class.isAssignableFrom(cls) || cls.isPrimitive();
    }


}