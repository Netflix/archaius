package netflix.archaius;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * Create an observable stream of property value changes.  When subscribed to
 * the ObservableProperty will emit either the current value or default if no
 * value exists. 
 * 
 * {@code 
 * <pre>   
 *     Subscription s = config.listen("my.service.property")
 *           .subscribe(new PropertyObserver<String>() {
 *               public void onUpdate(String newValue, String oldValue) {
 *               }
 *           }, String.class, "defaultValue");
 *           
 *     ...
 *     s.unsubscribe();
 * </pre>
 * }
 * 
 * 
 * TODO: Chain properties
 * TODO: Property validator
 * 
 * @param callback
 * @return
 */
public interface ObservableProperty {
    
    /**
     * Notify the observable that it should fetch the latest property value
     */
    void update();
    
    Property<String> asString(String defaultValue);
    
    Property<Integer> asInteger(Integer defaultValue);
    
    Property<Double> asDouble(Double defaultValue);
    
    Property<Float> asFloat(Float defaultValue);
    
    Property<Short> asShort(Short defaultValue);
    
    Property<Byte> asByte(Byte defaultValue);
    
    Property<BigDecimal> asBigDecimal(BigDecimal defaultValue);

    Property<Boolean> asBoolean(Boolean defaultValue);

    Property<BigInteger> asBigInteger(BigInteger defaultValue);
    
    <T> Property<T> asType(Class<T> type, T defaultValue);

    Property<String> asString(String defaultValue, PropertyObserver<String> observer);
    
    Property<Integer> asInteger(Integer defaultValue, PropertyObserver<Integer> observer);
    
    Property<Double> asDouble(Double defaultValue, PropertyObserver<Double> observer);
    
    Property<Float> asFloat(Float defaultValue, PropertyObserver<Float> observer);
    
    Property<Short> asShort(Short defaultValue, PropertyObserver<Short> observer);
    
    Property<Byte> asByte(Byte defaultValue, PropertyObserver<Byte> observer);
    
    Property<BigDecimal> asBigDecimal(BigDecimal defaultValue, PropertyObserver<BigDecimal> observer);

    Property<Boolean> asBoolean(Boolean defaultValue, PropertyObserver<Boolean> observer);

    Property<BigInteger> asBigInteger(BigInteger defaultValue, PropertyObserver<BigInteger> observer);
    
    <T> Property<T> asType(Class<T> type, T defaultValue, PropertyObserver<T> observer);
}
