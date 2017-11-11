package com.netflix.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.api.PropertyListener;

/**
 * PropertyRepo implementation for Archaius2
 *
 */
public class Archaius2PropertyRepo implements PropertyRepo {
    private interface Prop<T> extends Supplier<T> {
        Supplier<T> onChange(RunnablePropertyListener<T> r);
    }
    
    private final PropertyFactory propertyFactory;

    public Archaius2PropertyRepo(PropertyFactory propertyFactory) {
        this.propertyFactory = propertyFactory;
    }
    
    @VisibleForTesting
    PropertyFactory getPropertyFactory() {
        return propertyFactory;
    }

    @Override
    public Supplier<Boolean> getProperty(String propertyKey, Boolean defaultValue) {
        return new Archaius2Prop<Boolean>(propertyFactory.getProperty(propertyKey).asBoolean(defaultValue));
    }

    @Override
    public Supplier<Integer> getProperty(String propertyKey, Integer defaultValue) {
        return new Archaius2Prop<Integer>(propertyFactory.getProperty(propertyKey).asInteger(defaultValue));
    }

    @Override
    public Supplier<Long> getProperty(String propertyKey, Long defaultValue) {
        return new Archaius2Prop<Long>(propertyFactory.getProperty(propertyKey).asLong(defaultValue));
    }

    @Override
    public Supplier<String> getProperty(String propertyKey, String defaultValue) {
        return new Archaius2Prop<String>(propertyFactory.getProperty(propertyKey).asString(defaultValue));
    }

    @Override
    public Supplier<Set<String>> getProperty(String propertyKey, Set<String> defaultValue) {
        return new Archaius2Prop<Set<String>>(propertyFactory.getProperty(propertyKey).asType(setParser, null), defaultValue);
    }

    @Override
    public Supplier<Boolean> getProperty(String overrideKey, String primaryKey, Boolean defaultValue) {
        return new Archaius2ChainedProp<Boolean>(propertyFactory.getProperty(overrideKey).asBoolean(null),
                propertyFactory.getProperty(primaryKey).asBoolean(defaultValue));
    }

    @Override
    public Supplier<String> getProperty(String overrideKey, String primaryKey, String defaultValue) {
        return new Archaius2ChainedProp<String>(propertyFactory.getProperty(overrideKey).asString(null),
                propertyFactory.getProperty(primaryKey).asString(defaultValue));
    }

    @Override
    public Supplier<Long> getProperty(String overrideKey, String primaryKey, Long defaultValue) {
        return new Archaius2ChainedProp<Long>(propertyFactory.getProperty(overrideKey).asLong(null),
                propertyFactory.getProperty(primaryKey).asLong(defaultValue));
    }

    @Override
    public Supplier<Integer> getProperty(String overrideKey, String primaryKey, Integer defaultValue) {
        return new Archaius2ChainedProp<Integer>(propertyFactory.getProperty(overrideKey).asInteger(null),
                propertyFactory.getProperty(primaryKey).asInteger(defaultValue));
    }

    @Override
    public Supplier<Integer> getProperty(String overrideKey, Supplier<Integer> primaryProperty, Integer defaultValue) {
        return new Archaius2SupplierProp<Integer>(propertyFactory.getProperty(overrideKey).asInteger(null),
                primaryProperty, defaultValue);
    }
    
    @Override
    public <T> Supplier<T> onChange(Supplier<T> property, Runnable callback) {
        if (property instanceof Prop) {
            ((Prop<T>)property).onChange(new RunnablePropertyListener<T>(callback));
        }
        return null;
    }

    @Override
    public Supplier<Set<String>> getProperty(String overrideKey, String propertyKey, Set<String> defaultValue) {
        return new Archaius2ChainedProp<>(propertyFactory.getProperty(overrideKey).asType(setParser, null), propertyFactory.getProperty(propertyKey).asType(setParser, null), defaultValue);
    }

    @Override
    public Supplier<Boolean> getProperty(String overrideKey, Supplier<Boolean> primaryProperty, Boolean defaultValue) {
        return new Archaius2SupplierProp<>(propertyFactory.getProperty(overrideKey).asBoolean(null),
                primaryProperty, defaultValue);
    }

    @Override
    public Supplier<Long> getProperty(String overrideKey, Supplier<Long> primaryProperty, Long defaultValue) {
        return new Archaius2SupplierProp<>(propertyFactory.getProperty(overrideKey).asLong(null),
                primaryProperty, defaultValue);
    }

