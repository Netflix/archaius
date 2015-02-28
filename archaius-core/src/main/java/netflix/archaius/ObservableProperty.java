package netflix.archaius;

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
 * @param callback
 * @return
 */
public interface ObservableProperty {
    
    /**
     * Notify the observable that it should fetch the latest property value
     */
    void reload();
    
    /**
     * Subscribe for property change notification.  The observer will be called 
     * immediately with the latest value or the defaultValue if not set.
     * 
     * @param observer
     * @param type
     * @param defaultValue
     * @return
     */
    <T> PropertySubscription subscribe(PropertyObserver<T> observer, Class<T> type, T defaultValue);
}
