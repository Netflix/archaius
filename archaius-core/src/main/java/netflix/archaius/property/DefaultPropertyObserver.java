package netflix.archaius.property;

import netflix.archaius.PropertyObserver;

public class DefaultPropertyObserver<T> implements PropertyObserver<T> {
    @Override
    public void onChange(T value) {
    }
    
    @Override
    public void onError(Throwable error) {
    }
}
