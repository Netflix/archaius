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
import com.netflix.config.DeploymentContext;
import com.netflix.config.PropertyWithDeploymentContext;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * User: hieurl
 * Date: 5/5/16
 * Time: 10:18 AM
 * You should write something useful here.
 */
public class DynamoDbDeploymentContextTableCacheWithMultipleTableTest {
    private static final PropertyWithDeploymentContext test1 =
            new PropertyWithDeploymentContext(DeploymentContext.ContextKey.environment,
                    "test", "foo", "bar");
    private static final PropertyWithDeploymentContext test2 =
            new PropertyWithDeploymentContext(DeploymentContext.ContextKey.environment,
                    "test", "goo", "goo");
    private static final PropertyWithDeploymentContext test5 =
            new PropertyWithDeploymentContext(DeploymentContext.ContextKey.environment,
                    "test", "boo", "who");
    private static final PropertyWithDeploymentContext test6 =
            new PropertyWithDeploymentContext(null,
                    null, "foo", "bar");
    private static final PropertyWithDeploymentContext test7 =
            new PropertyWithDeploymentContext(DeploymentContext.ContextKey.environment,
                    "test", "meow", "grr");

    @BeforeClass
    public static void setUpClass() throws Exception {
        String tableName = DynamoDbConfigurationSource.defaultTable + "TABLE1,"
                + DynamoDbConfigurationSource.defaultTable + "TABLE2";
        System.setProperty("com.netflix.config.dynamo.tableName", tableName);
    }

    @Test
    public void testPollMultiTable() throws Exception {
        String tableName = DynamoDbConfigurationSource.defaultTable + "TABLE1,"
                + DynamoDbConfigurationSource.defaultTable + "TABLE2";
        System.setProperty("com.netflix.config.dynamo.tableName", tableName);
        AmazonDynamoDB mockContextDbClient = mock(AmazonDynamoDB.class);

        when(mockContextDbClient.scan(any(ScanRequest.class))).thenReturn(DynamoDbMocks.contextScanResult1,
                DynamoDbMocks.contextScanResult3);
        DynamoDbDeploymentContextTableCache cache = new DynamoDbDeploymentContextTableCache(mockContextDbClient, 100, 100);

        Collection<PropertyWithDeploymentContext> props = cache.getProperties();
        assertEquals(5, props.size());
        assertTrue(props.contains(test1));
        assertTrue(props.contains(test2));
        assertTrue(props.contains(test5));
        assertTrue(props.contains(test6));
        assertTrue(props.contains(test7));
    }
}
