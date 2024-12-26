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

package org.apache.commons.configuration2.convert;

import java.awt.Color;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.configuration2.ex.ConversionException;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * A utility class to convert the configuration properties into any type.
 *
 * @since 2.8.0
 */
public final class PropertyConverter {

    /** String literal for the value. */
    private static final String THE_VALUE = "The value ";

    /** String literal for could not convert. */
    private static final String COULD_NOT_CONVERT = "Could not convert ";

    /** String literal for cant convert color. */
    private static final String CANT_CONVERT_COLOR = " can't be converted to a Color";

    /** String literal for cant be converted. */
    private static final String CANT_BE_CONVERT = " can't be converted to a ";

    /** Constant for the prefix of hex numbers. */
    private static final String HEX_PREFIX = "0x";

    /** Constant for the radix of hex numbers. */
    private static final int HEX_RADIX = 16;

    /** Constant for the prefix of binary numbers. */
    private static final String BIN_PREFIX = "0b";

    /** Constant for the radix of binary numbers. */
    private static final int BIN_RADIX = 2;

    /** Constant for the argument classes of the Number constructor that takes a String. */
    private static final Class<?>[] CONSTR_ARGS = {String.class};

    /** The fully qualified name of {@code javax.mail.internet.InternetAddress}, as used in the javamail-1.* API.  */
    private static final String INTERNET_ADDRESS_CLASSNAME_JAVAX = "javax.mail.internet.InternetAddress";

    /** The fully qualified name of {@code jakarta.mail.internet.InternetAddress}, as used in the javamail-2.0+ API. */
    private static final String INTERNET_ADDRESS_CLASSNAME_JAKARTA = "jakarta.mail.internet.InternetAddress";

    /**
     * Converts a value to a constant of an enumeration class.
     *
     * @param enumClass the enumeration class
     * @param value the value to be converted
     * @return the converted value
     */
    @SuppressWarnings("unchecked")
    // conversion is safe because we know that the class is an Enum class
    private static Object convertToEnum(final Class<?> enumClass, final Object value) {
        return toEnum(value, enumClass.asSubclass(Enum.class));
    }

    public static Object to(final Class<?> cls, final Object value, final DefaultConversionHandler convHandler)
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

    /**
     * Handles all numeric conversions (Integer, Long, Byte, Short, Float,
     * Double, BigInteger, BigDecimal, or a generic Number).
     */
    private static Object handleNumericType(final Class<?> cls, final Object value) {
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
        // Fallback for other numeric wrapper classes
        return toNumber(value, cls);
    }

    /**
     * Converts the specified object into a BigDecimal.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to a BigDecimal
     */
    public static BigDecimal toBigDecimal(final Object value) throws ConversionException {
        final Number n = toNumber(value, BigDecimal.class);
        if (n instanceof BigDecimal) {
            return (BigDecimal) n;
        }
        return BigDecimal.valueOf(n.doubleValue());
    }

    /**
     * Converts the specified object into a BigInteger.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to a BigInteger
     */
    public static BigInteger toBigInteger(final Object value) throws ConversionException {
        final Number n = toNumber(value, BigInteger.class);
        if (n instanceof BigInteger) {
            return (BigInteger) n;
        }
        return BigInteger.valueOf(n.longValue());
    }

    /**
     * Converts the specified object into a Boolean. Internally the {@code org.apache.commons.lang.BooleanUtils} class from
     * the <a href="https://commons.apache.org/lang/">Commons Lang</a> project is used to perform this conversion. This
     * class accepts some more tokens for the boolean value of <b>true</b>, e.g. {@code yes} and {@code on}. Please refer to
     * the documentation of this class for more details.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to a boolean
     */
    public static Boolean toBoolean(final Object value) throws ConversionException {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (!(value instanceof String)) {
            throw new ConversionException(THE_VALUE + value + " can't be converted to a Boolean object");
        }
        final Boolean b = BooleanUtils.toBooleanObject((String) value);
        if (b == null) {
            throw new ConversionException(THE_VALUE + value + " can't be converted to a Boolean object");
        }
        return b;
    }

    /**
     * Converts the specified object into a Byte.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to a byte
     */
    public static Byte toByte(final Object value) throws ConversionException {
        final Number n = toNumber(value, Byte.class);
        if (n instanceof Byte) {
            return (Byte) n;
        }
        return n.byteValue();
    }

