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
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("on")) {
            return Boolean.TRUE;
        }
        else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("off")) {
            return Boolean.FALSE;
        }
        throw new ParseException("Error parsing value '" + value + "'", new Exception("Expected one of [true, yes, on, false, no, off]"));
    }

    private final Map<Type, TypeConverter<?>> converters;

    private DefaultTypeConverterFactory() {
        Map<Type, TypeConverter<?>> converters = new HashMap<>();
        converters.put(String.class, Function.identity()::apply);
        converters.put(boolean.class, DefaultTypeConverterFactory::convertBoolean);
        converters.put(Boolean.class, DefaultTypeConverterFactory::convertBoolean);
        converters.put(Integer.class, Integer::valueOf);
        converters.put(int.class, Integer::valueOf);
        converters.put(long.class, Long::valueOf);
        converters.put(Long.class, Long::valueOf);
        converters.put(short.class, Short::valueOf);
        converters.put(Short.class, Short::valueOf);
        converters.put(byte.class, Byte::valueOf);
        converters.put(Byte.class, Byte::valueOf);
        converters.put(double.class, Double::valueOf);
        converters.put(Double.class, Double::valueOf);
        converters.put(float.class, Float::valueOf);
        converters.put(Float.class, Float::valueOf);
        converters.put(BigInteger.class, BigInteger::new);
        converters.put(BigDecimal.class, BigDecimal::new);
        converters.put(AtomicInteger.class, v -> new AtomicInteger(Integer.parseInt(v)));
        converters.put(AtomicLong.class, v -> new AtomicLong(Long.parseLong(v)));
        converters.put(Duration.class, Duration::parse);
        converters.put(Period.class, Period::parse);
        converters.put(LocalDateTime.class, LocalDateTime::parse);
        converters.put(LocalDate.class, LocalDate::parse);
        converters.put(LocalTime.class, LocalTime::parse);
        converters.put(OffsetDateTime.class, OffsetDateTime::parse);
        converters.put(OffsetTime.class, OffsetTime::parse);
        converters.put(ZonedDateTime.class, ZonedDateTime::parse);
        converters.put(Instant.class, v -> Instant.from(OffsetDateTime.parse(v)));
        converters.put(Date.class, v -> new Date(Long.parseLong(v)));
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
}
