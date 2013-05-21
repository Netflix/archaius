/**
 * Copyright 2013 Netflix, Inc.
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

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;

import static com.netflix.config.sources.DynamoDbIntegrationTestHelper.*;

import com.netflix.config.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;



import static org.junit.Assert.assertEquals;

/**
 * User: gorzell
 * Date: 8/23/12
 */
public class DynamoBackedConfigurationIntegrationTest {
    private static final String tableName = DynamoDbConfigurationSource.defaultTable + "UNITTEST";
    private static AmazonDynamoDB dbClient;

    @BeforeClass
    public static void setUpClass() throws Exception {
        try {
            dbClient = new AmazonDynamoDBClient(new DefaultAWSCredentialsProviderChain().getCredentials());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.setProperty("com.netflix.config.dynamo.tableName", tableName);
        if (dbClient != null) {
            createTable(dbClient, tableName);
            addElements(dbClient, tableName);
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (dbClient != null) removeTable(dbClient, tableName);
    }

    //@Test
    public void testPropertyChange() throws Exception{
        System.setProperty("com.netflix.config.dynamo.tableName", tableName);
        if (dbClient != null) {
            DynamoDbConfigurationSource source = new DynamoDbConfigurationSource(dbClient);
            FixedDelayPollingScheduler scheduler = new FixedDelayPollingScheduler(0, 1000, false);
            DynamicConfiguration dynamicConfig = new DynamicConfiguration(source, scheduler);
            ConfigurationManager.loadPropertiesFromConfiguration(dynamicConfig);

            DynamicStringProperty test1 = DynamicPropertyFactory.getInstance().getStringProperty("test1","");
            DynamicStringProperty test2 = DynamicPropertyFactory.getInstance().getStringProperty("test2","");
            DynamicStringProperty test3 = DynamicPropertyFactory.getInstance().getStringProperty("test3","");

            assertEquals("val1", test1.get());
            assertEquals("val2", test2.get());
            assertEquals("val3", test3.get());

            updateValues(dbClient, tableName);
            Thread.sleep(5000);

            assertEquals("vala", test1.get());
            assertEquals("valb", test2.get());
            assertEquals("valc", test3.get());
        }
    }
}
