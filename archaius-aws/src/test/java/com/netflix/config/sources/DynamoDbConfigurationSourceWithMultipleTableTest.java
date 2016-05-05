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
import com.netflix.config.PollResult;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * User: hieurl
 * Date: 5/5/16
 * Time: 10:18 AM
 * You should write something useful here.
 */
public class DynamoDbConfigurationSourceWithMultipleTableTest {
    @BeforeClass
    public static void setUpClass() throws Exception {
        String tableName = DynamoDbConfigurationSource.defaultTable + "TABLE1,"
                + DynamoDbConfigurationSource.defaultTable + "TABLE2";
        System.setProperty("com.netflix.config.dynamo.tableName", tableName);
    }

    @Test
    public void testMultiTable() throws Exception {
        AmazonDynamoDB mockBasicDbClient = mock(AmazonDynamoDB.class);
        when(mockBasicDbClient.scan(any(ScanRequest.class))).thenReturn(DynamoDbMocks.basicScanResult1, DynamoDbMocks.basicScanResult3);

        DynamoDbConfigurationSource testConfigSource = new DynamoDbConfigurationSource(mockBasicDbClient);
        PollResult result = testConfigSource.poll(false, null);

        assertEquals(4, result.getComplete().size());
        assertEquals("bar", result.getComplete().get("foo"));
        assertEquals("goo", result.getComplete().get("goo"));
        assertEquals("who", result.getComplete().get("boo"));
        assertEquals("grr", result.getComplete().get("meow"));
    }
}
