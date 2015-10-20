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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.text.StrSubstitutor;

import com.netflix.archaius.annotations.Configuration;
import com.netflix.archaius.exceptions.MappingException;
import com.netflix.archaius.interpolate.ConfigStrLookup;

public class ConfigMapper {
    private static final IoCContainer NULL_IOC_CONTAINER = new IoCContainer() {
        @Override
        public <T> T getInstance(String name, Class<T> type) {
            return null;
        }
    };
    
    public ConfigMapper() {
        this(true);
    }
    
    @Deprecated
    public ConfigMapper(boolean allowPostConfigure) {
    }
    
    /**
     * Map the configuration from the provided config object onto the injectee and use
     * the provided IoCContainer to inject named bindings.
     * 
     * @param injectee
     * @param config
     * @param ioc
     * @throws MappingException
     */
    public <T> void mapConfig(T injectee, Config config) throws MappingException {
        mapConfig(injectee, config, NULL_IOC_CONTAINER);
    }

    /**
     * Map the configuration from the provided config object onto the injectee.
     * 
     * @param injectee
     * @param config
     * @param ioc
     * @throws MappingException
     */
    public <T> void mapConfig(T injectee, final Config config, IoCContainer ioc) throws MappingException {
        Configuration configAnnot = injectee.getClass().getAnnotation(Configuration.class);
        if (configAnnot == null) {
            return;
        }
        
        Class<T> injecteeType = (Class<T>) injectee.getClass();
        
        String prefix = configAnnot.prefix();
        
        // Extract parameters from the object.  For each parameter
        // look for either file 'paramname' or method 'getParamnam'
        String[] params = configAnnot.params();
        if (params.length > 0) {
            Map<String, String> map = new HashMap<String, String>();
            for (String param : params) {
                try {
                    Field f = injecteeType.getDeclaredField(param);
                    f.setAccessible(true);
                    map.put(param, f.get(injectee).toString());
                } catch (NoSuchFieldException e) {
                    try {
                        Method method = injecteeType.getDeclaredMethod(
                                "get" + Character.toUpperCase(param.charAt(0)) + param.substring(1));
                        method.setAccessible(true);
                        map.put(param, method.invoke(injectee).toString());
                    } catch (Exception e1) {
                        throw new MappingException(e1);
                    }
                } catch (Exception e) {
                    throw new MappingException(e);
                }
            }
            
            prefix = StrSubstitutor.replace(prefix, map, "${", "}");
        }
        
        // Interpolate using any replacements loaded into the configuration
        prefix = config.getStrInterpolator().create(ConfigStrLookup.from(config)).resolve(prefix);
        if (!prefix.isEmpty() && !prefix.endsWith("."))
            prefix += ".";
        
        // Iterate and set fields
        if (configAnnot.allowFields()) {
            for (Field field : injecteeType.getDeclaredFields()) {
                if (   Modifier.isFinal(field.getModifiers())
                    || Modifier.isTransient(field.getModifiers())
                    || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                
                String name = field.getName();
                Class<?> type = field.getType();
                Object value = null;
                if (type.isInterface()) {
                    // TODO: Do Class.newInstance() if objName is a classname
                    String objName = config.getString(prefix + name, null);
                    if (objName != null) {
                        value = ioc.getInstance(objName, type);
                    }
                }
                else {
                    value = config.get(type, prefix + name, null);
                }
                
                if (value != null) {
                    try {
                        field.setAccessible(true);
                        field.set(injectee, value);
                    } catch (Exception e) {
                        throw new MappingException("Unable to inject field " + injectee.getClass() + "." + name + " with value " + value, e);
                    }
                }
            }
        }
        
        // map to setter methods
        if (configAnnot.allowSetters()) {
            for (Method method : injectee.getClass().getDeclaredMethods()) {
                // Only support methods with one parameter 
                //  Ex.  setTimeout(int timeout);
                if (method.getParameterTypes().length != 1) {
                    continue;
                }
                
                // Extract field name from method name
                //  Ex.  setTimeout => timeout
                String name = method.getName();
                if (name.startsWith("set") && name.length() > 3) {
                    name = name.substring(3,4).toLowerCase() + name.substring(4);
                }
                // Or from builder
                //  Ex.  withTimeout => timeout
                else if (name.startsWith("with") && name.length() > 4) {
                    name = name.substring(4,1).toLowerCase() + name.substring(5);
                }
                else {
                    continue;
                }
    
                method.setAccessible(true);
                Class<?> type = method.getParameterTypes()[0];
                Object value = null;
                if (type.isInterface()) {
                    String objName = config.getString(prefix + name, null);
                    if (objName != null) {
                        value = ioc.getInstance(objName, type);
                    }
                }
                else {
                    value = config.get(type, prefix + name, null);
                }
                
                if (value != null) {
                    try {
                        method.invoke(injectee, value);
                    } catch (Exception e) {
                        throw new MappingException("Unable to inject field " + injectee.getClass() + "." + name + " with value " + value, e);
                    }
                }
            }
        }
        
        if (!configAnnot.postConfigure().isEmpty()) {
            try {
                Method m = injecteeType.getMethod(configAnnot.postConfigure());
                m.invoke(injectee);
            } catch (Exception e) {
                throw new MappingException("Unable to invoke postConfigure method " + configAnnot.postConfigure(), e);
            }
        }
    }
}
