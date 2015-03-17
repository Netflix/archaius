package com.netflix.archaius.persisted2;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.archaius.config.polling.PollingResponse;

/**
 * Reader for Netflix persisted properties (not yet available in OSS).  
 * 
 * Properties are read as a single JSON blob that contains a full list of properties.
 * Multiple property values may exist for different scopes and are resolved into 
 * a single property value using a ProperyValueResolver.
 * 
 * Example,
 * 
 * <pre>
 * {@code 
 *  JsonPersistedV2Reader reader =
 *      JsonPersistedV2Reader.builder(new HTTPStreamLoader(url))     // url from which config is fetched
 *          .withPredicate(ScopePredicates.fromMap(instanceScopes))  // Map of scope values for the running instance
 *          .build();
 *          
 *  appConfig.addConfigFirst(new PollingDynamicConfig("dyn", reader, new FixedPollingStrategy(30, TimeUnit.SECONDS)));
 *
 * }
 * </pre>
 * 
 * @author elandau
 *
 */
public class JsonPersistedV2Reader implements Callable<PollingResponse> {
    private static final Logger LOG = LoggerFactory.getLogger(JsonPersistedV2Reader.class);
    
    private final static List<String> DEFAULT_ORDERED_SCOPES = Arrays.asList("serverId", "asg", "ami", "cluster", "appId", "env", "countries", "stack", "zone", "region");
    private final static String DEFAULT_KEY_FIELD   = "key";
    private final static String DEFAULT_VALUE_FIELD = "value";
    private final static String DEFAULT_PATH        = "persistedproperties/properties/property";
            
    public static class Builder {
        private final Callable<InputStream> reader;
        private String       path        = DEFAULT_PATH;
        private List<String> scopeFields = DEFAULT_ORDERED_SCOPES;
        private String       keyField    = DEFAULT_KEY_FIELD;
        private String       valueField  = DEFAULT_VALUE_FIELD;
        private ScopePredicate predicate = ScopePredicates.alwaysTrue();
        private ScopedValueResolver resolver = new ScopePriorityPropertyValueResolver();
                
        public Builder(Callable<InputStream> reader) {
            this.reader = reader;
        }
        
        public Builder withPath(String path) {
            this.path = path;
            return this;
        }
        
        public Builder withScopes(List<String> scopes) {
            this.scopeFields = scopes;
            return this;
        }

        public Builder withPredicate(ScopePredicate predicate) {
            this.predicate = predicate;
            return this;
        }
        
        public Builder withKeyField(String keyField) {
            this.keyField = keyField;
            return this;
        }
        
        public Builder withValueField(String valueField) {
            this.valueField = valueField;
            return this;
        }
        
        public Builder withValueResolver(ScopedValueResolver resolver) {
            this.resolver = resolver;
            return this;
        }
        
        public JsonPersistedV2Reader build() {
            return new JsonPersistedV2Reader(this);
        }
        
    }
    
    public static Builder builder(Callable<InputStream> reader) {
        return new Builder(reader);
    }
    
    private final Callable<InputStream>   reader;
    private final ScopePredicate          predicate;
    private final ScopedValueResolver     valueResolver;
    private final ObjectMapper            mapper      = new ObjectMapper();
    private final List<String>            scopeFields;
    private final String                  keyField;
    private final String                  valueField;
    private final String                  path;

    private JsonPersistedV2Reader(Builder builder) {
        this.reader        = builder.reader;
        this.predicate     = builder.predicate;
        this.valueResolver = builder.resolver;
        this.keyField      = builder.keyField;
        this.valueField    = builder.valueField;
        this.scopeFields   = builder.scopeFields;
        this.path          = builder.path;
    }
    
    @Override
    public PollingResponse call() throws Exception {
        Map<String, List<ScopedValue>> props = new HashMap<String, List<ScopedValue>>();
        
        InputStream is = reader.call();
        if (is == null) {
            return PollingResponse.noop();
        }
        
        try {
            for (final JsonNode property : mapper.readTree(is).path(path))  {
                String key = null;
                try {
                    key   = property.get(keyField).asText();
                    String value = property.has(valueField) ? property.get(valueField).asText() : "";
                    
                    LinkedHashMap<String, String> scopes = new LinkedHashMap<String, String>();
                    
                    for (String scope : this.scopeFields) {
                        scopes.put(scope, property.has(scope) ? property.get(scope).asText() : "");
                    }
                    
                    // Filter out scopes that don't match at all
                    if (!this.predicate.evaluate(scopes)) {
                        continue;
                    }
                    
                    // Build up a list of valid scopes
                    List<ScopedValue> variations = props.get(key);
                    if (variations == null) {
                        variations = new ArrayList<ScopedValue>();
                        props.put(key, variations);
                    }
                    variations.add(new ScopedValue(value, scopes));
                }
                catch (Exception e) {
                    LOG.warn("Unable to process property '{}'", key);
                }
            }
        }
        finally {
            try {
                is.close();
            }
            catch (Exception e) {
                // OK to ignore
            }
        }
        
        // Resolve to a single property value
        final Map<String, String> result = new HashMap<String, String>();
        for (Entry<String, List<ScopedValue>> entry : props.entrySet()) {
            result.put(entry.getKey(), valueResolver.resolve(entry.getKey(), entry.getValue()));
        }
        
        return PollingResponse.forSnapshot(result);
    }

}
