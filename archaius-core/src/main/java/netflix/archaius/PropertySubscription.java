package netflix.archaius;

/**
 * Return from {@link ObservableProperty#subscribe}.  Call unsubscribe() to stop receiving
 * updates.
 * 
 * @author elandau
 *
 */
public interface PropertySubscription {
    public void unsubscribe();
}
