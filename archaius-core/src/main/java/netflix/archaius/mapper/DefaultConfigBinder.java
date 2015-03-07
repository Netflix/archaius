package netflix.archaius.mapper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import netflix.archaius.Config;
import netflix.archaius.exceptions.MappingException;
import netflix.archaius.mapper.annotations.Configuration;

import org.apache.commons.lang3.text.StrSubstitutor;

public class DefaultConfigBinder implements ConfigBinder {
    private final Config config;
    private static final IoCContainer NULL_IOC_CONTAINER = new IoCContainer() {
        @Override
        public <T> T getInstance(String name, Class<T> type) {
            return null;
        }
    };
    
    public DefaultConfigBinder(Config config) {
        this.config = config;
    }
    
    @Override
    public <T> void bindConfig(T injectee) throws MappingException {
        bindConfig(injectee, NULL_IOC_CONTAINER);
    }

    @Override
    public <T> void bindConfig(T injectee, IoCContainer ioc) throws MappingException {
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
        prefix = config.getStrInterpolator().resolve(prefix).toString();
        if (!prefix.isEmpty() || !prefix.endsWith("."))
            prefix += ".";
        
        // Iterate and set fiels
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
                    e.printStackTrace();
                    throw new MappingException("Unable to inject field " + injectee.getClass() + "." + name + " with value " + value, e);
                }
            }
        }
        
        // map to setter methods
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
}
