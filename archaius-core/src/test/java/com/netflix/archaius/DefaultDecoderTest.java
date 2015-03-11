package com.netflix.archaius;


import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.DefaultDecoder;

public class DefaultDecoderTest {
    @Test
    public void testPrimitives() {
        DefaultDecoder decoder = new DefaultDecoder();
        
        boolean flag = decoder.decode(boolean.class, "true");
        Assert.assertEquals(true, flag);
        int int_value = decoder.decode(int.class, "123");
        Assert.assertEquals(123, int_value);
    }
}
