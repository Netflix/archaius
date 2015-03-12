/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.archaius.config.polling;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.netflix.archaius.config.PollingStrategy;

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
