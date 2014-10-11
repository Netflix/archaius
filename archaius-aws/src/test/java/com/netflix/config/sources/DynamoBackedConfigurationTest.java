/**
 * Copyright 2014 Netflix, Inc.
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
package com.netflix.config.sources;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
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

        Thread.sleep(1250);

        assertEquals("bar", test1.get());
        assertEquals("foo", test2.get());
        assertEquals("who", test3.get());
    }
}
