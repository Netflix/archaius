package netflix.archaius.config;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * Strategy for the polling of a source.  
 * @author elandau
 *
 */
public interface PollingStrategy {
    public Future<?> execute(Callable<Boolean> run);
}
