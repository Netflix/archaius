package com.netflix.config.sources;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodb.AmazonDynamoDB;
import com.amazonaws.services.dynamodb.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodb.model.AttributeValue;
import com.amazonaws.services.dynamodb.model.Key;
import com.amazonaws.services.dynamodb.model.ScanRequest;
import com.amazonaws.services.dynamodb.model.ScanResult;
import com.netflix.config.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * User: gorzell
 * Date: 1/17/13
 * Time: 10:18 AM
 * You should write something useful here.
 */
public class DynamoDbDeploymentContextTableCache extends AbstractDynamoDbConfigurationSource<PropertyWithDeploymentContext> {
    private static Logger log = LoggerFactory.getLogger(AbstractPollingScheduler.class);

    //Property names
    static final String contextKeyAttributePropertyName = "com.netflix.config.dynamo.contextKeyAttributeName";
    static final String contextValueAttributePropertyName = "com.netflix.config.dynamo.contextValueAttributeName";

    //Property defaults
    static final String defaultContextKeyAttribute = "contextKey";
    static final String defaultContextValueAttribute = "contextValue";

    //Dynamic Properties
    private final DynamicStringProperty contextKeyAttributeName = DynamicPropertyFactory.getInstance()
            .getStringProperty(contextKeyAttributePropertyName, defaultContextKeyAttribute);
    private final DynamicStringProperty contextValueAttributeName = DynamicPropertyFactory.getInstance()
            .getStringProperty(contextValueAttributePropertyName, defaultContextValueAttribute);

    private final int initialDelayMillis;
    private final int delayMillis;

    private ScheduledExecutorService executor;
    private volatile Map<String, PropertyWithDeploymentContext> cachedTable = new HashMap<String, PropertyWithDeploymentContext>();

    public DynamoDbDeploymentContextTableCache() {
        super(new DefaultAWSCredentialsProviderChain().getCredentials());
        initialDelayMillis = 30000;
        delayMillis = 60000;
        start();
    }

    public DynamoDbDeploymentContextTableCache(AWSCredentials credentials, int initialDelayMillis, int delayMillis) {
        super(new AmazonDynamoDBClient(credentials));
        this.initialDelayMillis = initialDelayMillis;
        this.delayMillis = delayMillis;
        start();
    }

    public DynamoDbDeploymentContextTableCache(AmazonDynamoDB dbClient, int initialDelayMillis, int delayMillis) {
        super(dbClient);
        this.initialDelayMillis = initialDelayMillis;
        this.delayMillis = delayMillis;
        start();
    }

    private synchronized void schedule(Runnable runnable) {
        executor = Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "pollingConfigurationSource");
                t.setDaemon(true);
                return t;
            }
        });
        executor.scheduleWithFixedDelay(runnable, initialDelayMillis, delayMillis, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    private void start() {
        cachedTable = loadPropertiesFromTable(tableName.get());
        schedule(getPollingRunnable());
    }

    private Runnable getPollingRunnable() {
        return new Runnable() {
            public void run() {
                log.debug("Polling started");
                try {
                    Map<String, PropertyWithDeploymentContext> newMap = loadPropertiesFromTable(tableName.get());
                    cachedTable = newMap;
                } catch (Throwable e) {
                    log.error("Error getting result from polling source", e);
                    return;
                }
            }
        };
    }

    @Override
    protected Map<String, PropertyWithDeploymentContext> loadPropertiesFromTable(String table) {
        Map<String, PropertyWithDeploymentContext> propertyMap = new HashMap<String, PropertyWithDeploymentContext>();
        Key lastKeyEvaluated = null;
        do {
            ScanRequest scanRequest = new ScanRequest()
                    .withTableName(table)
                    .withExclusiveStartKey(lastKeyEvaluated);
            ScanResult result = dbClient.scan(scanRequest);
            for (Map<String, AttributeValue> item : result.getItems()) {
                String keyVal = item.get(keyAttributeName.get()).getS();
                String contextKey = item.get(contextKeyAttributeName.get()).getS();
                String contextVal = item.get(contextValueAttributeName.get()).getS();
                String key = keyVal + ";" + contextKey +";" + contextVal;
                propertyMap.put(key,
                        new PropertyWithDeploymentContext(
                                DeploymentContext.ContextKey.valueOf(contextKey),
                                contextVal,
                                keyVal,
                                item.get(valueAttributeName.get()).getS()
                        ));
            }
            lastKeyEvaluated = result.getLastEvaluatedKey();
        } while (lastKeyEvaluated != null);
        return propertyMap;
    }

    public Collection<PropertyWithDeploymentContext> getProperties(){
        return cachedTable.values();
    }
}