    /**
     * Converts the specified object into a Calendar.
     *
     * @param value the value to convert
     * @param format the DateFormat pattern to parse String values
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to a Calendar
     */
    public static Calendar toCalendar(final Object value, final String format) throws ConversionException {
        if (value instanceof Calendar) {
            return (Calendar) value;
        }
        if (value instanceof Date) {
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime((Date) value);
            return calendar;
        }
        if (!(value instanceof String)) {
            throw new ConversionException(THE_VALUE + value + " can't be converted to a Calendar");
        }
        try {
            final Calendar calendar = Calendar.getInstance();
            calendar.setTime(new SimpleDateFormat(format).parse((String) value));
            return calendar;
        } catch (final ParseException e) {
            throw new ConversionException(THE_VALUE + value + " can't be converted to a Calendar", e);
        }
    }

    /**
     * Converts the specified value object to a {@code Character}. This method converts the passed in object to a string. If
     * the string has exactly one character, this character is returned as result. Otherwise, conversion fails.
     *
     * @param value the value to be converted
     * @return the resulting {@code Character} object
     * @throws ConversionException if the conversion is not possible
     */
    public static Character toCharacter(final Object value) throws ConversionException {
        final String strValue = String.valueOf(value);
        if (strValue.length() == 1) {
            return Character.valueOf(strValue.charAt(0));
        }
        throw new ConversionException(String.format("The value '%s' cannot be converted to a Character object!", strValue));
    }

    /**
     * Converts the specified object into a Color. If the value is a String, the format allowed is
     * (#)?[0-9A-F]{6}([0-9A-F]{2})?. Examples:
     * <ul>
     * <li>FF0000 (red)</li>
     * <li>0000FFA0 (semi transparent blue)</li>
     * <li>#CCCCCC (gray)</li>
     * <li>#00FF00A0 (semi transparent green)</li>
     * </ul>
     *
     * @param value the value to convert
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to a Color
     */
    public static Color toColor(final Object value) throws ConversionException {
        if (value instanceof Color) {
            return (Color) value;
        }
        if (!(value instanceof String) || StringUtils.isBlank((String) value)) {
            throw new ConversionException(THE_VALUE + value + CANT_CONVERT_COLOR);
        }
        String color = ((String) value).trim();

        final int[] components = new int[3];

        // check the size of the string
        final int minlength = components.length * 2;
        if (color.length() < minlength) {
            throw new ConversionException(THE_VALUE + value + CANT_CONVERT_COLOR);
        }

        // remove the leading #
        if (color.startsWith("#")) {
            color = color.substring(1);
        }

        try {
            // parse the components
            for (int i = 0; i < components.length; i++) {
                components[i] = Integer.parseInt(color.substring(2 * i, 2 * i + 2), HEX_RADIX);
            }

            // parse the transparency
            final int alpha;
            if (color.length() >= minlength + 2) {
                alpha = Integer.parseInt(color.substring(minlength, minlength + 2), HEX_RADIX);
            } else {
                alpha = Color.black.getAlpha();
            }

            return new Color(components[0], components[1], components[2], alpha);
        } catch (final Exception e) {
            throw new ConversionException(THE_VALUE + value + CANT_CONVERT_COLOR, e);
        }
    }

