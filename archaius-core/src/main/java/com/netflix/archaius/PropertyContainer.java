/**
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.archaius;

import java.math.BigDecimal;
import java.math.BigInteger;

public interface PropertyContainer {
    
    /**
     * Notify the observable that it should fetch the latest property value
     */
    void update();
    
    /**
     * Parse the property as a string 
     */
    Property<String> asString();
    
    /**
     * Parse the property as an int 
     */
    Property<Integer> asInteger();
    
    /**
     * Parse the property as a Long 
     */
    Property<Long> asLong();
    
    /**
     * Parse the property as a double 
     */
    Property<Double> asDouble();
    
    /**
     * Parse the property as a float 
     */
    Property<Float> asFloat();
    
    /**
     * Parse the property as a short 
     */
    Property<Short> asShort();
    
    /**
     * Parse the property as a byte 
     */
    Property<Byte> asByte();
    
    /**
     * Parse the property as a boolean 
     */
    Property<Boolean> asBoolean();

    /**
     * Parse the property as a BigDecimal 
     */
    Property<BigDecimal> asBigDecimal();

    /**
     * Parse the property as a BigInteger 
     */
    Property<BigInteger> asBigInteger();
    
    /**
     * Custom parsing based on the provided type.  An implementation of ObservableProperty 
     * should be optimized to call one of the known parsing methods based on type. 
     */
    <T> Property<T> asType(Class<T> type);
}