package com.netflix.archaius.converters;

import com.netflix.archaius.api.TypeConverter;
import com.netflix.archaius.exceptions.ParseException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.BitSet;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public final class DefaultTypeConverterFactory implements TypeConverter.Factory {
    public static final DefaultTypeConverterFactory INSTANCE = new DefaultTypeConverterFactory();

    private static Boolean convertBoolean(String value) {
        value = value.trim();
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("on")) {
            return Boolean.TRUE;
        }
        else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("off")) {
            return Boolean.FALSE;
        }
        throw new ParseException("Error parsing value '" + value + "'. Expected one of [true, yes, on, false, no, off]");
    }

    private final Map<Type, TypeConverter<?>> converters;

    private DefaultTypeConverterFactory() {
        Map<Type, TypeConverter<?>> converters = new HashMap<>();
        converters.put(String.class, Function.identity()::apply);
        converters.put(boolean.class, DefaultTypeConverterFactory::convertBoolean);
        converters.put(Boolean.class, DefaultTypeConverterFactory::convertBoolean);
        converters.put(Integer.class, Lenient::parseInt);
        converters.put(int.class, Lenient::parseInt);
        converters.put(long.class, Lenient::parseLong);
        converters.put(Long.class, Lenient::parseLong);
        converters.put(short.class, Lenient::parseShort);
        converters.put(Short.class, Lenient::parseShort);
        converters.put(byte.class, Lenient::parseByte);
        converters.put(Byte.class, Lenient::parseByte);
        converters.put(double.class, Lenient::parseDouble);
        converters.put(Double.class, Lenient::parseDouble);
        converters.put(float.class, Lenient::parseFloat);
        converters.put(Float.class, Lenient::parseFloat);
        converters.put(BigInteger.class, BigInteger::new);
        converters.put(BigDecimal.class, BigDecimal::new);
        converters.put(AtomicInteger.class, v -> new AtomicInteger(Lenient.parseInt(v)));
        converters.put(AtomicLong.class, v -> new AtomicLong(Lenient.parseLong(v)));
        converters.put(Duration.class, Duration::parse);
        converters.put(Period.class, Period::parse);
        converters.put(LocalDateTime.class, LocalDateTime::parse);
        converters.put(LocalDate.class, LocalDate::parse);
        converters.put(LocalTime.class, LocalTime::parse);
        converters.put(OffsetDateTime.class, OffsetDateTime::parse);
        converters.put(OffsetTime.class, OffsetTime::parse);
        converters.put(ZonedDateTime.class, ZonedDateTime::parse);
        converters.put(Instant.class, v -> Instant.from(OffsetDateTime.parse(v)));
        converters.put(Date.class, v -> new Date(Lenient.parseLong(v)));
        converters.put(Currency.class, Currency::getInstance);
        converters.put(URI.class, URI::create);
        converters.put(Locale.class, Locale::forLanguageTag);

        converters.put(BitSet.class, v -> {
            try {
                return BitSet.valueOf(Hex.decodeHex(v));
            } catch (DecoderException e) {
                throw new RuntimeException(e);
            }
        });

        this.converters = Collections.unmodifiableMap(converters);
    }

    @Override
    public Optional<TypeConverter<?>> get(Type type, TypeConverter.Registry registry) {
        Objects.requireNonNull(type, "type == null");
        Objects.requireNonNull(registry, "registry == null");
        for (Map.Entry<Type, TypeConverter<?>> entry : converters.entrySet()) {
            if (entry.getKey().equals(type)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }

    /** A collection of lenient number parsers that allow whitespace and trailing 'L' or 'l' in long values */
    private static final class Lenient {
        private static String maybeTrim(String s) {
            // The way these are called, we'll never get a null. In any case, we pass it through, to ensure that
            // the exception thrown remains the same as whatever the JDK's parse***() methods throw.
            return s != null ? s.trim() : null;
        }

        private static long parseLong(String s) throws NumberFormatException {
            s = maybeTrim(s);
            // Also allow trailing 'L' or 'l' in long values
            if (s != null) {
                if (s.endsWith("L") || s.endsWith("l")) {
                    s = s.substring(0, s.length() - 1);
                }
            }

            return Long.parseLong(s);
        }

        private static int parseInt(String s) throws NumberFormatException {
            return Integer.parseInt(maybeTrim(s));
        }

        private static short parseShort(String s) throws NumberFormatException {
            return Short.parseShort(maybeTrim(s));
        }

        private static byte parseByte(String s) throws NumberFormatException {
            return Byte.parseByte(maybeTrim(s));
        }

        private static double parseDouble(String s) throws NumberFormatException {
            return Double.parseDouble(maybeTrim(s));
        }

        private static float parseFloat(String s) throws NumberFormatException {
            return Float.parseFloat(maybeTrim(s));
        }
    }
}
