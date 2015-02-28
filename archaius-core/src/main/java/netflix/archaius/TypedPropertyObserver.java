package netflix.archaius;

public interface TypedPropertyObserver<T> extends PropertyObserver<T> {
    Class<T> getType();
    T getDefaultValue();
}
