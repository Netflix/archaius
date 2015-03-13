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
package com.netflix.archaius.util;

import java.util.concurrent.locks.ReentrantLock;

public class ShardedReentrantLock {
    private final ReentrantLock locks[];
    private final int size;
    private final int mask;
    
    public ShardedReentrantLock(int count) {
        this.size = (int)(count * Math.ceil((double)count / 2));
        mask = size-1;
        
        locks = new ReentrantLock[size];
        for (int i = 0; i < size; i++) {
            locks[i] = new ReentrantLock();
        }
    }
    
    public ReentrantLock get(String key) {
        return locks[key.hashCode() & mask];
    }
}
