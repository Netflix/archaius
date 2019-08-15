package com.netflix.archaius.converters;

import com.netflix.archaius.api.TypeConverter;
import com.netflix.archaius.exceptions.ParseException;

import javax.xml.bind.DatatypeConverter;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
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
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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
    };

    private Map<Type, TypeConverter<?>> converters = new HashMap<>();

    private DefaultTypeConverterFactory() {
        converters.put(String.class, create(Function.identity()));
        converters.put(boolean.class, create(DefaultTypeConverterFactory::convertBoolean));
        converters.put(Boolean.class, create(DefaultTypeConverterFactory::convertBoolean));
        converters.put(Integer.class, create(Integer::valueOf));
        converters.put(int.class, create(Integer::valueOf));
        converters.put(long.class, create(Long::valueOf));
        converters.put(Long.class, create(Long::valueOf));
        converters.put(short.class, create(Short::valueOf));
        converters.put(Short.class, create(Short::valueOf));
        converters.put(byte.class, create(Byte::valueOf));
        converters.put(Byte.class, create(Byte::valueOf));
        converters.put(double.class, create(Double::valueOf));
        converters.put(Double.class, create(Double::valueOf));
        converters.put(float.class, create(Float::valueOf));
        converters.put(Float.class, create(Float::valueOf));
        converters.put(BigInteger.class, create(BigInteger::new));
        converters.put(BigDecimal.class, create(BigDecimal::new));
        converters.put(AtomicInteger.class, create(v -> new AtomicInteger(Integer.parseInt(v))));
        converters.put(AtomicLong.class, create(v -> new AtomicLong(Long.parseLong(v))));
        converters.put(Duration.class, create(Duration::parse));
        converters.put(Period.class, create(Period::parse));
        converters.put(LocalDateTime.class, create(LocalDateTime::parse));
        converters.put(LocalDate.class, create(LocalDate::parse));
        converters.put(LocalTime.class, create(LocalTime::parse));
        converters.put(OffsetDateTime.class, create(OffsetDateTime::parse));
        converters.put(OffsetTime.class, create(OffsetTime::parse));
        converters.put(ZonedDateTime.class, create(ZonedDateTime::parse));
        converters.put(Instant.class, create(v -> Instant.from(OffsetDateTime.parse(v))));
        converters.put(Date.class, create(v -> new Date(Long.parseLong(v))));
        converters.put(Currency.class, create(Currency::getInstance));
        converters.put(BitSet.class, create(v -> BitSet.valueOf(DatatypeConverter.parseHexBinary(v))));
    }

    private static <T> TypeConverter<T> create(Function<String, T> func) {
        assert func != null;
        return s -> func.apply(s);
    }

    @Override
    public Optional<TypeConverter<?>> get(Type type, TypeConverter.Registry registry) {
        assert type != null;
        assert registry != null;
        for (Map.Entry<Type, TypeConverter<?>> entry : converters.entrySet()) {
            if (entry.getKey().equals(type)) {
                return Optional.of(entry.getValue());
            }
        }
        return Optional.empty();
    }
}