    /**
     * Converts the specified object into a Date.
     *
     * @param value the value to convert
     * @param format the DateFormat pattern to parse String values
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to a Calendar
     */
    public static Date toDate(final Object value, final String format) throws ConversionException {
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Calendar) {
            return ((Calendar) value).getTime();
        }
        if (!(value instanceof String)) {
            throw new ConversionException(THE_VALUE + value + " can't be converted to a Date");
        }
        try {
            return new SimpleDateFormat(format).parse((String) value);
        } catch (final ParseException e) {
            throw new ConversionException(THE_VALUE + value + " can't be converted to a Date", e);
        }
    }

    /**
     * Converts the specified object into a Double.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to a Double
     */
    public static Double toDouble(final Object value) throws ConversionException {
        final Number n = toNumber(value, Double.class);
        if (n instanceof Double) {
            return (Double) n;
        }
        return Double.valueOf(n.doubleValue());
    }

    /**
     * Converts the specified object into a Duration.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to a Duration
     * @since 2.8.0
     */
    public static Duration toDuration(final Object value) throws ConversionException {
        if (value instanceof Duration) {
            return (Duration) value;
        }
        if (value instanceof CharSequence) {
            try {
                return Duration.parse((CharSequence) value);
            } catch (final DateTimeParseException e) {
                throw new ConversionException(COULD_NOT_CONVERT + value + " to Duration", e);
            }
        }
        throw new ConversionException(THE_VALUE + value + " can't be converted to a Duration");
    }

    /**
     * Converts the specified value into an {@link Enum}.
     *
     * @param value the value to convert
     * @param cls the type of the enumeration
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to an enumeration
     *
     * @since 1.5
     */
    static <E extends Enum<E>> E toEnum(final Object value, final Class<E> cls) throws ConversionException {
        if (value.getClass().equals(cls)) {
            return cls.cast(value);
        }
        if (value instanceof String) {
            try {
                return Enum.valueOf(cls, (String) value);
            } catch (final Exception e) {
                throw new ConversionException(THE_VALUE + value + CANT_BE_CONVERT + cls.getName());
            }
        }
        if (!(value instanceof Number)) {
            throw new ConversionException(THE_VALUE + value + CANT_BE_CONVERT + cls.getName());
        }
        try {
            final E[] enumConstants = cls.getEnumConstants();
            return enumConstants[((Number) value).intValue()];
        } catch (final Exception e) {
            throw new ConversionException(THE_VALUE + value + CANT_BE_CONVERT + cls.getName());
        }
    }

    /**
     * Converts the specified object into a File.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to a File
     * @since 2.3
     */
    public static File toFile(final Object value) throws ConversionException {
        if (value instanceof File) {
            return (File) value;
        }
        if (value instanceof Path) {
            return ((Path) value).toFile();
        }
        if (value instanceof String) {
            return new File((String) value);
        }
        throw new ConversionException(THE_VALUE + value + " can't be converted to a File");
    }

    /**
     * Converts the specified object into a Float.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to a Float
     */
    public static Float toFloat(final Object value) throws ConversionException {
        final Number n = toNumber(value, Float.class);
        if (n instanceof Float) {
            return (Float) n;
        }
        return Float.valueOf(n.floatValue());
    }

    /**
     * Converts the specified value into an internet address.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to a InetAddress
     *
     * @since 1.5
     */
    static InetAddress toInetAddress(final Object value) throws ConversionException {
        if (value instanceof InetAddress) {
            return (InetAddress) value;
        }
        if (!(value instanceof String)) {
            throw new ConversionException(THE_VALUE + value + " can't be converted to a InetAddress");
        }
        try {
            return InetAddress.getByName((String) value);
        } catch (final UnknownHostException e) {
            throw new ConversionException(THE_VALUE + value + " can't be converted to a InetAddress", e);
        }
    }

    /**
     * Converts the specified object into an Integer.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to an integer
     */
    public static Integer toInteger(final Object value) throws ConversionException {
        final Number n = toNumber(value, Integer.class);
        if (n instanceof Integer) {
            return (Integer) n;
        }
        return n.intValue();
    }

    /**
     * Converts the specified value into an email address with the given class name.
     *
     * @param value the value to convert
     * @param targetClassName the fully qualified name of the {@code InternetAddress} class to convert to, e.g.,
     *      {@value #INTERNET_ADDRESS_CLASSNAME_JAVAX} or {@value #INTERNET_ADDRESS_CLASSNAME_JAKARTA}
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to an email address
     *
     * @since 1.5
     */
    static Object toInternetAddress(final Object value, final String targetClassName) throws ConversionException {
        if (value.getClass().getName().equals(targetClassName)) {
            return value;
        }
        if (!(value instanceof String)) {
            throw new ConversionException(THE_VALUE + value + " can't be converted to an InternetAddress");
        }
        try {
            final Constructor<?> ctor = Class.forName(targetClassName).getConstructor(String.class);
            return ctor.newInstance(value);
        } catch (final Exception e) {
            throw new ConversionException(THE_VALUE + value + " can't be converted to an InternetAddress", e);
        }
    }

    /**
     * Converts the specified object into a Locale.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to a Locale
     */
    public static Locale toLocale(final Object value) throws ConversionException {
        if (value instanceof Locale) {
            return (Locale) value;
        }
        if (!(value instanceof String)) {
            throw new ConversionException(THE_VALUE + value + " can't be converted to a Locale");
        }
        final String[] elements = ((String) value).split("_");
        final int size = elements.length;

        if (size >= 1 && (elements[0].length() == 2 || elements[0].isEmpty())) {
            final String language = elements[0];
            final String country = size >= 2 ? elements[1] : "";
            final String variant = size >= 3 ? elements[2] : "";

            return new Locale(language, country, variant);
        }
        throw new ConversionException(THE_VALUE + value + " can't be converted to a Locale");
    }

    /**
     * Converts the specified object into a Long.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to a Long
     */
    public static Long toLong(final Object value) throws ConversionException {
        final Number n = toNumber(value, Long.class);
        if (n instanceof Long) {
            return (Long) n;
        }
        return n.longValue();
    }

    /**
     * Tries to convert the specified object into a number object. This method is used by the conversion methods for number
     * types. Note that the return value is not in always of the specified target class, but only if a new object has to be
     * created.
     *
     * @param value the value to be converted (must not be <b>null</b>)
     * @param targetClass the target class of the conversion (must be derived from {@link Number})
     * @return the converted number
     * @throws ConversionException if the object cannot be converted
     */
    static Number toNumber(final Object value, final Class<?> targetClass) throws ConversionException {
        if (value instanceof Number) {
            return (Number) value;
        }
        final String str = Objects.toString(value, null);
        if (StringUtils.startsWithAny(str, HEX_PREFIX)) {
            try {
                return new BigInteger(str.substring(HEX_PREFIX.length()), HEX_RADIX);
            } catch (final NumberFormatException nex) {
                throw new ConversionException(COULD_NOT_CONVERT + str + " to " + targetClass.getName() + "! Invalid hex number.", nex);
            }
        }

        if (StringUtils.startsWithAny(str, BIN_PREFIX)) {
            try {
                return new BigInteger(str.substring(BIN_PREFIX.length()), BIN_RADIX);
            } catch (final NumberFormatException nex) {
                throw new ConversionException(COULD_NOT_CONVERT + str + " to " + targetClass.getName() + "! Invalid binary number.", nex);
            }
        }

        try {
            final Constructor<?> constr = targetClass.getConstructor(CONSTR_ARGS);
            return (Number) constr.newInstance(str);
        } catch (final InvocationTargetException itex) {
            throw new ConversionException(COULD_NOT_CONVERT + str + " to " + targetClass.getName(), itex.getTargetException());
        } catch (final Exception ex) {
            // Treat all possible exceptions the same way
            throw new ConversionException("Conversion error when trying to convert " + str + " to " + targetClass.getName(), ex);
        }
    }

    /**
     * Converts the specified object into a Path.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to a Path
     * @since 2.3
     */
    public static Path toPath(final Object value) throws ConversionException {
        if (value instanceof File) {
            return ((File) value).toPath();
        }
        if (value instanceof Path) {
            return (Path) value;
        }
        if (value instanceof String) {
            return Paths.get((String) value);
        }
        throw new ConversionException(THE_VALUE + value + " can't be converted to a Path");
    }

    /**
     * Converts the specified object into a Pattern.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to a Pattern
     */
    public static Pattern toPattern(final Object value) throws ConversionException {
        if (value instanceof Pattern) {
            return (Pattern) value;
        }
        if (!(value instanceof String)) {
            throw new ConversionException(THE_VALUE + value + " can't be converted to a Pattern");
        }
        try {
            return Pattern.compile((String) value);
        } catch (final PatternSyntaxException e) {
            throw new ConversionException(THE_VALUE + value + " can't be converted to a Pattern", e);
        }
    }

    /**
     * Converts the specified object into a Short.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to a short
     */
    public static Short toShort(final Object value) throws ConversionException {
        final Number n = toNumber(value, Short.class);
        if (n instanceof Short) {
            return (Short) n;
        }
        return n.shortValue();
    }

    /**
     * Converts the specified object into an URI.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to an URI
     */
    public static URI toURI(final Object value) throws ConversionException {
        if (value instanceof URI) {
            return (URI) value;
        }
        if (!(value instanceof String)) {
            throw new ConversionException(THE_VALUE + value + " can't be converted to an URI");
        }
        try {
            return new URI((String) value);
        } catch (final URISyntaxException e) {
            throw new ConversionException(THE_VALUE + value + " can't be converted to an URI", e);
        }
    }

    /**
     * Converts the specified object into an URL.
     *
     * @param value the value to convert
     * @return the converted value
     * @throws ConversionException thrown if the value cannot be converted to an URL
     */
    public static URL toURL(final Object value) throws ConversionException {
        if (value instanceof URL) {
            return (URL) value;
        }
        if (!(value instanceof String)) {
            throw new ConversionException(THE_VALUE + value + " can't be converted to an URL");
        }
        try {
            return new URL((String) value);
        } catch (final MalformedURLException e) {
            throw new ConversionException(THE_VALUE + value + " can't be converted to an URL", e);
        }
    }

    /**
     * Private constructor prevents instances from being created.
     */
    private PropertyConverter() {
        // presvents instantiation.
    }
}
