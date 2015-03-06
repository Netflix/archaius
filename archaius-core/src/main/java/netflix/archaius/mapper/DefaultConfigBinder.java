package netflix.archaius.mapper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import netflix.archaius.Config;
import netflix.archaius.exceptions.MappingException;
import netflix.archaius.mapper.annotations.Configuration;

public class DefaultConfigBinder implements ConfigBinder {
    private final Config config;

    public DefaultConfigBinder(Config config) {
        this.config = config;
    }
    
    @Override
    public void bindConfig(Object injectee) throws MappingException {
        Configuration configAnnot = injectee.getClass().getAnnotation(Configuration.class);
        if (configAnnot != null) {
            String prefix = config.getStrInterpolator().resolve(configAnnot.prefix()).toString();
            if (!prefix.isEmpty() || !prefix.endsWith("."))
                prefix += ".";
            
            for (Field field : injectee.getClass().getDeclaredFields()) {
                String name = field.getName();
                Class<?> type = field.getType();
                Object value = config.get(type, prefix + name, null);
                if (value != null) {
                    try {
                        field.setAccessible(true);
                        field.set(injectee, value);
                    } catch (Exception e) {
                        throw new MappingException("Unable to inject field " + injectee.getClass() + "." + name + " with value " + value, e);
                    }
                }
            }
            
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
                Object value = config.get(type, prefix + name, null);
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
}
