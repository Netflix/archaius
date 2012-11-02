package com.netflix.config;

import static org.junit.Assert.*;

import java.util.Random;

import org.apache.commons.configuration.BaseConfiguration;
import org.junit.Test;

public class MultiThreadedInit {


    @Test
    public void test() {
        final BaseConfiguration baseConfig = new BaseConfiguration();
        baseConfig.setProperty("abc", 1);
        (new Thread() {
            public void run() {
                Random r = new Random();
                while (DynamicPropertyFactory.getBackingConfigurationSource() != baseConfig) {
                    try {
                        Thread.sleep(r.nextInt(100) + 1);
                    } catch (InterruptedException e) {} 
                    System.setProperty(DynamicPropertyFactory.DISABLE_DEFAULT_CONFIG, "true");
                    ConfigurationManager.install(baseConfig);
                }
            }
        }).start();
        Object config = null;
        DynamicIntProperty prop = DynamicPropertyFactory.getInstance().getIntProperty("abc", 0);
        while ((config = DynamicPropertyFactory.getBackingConfigurationSource()) != baseConfig && prop.get() != 1) {            
            // prop = DynamicPropertyFactory.getInstance().getIntProperty("abc", 0);
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {}
        }
        
    }
}