    @Override
    public Supplier<String> getProperty(String overrideKey, Supplier<String> primaryProperty, String defaultValue) {
        return new Archaius2SupplierProp<>(propertyFactory.getProperty(overrideKey).asString(null),
                primaryProperty, defaultValue);
    }

    @Override
    public Supplier<Set<String>> getProperty(String overrideKey, Supplier<Set<String>> primaryProperty,
            Set<String> defaultValue) {
        return new Archaius2SupplierProp<>(propertyFactory.getProperty(overrideKey).asType(setParser, null), primaryProperty, defaultValue);
    }
    

    private final Function<String, Set<String>> setParser = value -> {
        final Set<String> rv;
        if (value == null) {
            return null;
        }
        else if (!value.isEmpty()) {
            Predicate<String> emptyStringFilter = String::isEmpty;
            rv =  Arrays.stream(value.split("\\s*,\\s*")).filter(emptyStringFilter.negate()).collect(Collectors.toSet());
        } else {
            rv = Collections.emptySet();
        }
        return rv;
    };

    private static class RunnablePropertyListener<T> implements PropertyListener<T> {
        private final Runnable callback;
        private T lastValue = null;

        RunnablePropertyListener(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public void onChange(T value) {
            if (this.lastValue == value || (this.lastValue != null && this.lastValue.equals(value))) {
                // elide callback
                Logger.getLogger(getClass()).info(String.format("oldValue {} / newValue {}", lastValue, value));
            }
            else {
                callback.run();
            }
            this.lastValue = value;
        }

        @Override
        public void onParseError(Throwable error) {
        }

    }

    private class Archaius2Prop<T> implements Prop<T> {
        private final Property<T> archaiusProperty;
        private final T defaultValue;

        Archaius2Prop(Property<T> archaiusProperty) {
            this(archaiusProperty, null);
        }

        Archaius2Prop(Property<T> archaiusProperty, T defaultValue) {
            this.archaiusProperty = archaiusProperty;
            this.defaultValue = defaultValue;
        }

        @Override
        public T get() {
            return Optional.ofNullable(archaiusProperty.get()).orElse(defaultValue);
        }

        @Override
        public Supplier<T> onChange(RunnablePropertyListener<T> callback) {
            archaiusProperty.addListener(callback);
            return this;
        }

        @Override
        public String toString() {
            return String.format("Archaius2Prop - value: %s", archaiusProperty.get());
        }
    }

    private class Archaius2ChainedProp<T> implements Prop<T> {
        private final Property<T> overrideProperty;
        private final Property<T> primaryProperty;
        private final T defaultValue;

        Archaius2ChainedProp(Property<T> overrideProperty, Property<T> primaryProperty) {
            this(overrideProperty, primaryProperty, null);
        }
        
        Archaius2ChainedProp(Property<T> overrideProperty, Property<T> primaryProperty, T defaultValue) {
            this.overrideProperty = overrideProperty;
            this.primaryProperty = primaryProperty;
            this.defaultValue = defaultValue;
        }

        @Override
        public T get() {
            return Optional.ofNullable(Optional.ofNullable(overrideProperty.get()).orElseGet(primaryProperty::get)).orElse(defaultValue);
        }

        @Override
        public Supplier<T> onChange(RunnablePropertyListener<T> listener) {
            overrideProperty.addListener(listener);
            primaryProperty.addListener(new RunnablePropertyListener<T>(listener.callback));
            return this;
        }
        
        @Override
        public String toString() {
            return String.format("Archaius2ChainedProp - override: %s, primary: %s", overrideProperty.get(), primaryProperty.get());
        }

    }

    private class Archaius2SupplierProp<T> implements Prop<T> {
        private final Property<T> overrideProperty;
        private final Supplier<T> primarySupplier;
        private final T defaultValue;

        Archaius2SupplierProp(Property<T> overrideProperty, Supplier<T> primarySupplier, T defaultValue) {
            this.overrideProperty = overrideProperty;
            this.primarySupplier = primarySupplier;
            this.defaultValue = defaultValue;
        }

        @Override
        public T get() {
            return Optional.ofNullable(overrideProperty.get()).orElseGet(()->Optional.ofNullable(primarySupplier.get()).orElse(defaultValue));
        }

        @Override
        public Supplier<T> onChange(RunnablePropertyListener<T> listener) {
            overrideProperty.addListener(listener);
            if (primarySupplier instanceof Prop) {
                ((Prop<T>) primarySupplier).onChange(new RunnablePropertyListener<T>(listener.callback));
            }
            return this;
        }

        @Override
        public String toString() {
            return String.format("Archaius2SupplierProp - override: %s, primary: %s", overrideProperty.get(), primarySupplier.get());
        }
    }

}
