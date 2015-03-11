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
