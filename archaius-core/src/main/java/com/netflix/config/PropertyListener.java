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

/**
 * Listener that handles property event notifications. It handles events to add a property, set property,
 * remove property, load and clear of the configuration source. 
 * <p>
 * {@link DynamicPropertySupport} registers this type listener with a {@link DynamicPropertySupport} to receive 
 * callbacks on changes so that it can dynamically change a value of a DynamicProperty.
 * 
 * @see DynamicPropertySupport#addConfigurationListener(PropertyListener)
 * @author awang
 *
 */
public interface PropertyListener {
    
    
    /**
     * <p>Notifies this listener about a new source of configuration being invalidated and/or added</p>
     * 
     * 
     * @param source the event source.
     * 
     */
    public void configSourceLoaded(Object source);
    
    /**
     * <p>Notifies this listener about a new value for the given property.</p>
     * 
     * @param source the event source.
     * @param name the property name.
     * @param value the property value (current value if
     *        <code>beforeUpdate</code> is true, otherwise the new value).
     * @param beforeUpdate true if this callback is occuring before the
     *        property has changed.
     */
    public void addProperty(Object source, String name, Object value, boolean beforeUpdate);
    
    /**
     * <p>Notifies this listener about a changed value for the given
     * property.</p>
     * 
     * @param source the event source.
     * @param name the property name.
     * @param value the property value (current value if
     *        <code>beforeUpdate</code> is true, otherwise the replacement
     *        value).
     * @param beforeUpdate true if this callback is occuring before the
     *        property has changed.
     */
    public void setProperty(Object source, String name, Object value, boolean beforeUpdate);
    
    /**
     * <p>Notifies this listener about a cleared property, which now has no
     * value.</p>
     * 
     * @param source the event source.
     * @param name the property name.
     * @param value the property value (current value if
     *        <code>beforeUpdate</code> is true, otherwise the new value which
     *        should be <code>null</code>).
     * @param beforeUpdate true if this callback is occuring before the
     *        property has changed.
     */
    public void clearProperty(Object source, String name, Object value, boolean beforeUpdate);
    
    /**
     * <p>Notifies this listener that all properties have been cleared.</p>
     * 
     * @param source the event source.
     * @param beforeUpdate true if this callback is occuring before the
     *        properties have been cleared.
     */
    public void clear(Object source, boolean beforeUpdate);
}
