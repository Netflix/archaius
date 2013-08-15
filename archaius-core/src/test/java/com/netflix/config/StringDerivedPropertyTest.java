/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.config;

import org.junit.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

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
    
    @Test
    public void testCalendar() {
        Date now = new Date();
        StringDerivedProperty<Date> calendarProperty = new StringDerivedProperty<Date>("myproperty", now, new Function<String, Date>() {
            @Override
            public Date apply(@Nullable String input) {
                Date d = new Date();
                try {
                    d.setTime(Long.parseLong(input));
                } catch (Exception e) {
                    return new Date(0);
                }
                return d;
            }            
        });
        assertEquals(now, calendarProperty.getValue());
        
        Date newTime = new Date();
        newTime.setTime(now.getTime() + 60000);
        
        ConfigurationManager.getConfigInstance().setProperty("myproperty", String.valueOf(newTime.getTime()));
        assertEquals(newTime, calendarProperty.getValue());
    }

}
