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
 *     Property<String> p = factory.createProperty("my.service.property")
 *           .subscribe(new PropertyObserver<String>() {
 *               public void onUpdate(String newValue, String oldValue) {
 *               }
 *           }).asString("defaultValue");
 *     ...
 *     // When shutting down call...
 *     p.unsubscribe();
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
    
    /**
     * Parse the property as a string 
     */
    Property<String> asString();
    
    /**
     * Parse the property as an int 
     */
    Property<Integer> asInteger();
    
    /**
     * Parse the property as a Long 
     */
    Property<Long> asLong();
    
    /**
     * Parse the property as a double 
     */
    Property<Double> asDouble();
    
    /**
     * Parse the property as a float 
     */
    Property<Float> asFloat();
    
    /**
     * Parse the property as a short 
     */
    Property<Short> asShort();
    
    /**
     * Parse the property as a byte 
     */
    Property<Byte> asByte();
    
    /**
     * Parse the property as a boolean 
     */
    Property<Boolean> asBoolean();

    /**
     * Parse the property as a BigDecimal 
     */
    Property<BigDecimal> asBigDecimal();

    /**
     * Parse the property as a BigInteger 
     */
    Property<BigInteger> asBigInteger();
    
    /**
     * Custom parsing based on the provided type.  An implementation of ObservableProperty 
     * should be optimized to call one of the known parsing methods based on type. 
     */
    <T> Property<T> asType(Class<T> type);

}
