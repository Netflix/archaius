package netflix.archaius;

/**
 * Listener for property updates.  Due to the cascading nature of property value resolution
 * there's not much value in specifying the value or differentiating between set, add and
 * delete.  Instead the listener is expected to fetch the property value from the config.
 * 
 * @author elandau
 */
public interface PropertyListener {
    /**
     * Notify the parent that the value of a key has changed.  The parent is expected
     * to refresh its complete state.
     * 
     * @param propName
     */
    public void onUpdate(String propName, Config config);
}
