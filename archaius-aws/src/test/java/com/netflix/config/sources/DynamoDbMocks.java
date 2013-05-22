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

    public static final Collection<Map<String, AttributeValue>> basicResultValues1 = new LinkedList<Map<String, AttributeValue>>();
    public static final Collection<Map<String, AttributeValue>> basicResultValues2 = new LinkedList<Map<String, AttributeValue>>();
    public static final ScanResult basicScanResult1;
    public static final ScanResult basicScanResult2;

    public static final Collection<Map<String, AttributeValue>> contextResultValues1 = new LinkedList<Map<String, AttributeValue>>();
    public static final Collection<Map<String, AttributeValue>> contextResultValues2 = new LinkedList<Map<String, AttributeValue>>();
    public static final ScanResult contextScanResult1;
    public static final ScanResult contextScanResult2;


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
    }
}
