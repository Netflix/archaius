package com.netflix.config.sources;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.*;
import com.netflix.config.PollResult;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * User: gorzell
 * Date: 8/7/12
 */
public class DynamoDbConfigurationSourceTest {
    private static final String tableName = DynamoDbConfigurationSource.defaultTable + "UNITTEST";
    private static AmazonDynamoDB dbClient;

    @BeforeClass
    public static void setUpClass() throws Exception {
        dbClient = new AmazonDynamoDBClient(new DefaultAWSCredentialsProviderChain().getCredentials());
        System.setProperty("com.netflix.config.dynamo.tableName", tableName);
        createTable();
        addElements();
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        removeTable();
    }

    @Test
    public void testPoll() throws Exception {
        DynamoDbConfigurationSource testConfigSource = new DynamoDbConfigurationSource();
        PollResult result = testConfigSource.poll(true, null);

        assertEquals(3, result.getComplete().size());
        assertEquals("val1", result.getComplete().get("test1"));
        assertEquals("val2", result.getComplete().get("test2"));
        assertEquals("val3", result.getComplete().get("test3"));
    }

    private static void createTable() throws InterruptedException {
        KeySchemaElement hashKey = new KeySchemaElement()
                .withAttributeName(DynamoDbConfigurationSource.defaultKeyAttribute).withAttributeType("S");
        KeySchema ks = new KeySchema().withHashKeyElement(hashKey);

        ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput()
                .withReadCapacityUnits(10L)
                .withWriteCapacityUnits(10L);

        dbClient.createTable(new CreateTableRequest().withTableName(tableName)
                .withKeySchema(ks).withProvisionedThroughput(provisionedThroughput));

        while (!dbClient.describeTable(new DescribeTableRequest().withTableName(tableName)).getTable().getTableStatus().equalsIgnoreCase("active")) {
            Thread.sleep(10000);
        }
    }


    private static void addElements() {
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

    private static void removeTable() {
        dbClient.deleteTable(new DeleteTableRequest().withTableName(tableName));
    }
}
