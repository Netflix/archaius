/**
 * Copyright 2026 Netflix, Inc.
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
package com.netflix.archaius.property;

import java.util.concurrent.atomic.AtomicInteger;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyListener;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("deprecation")
public class DefaultPropertyContainerTest {
    @Test
    public void listenerIsNotInvokedWhenNewValueEqualsPreviousValue() {
        // Every call returns a new, equal String instance.
        Config config = Mockito.mock(Config.class);
        Mockito.when(config.getString("foo", null)).thenAnswer(inv -> new String("bar"));

        AtomicInteger version = new AtomicInteger();
        ListenerManager listeners = new ListenerManager();
        DefaultPropertyContainer container = new DefaultPropertyContainer("foo", config, version, listeners);

        AtomicInteger changes = new AtomicInteger();
        Property<String> prop = container.asString(null);
        prop.addListener(new PropertyListener<String>() {
            @Override
            public void onChange(String value) {
                changes.incrementAndGet();
            }

            @Override
            public void onParseError(Throwable error) {
            }
        });

        version.incrementAndGet();
        listeners.updateAll();
        assertEquals(1, changes.get());

        // The value is resolved again as an equal but distinct instance: not a change.
        version.incrementAndGet();
        listeners.updateAll();
        assertEquals(1, changes.get());
    }
}
