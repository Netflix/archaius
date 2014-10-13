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
import com.amazonaws.services.dynamodbv2.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DynamoDbIntegrationTestHelper {

    static void createTable(AmazonDynamoDB dbClient, String tableName) throws InterruptedException {
        //TODO check to make sure the table isn't being created or deleted.
        KeySchemaElement hashKey = new KeySchemaElement()
                .withAttributeName(DynamoDbConfigurationSource.defaultKeyAttribute).withKeyType(KeyType.HASH);

        ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L);

        dbClient.createTable(new CreateTableRequest().withTableName(tableName)
                .withKeySchema(hashKey).withProvisionedThroughput(provisionedThroughput));

        while (!dbClient.describeTable(new DescribeTableRequest().withTableName(tableName)).getTable().getTableStatus().equalsIgnoreCase("active")) {
            Thread.sleep(10000);
        }
    }

    static void addElements(AmazonDynamoDB dbClient, String tableName) {
        Map<String, List<WriteRequest>> requestMap = new HashMap<String, List<WriteRequest>>(1);
        List<WriteRequest> writeList = new ArrayList<WriteRequest>(3);

        Map<String, AttributeValue> item1 = new HashMap<String, AttributeValue>(1);
        item1.put(DynamoDbConfigurationSource.defaultKeyAttribute, new AttributeValue().withS("test1"));
        item1.put(DynamoDbConfigurationSource.defaultValueAttribute, new AttributeValue().withS("val1"));
        writeList.add(new WriteRequest().withPutRequest(new PutRequest().withItem(item1)));

        HashMap<String, AttributeValue> item2 = new HashMap<String, AttributeValue>(1);
        item2.put(DynamoDbConfigurationSource.defaultKeyAttribute, new AttributeValue().withS("test2"));
        item2.put(DynamoDbConfigurationSource.defaultValueAttribute, new AttributeValue().withS("val2"));
        writeList.add(new WriteRequest().withPutRequest(new PutRequest().withItem(item2)));

        HashMap<String, AttributeValue> item3 = new HashMap<String, AttributeValue>(1);
        item3.put(DynamoDbConfigurationSource.defaultKeyAttribute, new AttributeValue().withS("test3"));
        item3.put(DynamoDbConfigurationSource.defaultValueAttribute, new AttributeValue().withS("val3"));
        writeList.add(new WriteRequest().withPutRequest(new PutRequest().withItem(item3)));

        requestMap.put(tableName, writeList);

        BatchWriteItemRequest request = new BatchWriteItemRequest().withRequestItems(requestMap);
        dbClient.batchWriteItem(request);
    }

    static void updateValues(AmazonDynamoDB dbClient, String tableName) {

        Map<String, AttributeValue> key1 = new HashMap<String, AttributeValue>(1);
        key1.put("test1", new AttributeValue().withS("HASH"));

        Map<String, AttributeValueUpdate> item1 = new HashMap<String, AttributeValueUpdate>(1);
        item1.put(DynamoDbConfigurationSource.defaultValueAttribute, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT).withValue(new AttributeValue().withS("vala")));

        dbClient.updateItem(new UpdateItemRequest().withTableName(tableName).
                withKey(key1).withAttributeUpdates(item1));

        Map<String, AttributeValue> key2 = new HashMap<String, AttributeValue>(1);
        key2.put("test2", new AttributeValue().withS("HASH"));

        HashMap<String, AttributeValueUpdate> item2 = new HashMap<String, AttributeValueUpdate>(1);
        item2.put(DynamoDbConfigurationSource.defaultValueAttribute, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT).withValue(new AttributeValue().withS("valb")));

        dbClient.updateItem(new UpdateItemRequest().withTableName(tableName).
                withKey(key2).withAttributeUpdates(item2));

        Map<String, AttributeValue> key3 = new HashMap<String, AttributeValue>(1);
        key3.put("test3", new AttributeValue().withS("HASH"));

        HashMap<String, AttributeValueUpdate> item3 = new HashMap<String, AttributeValueUpdate>(1);
        item3.put(DynamoDbConfigurationSource.defaultValueAttribute, new AttributeValueUpdate()
                .withAction(AttributeAction.PUT).withValue(new AttributeValue().withS("valc")));

        dbClient.updateItem(new UpdateItemRequest().withTableName(tableName).
                withKey(key3).withAttributeUpdates(item3));
    }

    static void removeTable(AmazonDynamoDB dbClient, String tableName) {
        //TODO check to make sure the table isn't being created or deleted.
        if (dbClient != null) {
            dbClient.deleteTable(new DeleteTableRequest().withTableName(tableName));
        }
    }
}
