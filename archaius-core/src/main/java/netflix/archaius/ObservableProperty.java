package netflix.archaius;

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
public interface ObservableProperty extends PropertyConversions {
    
    /**
     * Notify the observable that it should fetch the latest property value
     */
    void update();
}
