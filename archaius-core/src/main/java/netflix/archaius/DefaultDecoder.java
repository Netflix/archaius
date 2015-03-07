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
            if (c != null) {
                return c.newInstance(encoded);
            }

            Method method = type.getMethod("valueOf", String.class);
            if (method != null) {
                return (T) method.invoke(null, encoded);
            }
            throw new RuntimeException(type.getCanonicalName() + " has no String constructor or valueOf static method");
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate value of type " + type.getCanonicalName(), e);
        }
    }
}
