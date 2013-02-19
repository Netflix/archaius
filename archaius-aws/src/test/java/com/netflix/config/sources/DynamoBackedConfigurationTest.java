package com.netflix.config.sources;

import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.model.ScanRequest;
import com.netflix.config.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * User: gorzell
 * Date: 1/17/13
 * Time: 10:18 AM
 * You should write something useful here.
 */
public class DynamoBackedConfigurationTest {
    @Test
    public void testPropertyChange() throws Exception {
        AmazonDynamoDB mockBasicDbClient = mock(AmazonDynamoDB.class);

        //3 of the first config to cover: object creation, threadRun at 0 delay, load properties
        when(mockBasicDbClient.scan(any(ScanRequest.class))).thenReturn(DynamoDbMocks.basicScanResult1,
                DynamoDbMocks.basicScanResult1, DynamoDbMocks.basicScanResult1, DynamoDbMocks.basicScanResult2);
        DynamoDbConfigurationSource source = new DynamoDbConfigurationSource(mockBasicDbClient);

        FixedDelayPollingScheduler scheduler = new FixedDelayPollingScheduler(0, 500, false);
        DynamicConfiguration dynamicConfig = new DynamicConfiguration(source, scheduler);
        ConfigurationManager.loadPropertiesFromConfiguration(dynamicConfig);

        DynamicStringProperty test1 = DynamicPropertyFactory.getInstance().getStringProperty("foo", "");
        DynamicStringProperty test2 = DynamicPropertyFactory.getInstance().getStringProperty("goo", "");
        DynamicStringProperty test3 = DynamicPropertyFactory.getInstance().getStringProperty("boo", "");

        assertEquals("bar", test1.get());
        assertEquals("goo", test2.get());
        assertEquals("who", test3.get());

        Thread.sleep(1000);

        assertEquals("bar", test1.get());
        assertEquals("foo", test2.get());
        assertEquals("who", test3.get());
    }
}
