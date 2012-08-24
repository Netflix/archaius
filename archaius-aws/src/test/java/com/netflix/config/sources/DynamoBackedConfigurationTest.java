package com.netflix.config.sources;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.*;
import com.netflix.config.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * User: gorzell
 * Date: 8/23/12
 */
public class DynamoBackedConfigurationTest {

    @Test
    public void testPropertyChange() throws Exception{
        String tableName = DynamoDbConfigurationSource.defaultTable + "UNITTEST";
        AmazonDynamoDB dbClient = null;

        try {
            dbClient = new AmazonDynamoDBClient(new DefaultAWSCredentialsProviderChain().getCredentials());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.setProperty("com.netflix.config.dynamo.tableName", tableName);
        if (dbClient != null) {
            createTable(dbClient, tableName);
            addElements(dbClient, tableName);

            DynamoDbConfigurationSource source = new DynamoDbConfigurationSource(dbClient);
            FixedDelayPollingScheduler scheduler = new FixedDelayPollingScheduler(0, 1000, false);
            DynamicConfiguration dynamicConfig = new DynamicConfiguration(source, scheduler);
            DynamicPropertyFactory.initWithConfigurationSource(dynamicConfig);

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

    private void updateValues(AmazonDynamoDB dbClient, String tableName){
        Map<String, List<WriteRequest>> requestMap = new HashMap<String, List<WriteRequest>>(1);
        List<WriteRequest> writeList = new ArrayList<WriteRequest>(3);

        Map<String, AttributeValue> item1 = new HashMap<String, AttributeValue>(1);
        item1.put(DynamoDbConfigurationSource.defaultKeyAttribute, new AttributeValue().withS("test1"));
        item1.put(DynamoDbConfigurationSource.defaultValueAttribute, new AttributeValue().withS("vala"));
        writeList.add(new WriteRequest().withPutRequest(new PutRequest().withItem(item1)));

        HashMap<String, AttributeValue> item2 = new HashMap<String, AttributeValue>(1);
        item2.put(DynamoDbConfigurationSource.defaultKeyAttribute, new AttributeValue().withS("test2"));
        item2.put(DynamoDbConfigurationSource.defaultValueAttribute, new AttributeValue().withS("valb"));
        writeList.add(new WriteRequest().withPutRequest(new PutRequest().withItem(item2)));

        HashMap<String, AttributeValue> item3 = new HashMap<String, AttributeValue>(1);
        item3.put(DynamoDbConfigurationSource.defaultKeyAttribute, new AttributeValue().withS("test3"));
        item3.put(DynamoDbConfigurationSource.defaultValueAttribute, new AttributeValue().withS("valc"));
        writeList.add(new WriteRequest().withPutRequest(new PutRequest().withItem(item3)));

        requestMap.put(tableName, writeList);

        BatchWriteItemRequest request = new BatchWriteItemRequest().withRequestItems(requestMap);
        dbClient.batchWriteItem(request);
    }

    private static void createTable(AmazonDynamoDB dbClient, String tableName) throws InterruptedException {
        KeySchemaElement hashKey = new KeySchemaElement()
                .withAttributeName(DynamoDbConfigurationSource.defaultKeyAttribute).withAttributeType("S");
        KeySchema ks = new KeySchema().withHashKeyElement(hashKey);

        ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
                .withReadCapacityUnits(1L)
                .withWriteCapacityUnits(1L);

        dbClient.createTable(new CreateTableRequest().withTableName(tableName)
                .withKeySchema(ks).withProvisionedThroughput(provisionedThroughput));

        while (!dbClient.describeTable(new DescribeTableRequest().withTableName(tableName)).getTable().getTableStatus().equalsIgnoreCase("active")) {
            Thread.sleep(10000);
        }
    }


    private static void addElements(AmazonDynamoDB dbClient, String tableName) {
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

    private static void removeTable(AmazonDynamoDB dbClient, String tableName) {
        if (dbClient != null) {
            dbClient.deleteTable(new DeleteTableRequest().withTableName(tableName));
        }
    }

}
