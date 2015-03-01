package netflix.archaius.property;

import java.lang.reflect.Method;

import netflix.archaius.TypedPropertyObserver;

public class MethodInvoker<T> implements TypedPropertyObserver<T> {
    
    private Method method;
    private Object obj;
    private Class<T> type;
    private T defaultValue;
    
    public MethodInvoker(Object obj, String methodName, T defaultValue) {
        this.method = getMethodWithOneParameter(obj, methodName);
        this.obj    = obj;
        this.type   = (Class<T>) method.getParameterTypes()[0]; 
        this.defaultValue = defaultValue;
    }
    
    private static Method getMethodWithOneParameter(Object obj, String methodName) {
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
    public void onChange(String propName, T prevValue, T newValue) {
        try {
            method.invoke(obj, newValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(String propName, Throwable error) {
        // No op
    }

    @Override
    public Class<T> getType() {
        return type;
    }

    @Override
    public T getDefaultValue() {
        return defaultValue;
    }

}
