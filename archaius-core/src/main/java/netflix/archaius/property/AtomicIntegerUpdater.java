package netflix.archaius.property;

import java.util.concurrent.atomic.AtomicInteger;

import netflix.archaius.TypedPropertyObserver;

public class AtomicIntegerUpdater implements TypedPropertyObserver<Integer> {
    private final AtomicInteger value;
    private final int defaultValue;
    
    public AtomicIntegerUpdater(AtomicInteger value) {
        this.value = value;
        this.defaultValue = value.get();
    }
    
    @Override
    public void onChange(String propName, Integer prevValue, Integer newValue) {
        this.value.set(newValue);
    }

    @Override
    public Class<Integer> getType() {
        return Integer.class;
    }

    @Override
    public Integer getDefaultValue() {
        return defaultValue;
    }

    @Override
    public void onError(String propName, Throwable error) {
        // No op
    }
}
