package com.netflix.config.sources;

import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.ScanResult;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * User: gorzell
 * Date: 1/17/13
 * Time: 10:18 AM
 * A set of mock dynamo values/responses that can be used in unit tests.
 */
public class DynamoDbMocks {

    static final String defaultKeyAttribute = AbstractDynamoDbConfigurationSource.defaultKeyAttribute;
    static final String defaultValueAttribute = AbstractDynamoDbConfigurationSource.defaultValueAttribute;
    static final String defaultContextKeyAttribute = DynamoDbDeploymentContextTableCache.defaultContextKeyAttribute;
    static final String defaultContextValueAttribute = DynamoDbDeploymentContextTableCache.defaultContextValueAttribute;
    static final String defaultContextAttribute = DynamoDbDeploymentContextTableCache.defaultContextAttribute;

    public static final Collection<Map<String, AttributeValue>> basicResultValues1 = new LinkedList<Map<String, AttributeValue>>();
    public static final Collection<Map<String, AttributeValue>> basicResultValues2 = new LinkedList<Map<String, AttributeValue>>();
    public static final ScanResult basicScanResult1;
    public static final ScanResult basicScanResult2;

    public static final Collection<Map<String, AttributeValue>> contextResultValues1 = new LinkedList<Map<String, AttributeValue>>();
    public static final Collection<Map<String, AttributeValue>> contextResultValues2 = new LinkedList<Map<String, AttributeValue>>();
    public static final ScanResult contextScanResult1;
    public static final ScanResult contextScanResult2;

    public static final Collection<Map<String, AttributeValue>> newContextResultValues1 = new LinkedList<Map<String, AttributeValue>>();
    public static final Collection<Map<String, AttributeValue>> newContextResultValues2 = new LinkedList<Map<String, AttributeValue>>();
    public static final ScanResult newContextScanResult1;
    public static final ScanResult newContextScanResult2;    


