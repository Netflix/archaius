package netflix.archaius;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * @author Spencer Gibb
 */
public class DefaultDecoder implements Decoder {

    @SuppressWarnings("unchecked")
    @Override
    public <T> T decode(Class<T> type, String encoded) {
        Constructor<T> c;
        try {
            c = type.getConstructor(String.class);
            return c.newInstance(encoded);
        }
        catch (NoSuchMethodException e) {
            Method method;
            try {
                method = type.getMethod("valueOf", String.class);
                return (T) method.invoke(null, encoded);
            } catch (NoSuchMethodException e1) {
                if (type.equals(boolean.class)) {
                    return (T) Boolean.valueOf(encoded);
                }
                else if (type.equals(int.class)) {
                    return (T) Integer.valueOf(encoded);
                }
                else if (type.equals(long.class)) {
                    return (T) Long.valueOf(encoded);
                }
                else if (type.equals(short.class)) {
                    return (T) Short.valueOf(encoded);
                }
                else if (type.equals(double.class)) {
                    return (T) Double.valueOf(encoded);
                }
                else if (type.equals(float.class)) {
                    return (T) Float.valueOf(encoded);
                }
            } catch (Exception e2) {
                try {
                    throw e2;
                } catch (Exception e1) {
                    throw new RuntimeException("Unable to instantiate value of type " + type.getCanonicalName(), e);
                }
            }
            throw new RuntimeException(type.getCanonicalName() + " has no String constructor or valueOf static method");
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate value of type " + type.getCanonicalName(), e);
        }
    }
}
