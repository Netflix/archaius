package com.netflix.config;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

    @Ignore
    public void testWeakRef() throws InterruptedException {
        DynamicStringProperty dp = new DynamicStringProperty("testWeakRef", null);
        dp.addCallback(new Runnable() {
            @Override
            public void run() {
                System.out.println("No-op");
            }
        });
        assertEquals(dp.getDynamicProperty().getCallbacks().size(), 1);
        int count = 0;
        //don't release dp. callback is going to be there
        while(count < 3) {
            System.gc();
            count++;
        }
        assertEquals(dp.getDynamicProperty().getCallbacks().size(), 1);

        //setup dp for GC
        dp = null;
        count = 0;
        while(DynamicPropertyFactory.getInstance().
                getStringProperty("testWeakRef", null).getDynamicProperty().getCallbacks().size() != 0 && count < 3) {
            System.gc();
            count ++;

        }
        if (DynamicPropertyFactory.getInstance().
                getStringProperty("testWeakRef", null).getDynamicProperty().getCallbacks().size() == 0) {
            assertTrue(true);
        } else {
            System.out.println("Fail to force a GC. Ignore the test");
        }

    }

    @Ignore
    public void testOverrideCallback() {
        DynamicStringProperty dp = new DynamicStringProperty("testWeakRef", null) {
          @Override
          public String getValue() {
            return "No-Op";
          }
        };
        assertEquals(dp.getDynamicProperty().getCallbacks().size(), 1);
        int count = 0;
        //don't release dp. callback is going to be there
        while(count < 3) {
            System.gc();
            count++;
        }
        assertEquals(dp.getDynamicProperty().getCallbacks().size(), 1);

        //setup dp for GC
        dp = null;
        count = 0;
        while(DynamicPropertyFactory.getInstance().
                getStringProperty("testWeakRef", null).getDynamicProperty().getCallbacks().size() != 0 && count < 3) {
            System.gc();
            count ++;

        }
        if (DynamicPropertyFactory.getInstance().
                getStringProperty("testWeakRef", null).getDynamicProperty().getCallbacks().size() == 0) {
            assertTrue(true);
        } else {
            System.out.println("Fail to force a GC. Ignore the test");
        }

    }
}
