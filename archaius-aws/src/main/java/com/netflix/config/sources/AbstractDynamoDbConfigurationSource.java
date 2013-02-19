package com.netflix.config.sources;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.DynamicStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * User: gorzell
 * Date: 1/17/13
 * Time: 10:18 AM
 * Some of the basic plumbing for a polling source that talks to Dynamo.
 */
public abstract class AbstractDynamoDbConfigurationSource <T> {
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
    protected DynamicStringProperty tableName = DynamicPropertyFactory.getInstance()
            .getStringProperty(tablePropertyName, defaultTable);
    protected DynamicStringProperty keyAttributeName = DynamicPropertyFactory.getInstance()
            .getStringProperty(keyAttributePropertyName, defaultKeyAttribute);
    protected DynamicStringProperty valueAttributeName = DynamicPropertyFactory.getInstance()
            .getStringProperty(valueAttributePropertyName, defaultValueAttribute);


    protected AmazonDynamoDB dbClient;

    public AbstractDynamoDbConfigurationSource() {
        this(new DefaultAWSCredentialsProviderChain().getCredentials());
    }

    public AbstractDynamoDbConfigurationSource(AWSCredentials credentials) {
        this(new AmazonDynamoDBClient(credentials));
    }

    public AbstractDynamoDbConfigurationSource(AmazonDynamoDB dbClient) {
        this.dbClient = dbClient;
    }

    protected abstract Map<String, T> loadPropertiesFromTable(String table);

    //TODO Javadoc
    public void validateDb(){
        String table = tableName.get();

        loadPropertiesFromTable(table);
        log.info("Successfully polled Dynamo for a new configuration based on table:" + table);
    }
}