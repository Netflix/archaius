package com.netflix.config;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.netflix.config.ChainedDynamicProperty.ChainLink;

/**
 * Archaius 1 implementation of PropertyRepo with com.netflix.config.**
 * 
 */
public class Archaius1PropertyRepo implements PropertyRepo {
    private interface Prop<T> extends Supplier<T> {
        Supplier<T> onChange(Runnable r);
    }

    static class DynamicPropertyAdapter<T> implements Prop<T> {
        private final Property<T> property;

        DynamicPropertyAdapter(Property<T> property) {
            this.property = property;
        }

        @Override
        public T get() {
            return property.getValue();
        }

        @Override
        public Supplier<T> onChange(Runnable r) {
            property.addCallback(r);
            return this;
        }
    }

    static class SupplierAdapter<T> extends PropertyWrapper<T> implements Prop<T> {
        private final Function<DynamicProperty, T> overrideValueFunction;
        private final Supplier<T> primary;
        private final T defaultValue;

        SupplierAdapter(String overrideKey, Function<DynamicProperty, T> valueFunction, Supplier<T> primary,
                T defaultValue) {
            super(overrideKey, null);
            this.overrideValueFunction = valueFunction;
            this.primary = primary;
            this.defaultValue = defaultValue;
        }

        @Override
        public T getValue() {
            return get();
        }

        @Override
        public T get() {
            return Optional.ofNullable(overrideValueFunction.apply(super.getDynamicProperty()))
                    .orElseGet(() -> Optional.ofNullable(primary.get()).orElse(defaultValue));
        }

        @Override
        public Supplier<T> onChange(Runnable r) {
            addCallback(r);
            if (primary instanceof Prop) {
                ((Prop<?>) primary).onChange(r);
            }
            return this;
        }
    }

    static class ChainedPropertyAdapter<T> implements Prop<T> {
        private final ChainLink<T> chainLink;
        private final Property<T> property;

        ChainedPropertyAdapter(ChainLink<T> chainLink, Property<T> property) {
            this.chainLink = chainLink;
            this.property = property;
        }

        @Override
        public T get() {
            return chainLink.get();
        }

        @Override
        public Supplier<T> onChange(Runnable r) {
            chainLink.addCallback(r);
            property.addCallback(r);
            return this;
        }
    }

    // archaius 1 fast property factory
    private final DynamicPropertyFactory propertyFactory;

    public Archaius1PropertyRepo() {
        this(DynamicPropertyFactory.getInstance());
    }

    public Archaius1PropertyRepo(DynamicPropertyFactory propertyFactory) {
        this.propertyFactory = propertyFactory;
    }

    private <T> Prop<T> getProperty(String propertyKey, Function<String, Property<T>> propertyFactory) {
        return new DynamicPropertyAdapter<T>(propertyFactory.apply(propertyKey));
    }

    private <T> Prop<T> getChainedProperty(String propertyKey, Function<String, Prop<T>> propertyFactory) {
        return propertyFactory.apply(propertyKey);
    }

    @Override
    public Supplier<Boolean> getProperty(String overrideKey, String primaryKey, Boolean defaultValue) {
        Function<String, Prop<Boolean>> propFactory = k -> {
            return new SupplierAdapter<>(overrideKey, DynamicProperty::getBoolean, getProperty(primaryKey, defaultValue), defaultValue);
        };
        return getChainedProperty(overrideKey + primaryKey, propFactory);
    }

    @Override
    public Supplier<String> getProperty(String overrideKey, String primaryKey, String defaultValue) {
        Function<String, Prop<String>> propFactory = k -> {
            return new SupplierAdapter<>(overrideKey, DynamicProperty::getString, getProperty(primaryKey, defaultValue), defaultValue);
        };
        return getChainedProperty(overrideKey + primaryKey, propFactory);
    }

    @Override
    public Supplier<Long> getProperty(String overrideKey, String primaryKey, Long defaultValue) {
        Function<String, Prop<Long>> propFactory = k -> {
            return new SupplierAdapter<>(overrideKey, DynamicProperty::getLong, getProperty(primaryKey, defaultValue), defaultValue);
        };
        return getChainedProperty(overrideKey + primaryKey, propFactory);
    }

    @Override
    public Supplier<Integer> getProperty(String overrideKey, String primaryKey, Integer defaultValue) {
        Function<String, Prop<Integer>> propFactory = k -> {
            return new SupplierAdapter<>(overrideKey, DynamicProperty::getInteger, getProperty(primaryKey, defaultValue), defaultValue);
        };
        return getChainedProperty(overrideKey + primaryKey, propFactory);
    }

