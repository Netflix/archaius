/*
 *
 *  Copyright 2012 Netflix, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
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

    @Override
    protected void propertyChanged() {
        super.propertyChanged();
        deriveAndSet();
    }

    private void deriveAndSet() {
         try {
            derived.set(derive(this.get()));
        } catch (Exception e) {
            log.error("error when deriving initial value", e);
        }
    }

}
