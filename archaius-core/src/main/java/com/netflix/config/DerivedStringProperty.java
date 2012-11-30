package com.netflix.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Derives a complex value from a {@link DynamicStringProperty}.
 *
 * @author mhawthorne
 */
public abstract class DerivedStringProperty<D> extends DynamicStringProperty {

    private static final Logger log = LoggerFactory.getLogger(DerivedStringProperty.class);

    /**
     * Holds derived value, which may be null.
     */
    private final AtomicReference<D> derived = new AtomicReference<D>(null);

    public DerivedStringProperty(String name, String defaultValue) {
        super(name, defaultValue);
        deriveAndSet();
    }

    private void deriveAndSet() {
         try {
            derived.set(derive(this.get()));
        } catch (Exception e) {
            log.error("error when deriving initial value", e);
        }
    }

    @Override
    protected void propertyChanged() {
        super.propertyChanged();
//        derived.set(this.derive(this.get()));
        deriveAndSet();
    }

    /**
     * Fetches derived value.
     */
    public D getDerived() {
        return derived.get();
    }

    /**
     * Invoked when property is updated with a new value.  Should return the new derived value, which may be null.
     */
    protected abstract D derive(String value);

}
