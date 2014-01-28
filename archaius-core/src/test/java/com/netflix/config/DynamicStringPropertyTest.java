package com.netflix.config;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for DynamicStringProperty
 * @author hyuan
 */
public class DynamicStringPropertyTest {

    private static final String NOCALLBACK = "no call back";
    private static final String AFTERCALLBACK = "after call back";
    private Runnable callback = new Runnable() {
        public void run() {
            if (AFTERCALLBACK.equals(callbackFlag)) {
                callbackFlag = NOCALLBACK;
            } else {
                callbackFlag = AFTERCALLBACK;
            }
        }
    };
    private static String callbackFlag = NOCALLBACK;
    @Before
    public void init() {
        ConfigurationManager.getConfigInstance().setProperty("testProperty", "abc");
    }

    @Test
    public void testCallbacksAddUnsubscribe() {
        DynamicStringProperty dp = new DynamicStringProperty("testProperty", null);
        dp.addCallback(callback);
        //trigger callback
        ConfigurationManager.getConfigInstance().setProperty("testProperty", "cde");
        assertEquals(AFTERCALLBACK, callbackFlag);
        dp.removeAllCallbacks();
        //trigger callback again
        ConfigurationManager.getConfigInstance().setProperty("testProperty", "def");
        assertEquals(AFTERCALLBACK, callbackFlag);
        dp.addCallback(callback);
        ConfigurationManager.getConfigInstance().setProperty("testProperty", "efg");
        assertEquals(NOCALLBACK, callbackFlag);

    }
}
