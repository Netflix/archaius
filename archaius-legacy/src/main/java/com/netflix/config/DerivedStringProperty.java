/**
 * Copyright 2014 Netflix, Inc.
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

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Derives a complex value from a {@link DynamicStringProperty}.
 *
 * @author mhawthorne
 */
public abstract class DerivedStringProperty<D> implements Property<D> {

    private static final Logger log = LoggerFactory.getLogger(DerivedStringProperty.class);

    private final DynamicStringProperty delegate;

    private final List<Runnable> callbackList = Lists.newArrayList();

    /**
     * Holds derived value, which may be null.
     */
    private final AtomicReference<D> derived = new AtomicReference<D>(null);

    public DerivedStringProperty(String name, String defaultValue) {
        delegate = DynamicPropertyFactory.getInstance().getStringProperty(name, defaultValue);
        deriveAndSet();
        Runnable callback = new Runnable() {
            public void run() {
                propertyChangedInternal();
            }
        };
        delegate.addCallback(callback);
        callbackList.add(callback);
    }

    /**
     * Fetches derived value.
     */
    public D get() {
        return derived.get();
    }
    
    @Override
    public D getValue() {
        return get();
    }

    @Override
    public D getDefaultValue() {
        return derive(delegate.getDefaultValue());
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public long getChangedTimestamp() {
        return delegate.getChangedTimestamp();
    }

    @Override
    public void addCallback(Runnable callback) {
        delegate.addCallback(callback);
        callbackList.add(callback);
    }

    /**
     * Remove all callbacks registered through this instance of property
     */
    @Override
    public void removeAllCallbacks() {
        for (Runnable callback: callbackList) {
            delegate.prop.removeCallback(callback);
        }
    }

    /**
     * Invoked when property is updated with a new value.  Should return the new derived value, which may be null.
     */
    protected abstract D derive(String value);

    /**
     * {@link com.netflix.config.PropertyWrapper#propertyChanged()}
     */
    protected void propertyChanged() {}

    void propertyChangedInternal() {
        deriveAndSet();
        propertyChanged();
    }

    private void deriveAndSet() {
         try {
            derived.set(derive(this.delegate.get()));
        } catch (Exception e) {
            log.error("error when deriving initial value", e);
        }
    }
}
