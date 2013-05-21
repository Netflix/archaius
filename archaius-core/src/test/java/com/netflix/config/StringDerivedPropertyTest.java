package com.netflix.config;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StringDerivedPropertyTest {

    @Test
    public void testPropertyChanged() {
        final AtomicBoolean derived = new AtomicBoolean(false);

        final String defaultVal = "hi";
        StringDerivedProperty<String> p = new StringDerivedProperty<String>("com.netflix.hello", defaultVal, 
                new Function<String, String>() {
            @Override
            public String apply(String input) {
                derived.set(true);
                return String.format("%s/derived", input);
            }

        });

        assertEquals(defaultVal, p.getValue());
        
        ConfigurationManager.getConfigInstance().setProperty("com.netflix.hello", "archaius");
        
        assertTrue("derive() was not called", derived.get());
        
        assertEquals(String.format("%s/derived", "archaius"), p.getValue());
    }

    @Test
    public void testPropertyChangedWhenDeriveThrowsException() {
        final String defaultVal = "hi";
        StringDerivedProperty<String> p = new StringDerivedProperty<String>("com.netflix.test", defaultVal, 
                new Function<String, String>() {
            @Override
            public String apply(String input) {
                throw new RuntimeException("oops");
            }
        });

        ConfigurationManager.getConfigInstance().setProperty("com.netflix.test", "xyz");
        assertEquals("hi", p.getValue());
    }

}
