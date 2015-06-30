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

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;

import javax.inject.Singleton;

/**
 * @author Spencer Gibb
 */
@Singleton
public class DefaultDecoder implements Decoder {

    public static DefaultDecoder INSTANCE = new DefaultDecoder();
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T decode(Class<T> type, String encoded) {
        // Try primitives first
        if (type.equals(String.class)) {
            return (T) encoded;
        }
        else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return (T) Boolean.valueOf(encoded);
        }
        else if (type.equals(int.class) || type.equals(Integer.class)) {
            return (T) Integer.valueOf(encoded);
        }
        else if (type.equals(long.class) || type.equals(Long.class)) {
            return (T) Long.valueOf(encoded);
        }
        else if (type.equals(short.class) || type.equals(Short.class)) {
            return (T) Short.valueOf(encoded);
        }
        else if (type.equals(double.class) || type.equals(Double.class)) {
            return (T) Double.valueOf(encoded);
        }
        else if (type.equals(float.class) || type.equals(Float.class)) {
            return (T) Float.valueOf(encoded);
        }
        else if (type.equals(BigInteger.class)) {
            return (T) new BigInteger(encoded);
        }
        else if (type.equals(BigDecimal.class)) {
            return (T) new BigDecimal(encoded);
        }
        else if (type.isArray()) {
            String[] elements = encoded.split(",");
            T[] ar = (T[]) Array.newInstance(type.getComponentType(), elements.length);
            for (int i = 0; i < elements.length; i++) {
                ar[i] = (T) decode(type.getComponentType(), elements[i]);
            }
            return (T) ar;
        }

        // Next look a valueOf(String) static method
        try {
            Method method;
            try {
                method = type.getMethod("valueOf", String.class);
                return (T) method.invoke(null, encoded);
            } catch (NoSuchMethodException e1) {
                // Next look for a T(String) constructor
                Constructor<T> c;
                try {
                    c = type.getConstructor(String.class);
                    return c.newInstance(encoded);
                }
                catch (NoSuchMethodException e) {
                    throw new RuntimeException(type.getCanonicalName() + " has no String constructor or valueOf static method");
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to instantiate value of type " + type.getCanonicalName(), e);
        }
    }
}
