/*
 *
 *  Copyright 2012 Sumo Logic, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */
package com.netflix.config.sources;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.Key;
import com.amazonaws.services.dynamodb.model.ScanRequest;
import com.amazonaws.services.dynamodb.model.ScanResult;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import com.netflix.config.PollResult;
import com.netflix.config.PolledConfigurationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * User: gorzell
 * Date: 8/6/12
 */
public class DynamoDbConfigurationSource implements PolledConfigurationSource {
    private static final Logger log = LoggerFactory.getLogger(DynamoDbConfigurationSource.class);

    //Property names
    static final String tablePropertyName = "com.netflix.config.dynamo.tableName";
    static final String keyAttributePropertyName = "com.netflix.config.dynamo.keyAttributeName";
    static final String valueAttributePropertyName = "com.netflix.config.dynamo.valueAttributeName";

    //Property defaults
    static final String defaultTable = "archaiusProperties";
    static final String defaultKeyAttribute = "key";
    static final String defaultValueAttribute = "value";

    //Dynamic Properties
    private DynamicStringProperty tableName = DynamicPropertyFactory.getInstance()
            .getStringProperty(tablePropertyName, defaultTable);
    private DynamicStringProperty keyAttributeName = DynamicPropertyFactory.getInstance()
            .getStringProperty(keyAttributePropertyName, defaultKeyAttribute);
    private DynamicStringProperty valueAttributeName = DynamicPropertyFactory.getInstance()
            .getStringProperty(valueAttributePropertyName, defaultValueAttribute);


    private AmazonDynamoDB dbClient;

    public DynamoDbConfigurationSource() {
        this(new DefaultAWSCredentialsProviderChain().getCredentials());
    }

    public DynamoDbConfigurationSource(AWSCredentials credentials) {
        this(new AmazonDynamoDBClient(credentials));
    }

    public DynamoDbConfigurationSource(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
        String table = tableName.get();
        Map<String, Object> map = load(table, keyAttributeName.get(), valueAttributeName.get());
        log.info("Successfully polled Dynamo for a new configuration based on table:" + table);
    }

    @Override
    public PollResult poll(boolean initial, Object checkPoint) throws Exception {
        String table = tableName.get();
        Map<String, Object> map = load(table, keyAttributeName.get(), valueAttributeName.get());
        log.info("Successfully polled Dynamo for a new configuration based on table:" + table);
        return PollResult.createFull(map);
    }

    private synchronized Map<String, Object> load(String table, String key, String val) {
        Map<String, Object> propertyMap = new HashMap<String, Object>();
        Key lastKeyEvaluated = null;
        do {
            ScanRequest scanRequest = new ScanRequest()
                    .withTableName(table)
                    .withExclusiveStartKey(lastKeyEvaluated);
            ScanResult result = dbClient.scan(scanRequest);
            for (Map<String, AttributeValue> item : result.getItems()) {
                propertyMap.put(item.get(key).getS(), item.get(val).getS());
            }
            lastKeyEvaluated = result.getLastEvaluatedKey();
        } while (lastKeyEvaluated != null);
        return propertyMap;
    }
}
