package netflix.archaius.config.polling;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import netflix.archaius.config.PollingStrategy;

/**
 * Polling strategy using external input to trigger a refresh.  This should only be used
 * for testing.
 * 
 * @author elandau
 *
 */
public class ManualPollingStrategy implements PollingStrategy {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final LinkedBlockingQueue<Request> queue = new LinkedBlockingQueue<Request>();
    
    private static class Request {
        CountDownLatch latch = new CountDownLatch(1);
        Exception error;
    }
    
    @Override
    public Future<?> execute(final Callable<Boolean> run) {
        return executor.submit(new Runnable() {
            @Override
            public void run() {
                Request request;
                try {
                    while (null != (request = queue.take())) {
                        try {
                            run.call();
                        } catch (Exception e) {
                            request.error = e;
                        } finally {
                            request.latch.countDown();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }
    
    public void fire(long timeout, TimeUnit units) throws Exception {
        Request request = new Request();
        queue.put(request);
        request.latch.await(timeout, units);
        if (request.error != null) {
            throw request.error;
        }
    }

    public void fire() throws Exception {
        Request request = new Request();
        queue.put(request);
        request.latch.await();
        if (request.error != null) {
            throw request.error;
        }
    }

}
