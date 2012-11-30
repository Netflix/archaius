package com.netflix.config;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DerivedStringPropertyTest {

    @Test
    public void testPropertyChanged() {
        final AtomicBoolean derived = new AtomicBoolean(false);

        final String defaultVal = "hi";
        DerivedStringProperty p = new DerivedStringProperty("com.netflix.hello", defaultVal) {
            @Override
            protected Object derive(String value) {
                derived.set(true);
                return String.format("%s/derived", value);
            }
        };

        p.propertyChanged();
        assertTrue("derive() was not called", derived.get());
        assertEquals(String.format("%s/derived", defaultVal), p.getDerived());
    }

    @Test
    public void testPropertyChangedWhenDeriveThrowsException() {
        final String defaultVal = "hi";
        DerivedStringProperty p = new DerivedStringProperty("com.netflix.hello", defaultVal) {
            @Override
            protected Object derive(String value) {
                throw new RuntimeException("oops");
            }
        };

        p.propertyChanged();
        assertEquals(null, p.getDerived());
    }

}
