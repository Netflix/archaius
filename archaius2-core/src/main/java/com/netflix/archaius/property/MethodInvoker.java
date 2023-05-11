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
package com.netflix.archaius.property;

import java.lang.reflect.Method;

public class MethodInvoker<T> extends DefaultPropertyListener<T> {
    
    private final Method method;
    private final Object obj;
    
    public MethodInvoker(Object obj, String methodName) {
        this.method = getMethodWithOneParameter(obj);
        this.obj    = obj;
    }
    
    private static Method getMethodWithOneParameter(Object obj) {
        Method[] methods = obj.getClass().getMethods();
        if (methods != null && methods.length > 0) {
            for (Method method : methods) {
                if (method.getParameterTypes().length == 1) {
                    return method;
                }
            }
            throw new IllegalArgumentException("Method with one argument does not exist");
        }
        throw new IllegalArgumentException("Method does not exit");
    }
    
    @Override
    public void onChange(T newValue) {
        try {
            method.invoke(obj, newValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
