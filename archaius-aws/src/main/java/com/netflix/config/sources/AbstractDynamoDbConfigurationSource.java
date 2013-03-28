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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughputExceededException;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.netflix.config.DynamicLongProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * User: gorzell
 * Date: 1/17/13
 * Time: 10:18 AM
 * Some of the basic plumbing and properties for a polling source that talks to Dynamo.
 */
public abstract class AbstractDynamoDbConfigurationSource <T> {
    private static final Logger log = LoggerFactory.getLogger(AbstractDynamoDbConfigurationSource.class);

    //Property names
    static final String tablePropertyName = "com.netflix.config.dynamo.tableName";
    static final String keyAttributePropertyName = "com.netflix.config.dynamo.keyAttributeName";
    static final String valueAttributePropertyName = "com.netflix.config.dynamo.valueAttributeName";
    static final String endpointPropertyName = "com.netflix.config.dynamo.endpoint";
    static final String pollingMaxBackOff = "com.netflix.config.dynamo.maxPollingBackOff";
    static final String pollingMinBackOff = "com.netflix.config.dynamo.maxPollingBackOff";

    //Property defaults
    static final String defaultTable = "archaiusProperties";
    static final String defaultKeyAttribute = "key";
    static final String defaultValueAttribute = "value";
    static final String defaultEndpoint = "dynamodb.us-east-1.amazonaws.com";
    static final Long defaultMaxBackOff = 5 * 1000L;
    static final Long defaultMinBackOff = 500L;

    //Dynamic Properties
    protected DynamicStringProperty tableName = DynamicPropertyFactory.getInstance()
            .getStringProperty(tablePropertyName, defaultTable);
    protected DynamicStringProperty keyAttributeName = DynamicPropertyFactory.getInstance()
            .getStringProperty(keyAttributePropertyName, defaultKeyAttribute);
    protected DynamicStringProperty valueAttributeName = DynamicPropertyFactory.getInstance()
            .getStringProperty(valueAttributePropertyName, defaultValueAttribute);
    protected DynamicStringProperty endpointName = DynamicPropertyFactory.getInstance()
            .getStringProperty(endpointPropertyName, defaultEndpoint);
    protected DynamicLongProperty maxBackOff = DynamicPropertyFactory.getInstance()
            .getLongProperty(pollingMaxBackOff, defaultMaxBackOff);
    protected DynamicLongProperty minBackOff = DynamicPropertyFactory.getInstance()
            .getLongProperty(pollingMinBackOff, defaultMinBackOff);

    protected AmazonDynamoDB dbClient;

    public AbstractDynamoDbConfigurationSource() {
        this(new AmazonDynamoDBClient());
        setEndpoint();
    }

    public AbstractDynamoDbConfigurationSource(ClientConfiguration clientConfiguration) {
        this(new AmazonDynamoDBClient(clientConfiguration));
        setEndpoint();
    }

    public AbstractDynamoDbConfigurationSource(AWSCredentials credentials) {
        this(new AmazonDynamoDBClient(credentials));
        setEndpoint();
    }

    public AbstractDynamoDbConfigurationSource(AWSCredentials credentials, ClientConfiguration clientConfiguration) {
        this(new AmazonDynamoDBClient(credentials, clientConfiguration));
        setEndpoint();
    }

    public AbstractDynamoDbConfigurationSource(AWSCredentialsProvider credentialsProvider) {
        this(new AmazonDynamoDBClient(credentialsProvider));
        setEndpoint();
    }

    public AbstractDynamoDbConfigurationSource(AWSCredentialsProvider credentialsProvider, ClientConfiguration clientConfiguration) {
        this(new AmazonDynamoDBClient(credentialsProvider, clientConfiguration));
        setEndpoint();
    }

    public AbstractDynamoDbConfigurationSource(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
    }


    protected ScanResult dbScanWithThroughputBackOff(ScanRequest scanRequest) {
        Long currentBackOff = minBackOff.get();
        while(true) {
            try {
                return dbClient.scan(scanRequest);
            }
            catch (ProvisionedThroughputExceededException e) {
                currentBackOff = Math.min(currentBackOff * 2, maxBackOff.get());
                log.info("Failed to poll Dynamo due to ProvisionedThroughputExceededException. Backing off.");

                try {
                    Thread.sleep(currentBackOff);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

    protected abstract Map<String, T> loadPropertiesFromTable(String table);

    //TODO Javadoc
    public void validateDb(){
        String table = tableName.get();

        loadPropertiesFromTable(table);
        log.info("Successfully polled Dynamo for a new configuration based on table:" + table);
    }

    private void setEndpoint() {
        String endpoint = endpointName.get();
        dbClient.setEndpoint(endpoint);
        log.info("Set Dynamo endpoint:" + endpoint);
    }
}