    @Override
    public Supplier<Set<String>> getProperty(String overrideKey, String primaryKey, Set<String> defaultValue) {
        Function<String, Prop<Set<String>>> propFactory = k -> {
            DynamicStringSetProperty baseProp = new DynamicStringSetProperty(primaryKey, defaultValue);
            DynamicStringSetProperty overrideProperty = new DynamicStringSetProperty(overrideKey, (Set<String>) null);
            return new Prop<Set<String>>() {

                @Override
                public Set<String> get() {
                    return Optional.ofNullable(overrideProperty.get()).orElseGet(baseProp::get);
                }

                @Override
                public Supplier<Set<String>> onChange(Runnable r) {
                    overrideProperty.addCallback(r);
                    baseProp.addCallback(r);
                    return this;
                }

            };
        };
        return getChainedProperty(overrideKey + primaryKey, propFactory);
    }

    @Override
    public Supplier<Boolean> getProperty(String overrideKey, Supplier<Boolean> primaryProperty, Boolean defaultValue) {
        Function<String, Prop<Boolean>> propFactory = k -> {
            return new SupplierAdapter<>(overrideKey, DynamicProperty::getBoolean, primaryProperty, defaultValue);
        };
        return getChainedProperty(overrideKey + System.identityHashCode(primaryProperty), propFactory);
    }

    @Override
    public Supplier<Integer> getProperty(String overrideKey, Supplier<Integer> primaryProperty, Integer defaultValue) {
        Function<String, Prop<Integer>> propFactory = k -> {
            return new SupplierAdapter<>(overrideKey, DynamicProperty::getInteger, primaryProperty, defaultValue);
        };
        return getChainedProperty(overrideKey + System.identityHashCode(primaryProperty), propFactory);
    }

    @Override
    public Supplier<Long> getProperty(String overrideKey, Supplier<Long> primaryProperty, Long defaultValue) {
        Function<String, Prop<Long>> propFactory = k -> {
            return new SupplierAdapter<>(overrideKey, DynamicProperty::getLong, primaryProperty, defaultValue);
        };
        return getChainedProperty(overrideKey + System.identityHashCode(primaryProperty), propFactory);
    }

    @Override
    public Supplier<String> getProperty(String overrideKey, Supplier<String> primaryProperty, String defaultValue) {
        Function<String, Prop<String>> propFactory = k -> {
            return new SupplierAdapter<>(overrideKey, DynamicProperty::getString, primaryProperty, defaultValue);
        };
        return getChainedProperty(overrideKey + System.identityHashCode(primaryProperty), propFactory);
    }

    @Override
    public Supplier<Set<String>> getProperty(String overrideKey, Supplier<Set<String>> primaryProperty,
            Set<String> defaultValue) {
        Function<String, Prop<Set<String>>> propFactory = k -> {
            DynamicStringSetProperty overrideProperty = new DynamicStringSetProperty(overrideKey, (Set<String>) null);
            return new Prop<Set<String>>() {

                @Override
                public Set<String> get() {
                    return Optional.ofNullable(overrideProperty.get())
                            .orElseGet(() -> Optional.ofNullable(primaryProperty.get()).orElse(defaultValue));
                }

                @Override
                public Supplier<Set<String>> onChange(Runnable r) {
                    overrideProperty.addCallback(r);
                    if (primaryProperty instanceof Prop) {
                        ((Prop<?>) primaryProperty).onChange(r);
                    }
                    return this;
                }

            };
        };
        return getChainedProperty(overrideKey + System.identityHashCode(primaryProperty), propFactory);
    }

    @Override
    public Supplier<Boolean> getProperty(String propertyKey, Boolean defaultValue) {
        return getProperty(propertyKey, key -> new FunctionalPropertyWrapper<>(propertyKey, DynamicProperty::getBoolean, defaultValue));

    }

    @Override
    public Supplier<Integer> getProperty(String propertyKey, Integer defaultValue) {
        return getProperty(propertyKey, key -> new FunctionalPropertyWrapper<>(propertyKey, DynamicProperty::getInteger, defaultValue));
    }

    @Override
    public Supplier<Long> getProperty(String propertyKey, Long defaultValue) {
        return getProperty(propertyKey, key -> new FunctionalPropertyWrapper<>(propertyKey, DynamicProperty::getLong, defaultValue));
    }

    @Override
    public Supplier<String> getProperty(String propertyKey, String defaultValue) {
        return getProperty(propertyKey, key -> propertyFactory.getStringProperty(key, defaultValue));
    }

    @Override
    public Supplier<Set<String>> getProperty(String propertyKey, Set<String> defaultValue) {
        return getProperty(propertyKey, key -> new DynamicStringSetProperty(key, defaultValue));
    }

    @Override
    public <T> Supplier<T> onChange(Supplier<T> property, Runnable callback) {
        if (property instanceof Prop) {
            Prop<T> prop = (Prop<T>) property;
            prop.onChange(callback);
        }
        return null;
    }
    
    static class FunctionalPropertyWrapper<T> extends PropertyWrapper<T> {
        Function<DynamicProperty, T> valueFunction;
        FunctionalPropertyWrapper(String key, Function<DynamicProperty, T> valueFunction, T defaultValue) {
            super(key, defaultValue);
            this.valueFunction = valueFunction;
        }
        @Override
        public T getValue() {
            return Optional.ofNullable(valueFunction.apply(getDynamicProperty())).orElse(getDefaultValue());
        }
        
    }

}