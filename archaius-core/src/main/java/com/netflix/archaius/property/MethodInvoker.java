package com.netflix.archaius.property;

import java.lang.reflect.Method;

public class MethodInvoker<T> extends DefaultPropertyObserver<T> {
    
    private Method method;
    private Object obj;
    
    public MethodInvoker(Object obj, String methodName) {
        this.method = getMethodWithOneParameter(obj, methodName);
        this.obj    = obj;
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
    public void onChange(T newValue) {
        try {
            method.invoke(obj, newValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