    static {
        //Basic results config
        Map<String, AttributeValue> basicRow1 = new HashMap<String, AttributeValue>();
        basicRow1.put(defaultKeyAttribute, new AttributeValue().withS("foo"));
        basicRow1.put(defaultValueAttribute, new AttributeValue().withS("bar"));
        basicResultValues1.add(basicRow1);

        Map<String, AttributeValue> basicRow2 = new HashMap<String, AttributeValue>();
        basicRow2.put(defaultKeyAttribute, new AttributeValue().withS("goo"));
        basicRow2.put(defaultValueAttribute, new AttributeValue().withS("goo"));
        basicResultValues1.add(basicRow2);

        Map<String, AttributeValue> basicRow3 = new HashMap<String, AttributeValue>();
        basicRow3.put(defaultKeyAttribute, new AttributeValue().withS("boo"));
        basicRow3.put(defaultValueAttribute, new AttributeValue().withS("who"));
        basicResultValues1.add(basicRow3);

        //Result2
        Map<String, AttributeValue> updatedBasicRow = new HashMap<String, AttributeValue>();
        updatedBasicRow.put(defaultKeyAttribute, new AttributeValue().withS("goo"));
        updatedBasicRow.put(defaultValueAttribute, new AttributeValue().withS("foo"));
        basicResultValues2.add(updatedBasicRow);

        basicResultValues2.add(basicRow1);
        basicResultValues2.add(updatedBasicRow);
        basicResultValues2.add(basicRow3);

        basicScanResult1 = new ScanResult().withItems(basicResultValues1).withLastEvaluatedKey(null);
        basicScanResult2 = new ScanResult().withItems(basicResultValues2).withLastEvaluatedKey(null);

        //DeploymentContext results config
        Map<String, AttributeValue> contextRow1 = new HashMap<String, AttributeValue>();
        contextRow1.put(defaultKeyAttribute, new AttributeValue().withS("foo"));
        contextRow1.put(defaultValueAttribute, new AttributeValue().withS("bar"));
        contextRow1.put(defaultContextKeyAttribute, new AttributeValue().withS("environment"));
        contextRow1.put(defaultContextValueAttribute, new AttributeValue().withS("test"));
        contextResultValues1.add(contextRow1);

        Map<String, AttributeValue> contextRow2 = new HashMap<String, AttributeValue>();
        contextRow2.put(defaultKeyAttribute, new AttributeValue().withS("goo"));
        contextRow2.put(defaultValueAttribute, new AttributeValue().withS("goo"));
        contextRow2.put(defaultContextKeyAttribute, new AttributeValue().withS("environment"));
        contextRow2.put(defaultContextValueAttribute, new AttributeValue().withS("test"));
        contextResultValues1.add(contextRow2);

        Map<String, AttributeValue> contextRow3 = new HashMap<String, AttributeValue>();
        contextRow3.put(defaultKeyAttribute, new AttributeValue().withS("boo"));
        contextRow3.put(defaultValueAttribute, new AttributeValue().withS("who"));
        contextRow3.put(defaultContextKeyAttribute, new AttributeValue().withS("environment"));
        contextRow3.put(defaultContextValueAttribute, new AttributeValue().withS("test"));
        contextResultValues1.add(contextRow3);

        contextResultValues1.add(basicRow1);

        //Result2
        contextResultValues2.add(contextRow1);
        contextResultValues2.add(contextRow3);

        Map<String, AttributeValue> contextRow4 = new HashMap<String, AttributeValue>();
        contextRow4.put(defaultKeyAttribute, new AttributeValue().withS("goo"));
        contextRow4.put(defaultValueAttribute, new AttributeValue().withS("foo"));
        contextRow4.put(defaultContextKeyAttribute, new AttributeValue().withS("environment"));
        contextRow4.put(defaultContextValueAttribute, new AttributeValue().withS("prod"));
        contextResultValues2.add(contextRow4);

        Map<String, AttributeValue> updatedContextRow = new HashMap<String, AttributeValue>();
        updatedContextRow.put(defaultKeyAttribute, new AttributeValue().withS("goo"));
        updatedContextRow.put(defaultValueAttribute, new AttributeValue().withS("foo"));
        updatedContextRow.put(defaultContextKeyAttribute, new AttributeValue().withS("environment"));
        updatedContextRow.put(defaultContextValueAttribute, new AttributeValue().withS("test"));
        contextResultValues2.add(updatedContextRow);

        contextResultValues2.add(basicRow1);

        //Create results from initialized values
        contextScanResult1 = new ScanResult().withItems(contextResultValues1);
        contextScanResult2 = new ScanResult().withItems(contextResultValues2);

        //New DeploymentContext results config
        Map<String, AttributeValue> newContextRow1 = new HashMap<String, AttributeValue>();
        newContextRow1.put(defaultKeyAttribute, new AttributeValue().withS("foo"));
        newContextRow1.put(defaultValueAttribute, new AttributeValue().withS("bar"));
        newContextRow1.put(defaultContextAttribute, new AttributeValue().withS("environment=test"));    
        newContextResultValues1.add(newContextRow1);

        Map<String, AttributeValue> newContextRow2 = new HashMap<String, AttributeValue>();
        newContextRow2.put(defaultKeyAttribute, new AttributeValue().withS("goo"));
        newContextRow2.put(defaultValueAttribute, new AttributeValue().withS("goo"));
        newContextRow2.put(defaultContextAttribute, new AttributeValue().withS("environment=test"));
        newContextResultValues1.add(newContextRow2);

        Map<String, AttributeValue> newContextRow3 = new HashMap<String, AttributeValue>();
        newContextRow3.put(defaultKeyAttribute, new AttributeValue().withS("boo"));
        newContextRow3.put(defaultValueAttribute, new AttributeValue().withS("who"));
        newContextRow3.put(defaultContextAttribute, new AttributeValue().withS("environment=test"));
        newContextResultValues1.add(newContextRow3);    

        Map<String, AttributeValue> newGlobalRow1 = new HashMap<String, AttributeValue>();
        newGlobalRow1.put(defaultKeyAttribute, new AttributeValue().withS("foo"));
        newGlobalRow1.put(defaultValueAttribute, new AttributeValue().withS("bar"));
        newGlobalRow1.put(defaultContextAttribute, new AttributeValue().withS("global"));
        newContextResultValues1.add(newGlobalRow1);

        //Result2
        newContextResultValues2.add(contextRow1);
        newContextResultValues2.add(contextRow3);

        Map<String, AttributeValue> newContextRow4 = new HashMap<String, AttributeValue>();
        newContextRow4.put(defaultKeyAttribute, new AttributeValue().withS("goo"));
        newContextRow4.put(defaultValueAttribute, new AttributeValue().withS("foo"));
        newContextRow4.put(defaultContextAttribute, new AttributeValue().withS("environment=prod"));
        newContextResultValues2.add(newContextRow4);

        Map<String, AttributeValue> newUpdatedContextRow = new HashMap<String, AttributeValue>();
        newUpdatedContextRow.put(defaultKeyAttribute, new AttributeValue().withS("goo"));
        newUpdatedContextRow.put(defaultValueAttribute, new AttributeValue().withS("foo"));
        newUpdatedContextRow.put(defaultContextAttribute, new AttributeValue().withS("environment=test"));
        newContextResultValues2.add(newUpdatedContextRow);

        newContextResultValues2.add(newGlobalRow1);

        Map<String, AttributeValue> invalidContextRow = new HashMap<String, AttributeValue>();
        invalidContextRow.put(defaultKeyAttribute, new AttributeValue().withS("goo"));
        invalidContextRow.put(defaultValueAttribute, new AttributeValue().withS("foo"));
        invalidContextRow.put(defaultContextAttribute, new AttributeValue().withS("environment"));
        newContextResultValues2.add(invalidContextRow);

        //Create results from initialized values
        newContextScanResult1 = new ScanResult().withItems(newContextResultValues1);
        newContextScanResult2 = new ScanResult().withItems(newContextResultValues2);
    }
}
