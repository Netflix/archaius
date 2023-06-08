package com.netflix.archaius.persisted2;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final static String DEFAULT_ID_FIELD = "propertyId";
    private final static List<String>   DEFAULT_PATH  = Arrays.asList("persistedproperties", "properties", "property");
            
    public static class Builder {
        private final Callable<InputStream> reader;
        private List<String> path        = DEFAULT_PATH;
        private List<String> scopeFields = DEFAULT_ORDERED_SCOPES;
        private String       keyField    = DEFAULT_KEY_FIELD;
        private String       valueField  = DEFAULT_VALUE_FIELD;
        private String       idField = DEFAULT_ID_FIELD;
        private ScopePredicate predicate = ScopePredicates.alwaysTrue();
        private ScopedValueResolver resolver = new ScopePriorityPropertyValueResolver();
        private boolean readIdField = false;
                
        public Builder(Callable<InputStream> reader) {
            this.reader = reader;
        }
        
        public Builder withPath(String path) {
            return withPath(Arrays.asList(StringUtils.split(path, "/")));
        }
        
        public Builder withPath(List<String> path) {
            List<String> copy = new ArrayList<String>();
            copy.addAll(path);
            this.path = Collections.unmodifiableList(copy);
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

        public Builder withIdField(String idField) {
            this.idField = idField;
            return this;
        }
        
        public Builder withValueResolver(ScopedValueResolver resolver) {
            this.resolver = resolver;
            return this;
        }

        public Builder withReadIdField(boolean readIdField) {
            this.readIdField = readIdField;
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
    private final ObjectMapper            mapper         = new ObjectMapper();
    private final List<String>            scopeFields;
    private final String                  keyField;
    private final String                  idField;
    private final String                  valueField;
    private final List<String>            path;
    private final boolean       readIdField;

    private JsonPersistedV2Reader(Builder builder) {
        this.reader        = builder.reader;
        this.predicate     = builder.predicate;
        this.valueResolver = builder.resolver;
        this.keyField      = builder.keyField;
        this.valueField    = builder.valueField;
        this.idField       = builder.idField;
        this.scopeFields   = builder.scopeFields;
        this.path          = builder.path;
        this.readIdField   = builder.readIdField;
    }
    
    @Override
    public PollingResponse call() throws Exception {
        Map<String, List<ScopedValue>> props = new HashMap<String, List<ScopedValue>>();
        Map<String, List<ScopedValue>> propIds = new HashMap<>();
        
        InputStream is = reader.call();
        if (is == null) {
            return PollingResponse.noop();
        }
        
        try {
            JsonNode node = mapper.readTree(is);
            for (String part : this.path) {
                node = node.path(part);
            }
            
            for (final JsonNode property : node)  {
                String key = null;
                try {
                    key   = property.get(keyField).asText();
                    String value = property.has(valueField) ? property.get(valueField).asText() : "";
                    
                    LinkedHashMap<String, Set<String>> scopes = new LinkedHashMap<String, Set<String>>();
                    
                    for (String scope : this.scopeFields) {
                        String[] values = StringUtils.splitByWholeSeparator(property.has(scope) ? property.get(scope).asText().toLowerCase() : "", ",");
                        scopes.put(scope, values.length == 0 ? Collections.<String>emptySet() : immutableSetFrom(values));
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
                    if (readIdField) {
                        propIds.putIfAbsent(key, new ArrayList<>());
                        propIds.get(key).add(
                                new ScopedValue(property.has(idField) ? property.get(idField).asText() : "", scopes));
                    }
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

        if (readIdField) {
            final Map<String, String> idResult = new HashMap<>();
            for (Entry<String, List<ScopedValue>> entry : propIds.entrySet()) {
                idResult.put(entry.getKey(), valueResolver.resolve(entry.getKey(), entry.getValue()));
            }
            return PollingResponse.forSnapshot(result, idResult);
        }
        
        return PollingResponse.forSnapshot(result);
    }
    
    private static Set<String> immutableSetFrom(String[] values) {
        if (values.length == 0) {
            return Collections.<String>emptySet();
        }
        else {
            HashSet<String> set = new HashSet<String>();
            set.addAll(Arrays.asList(values));
            return Collections.unmodifiableSet(set);
        }
    }

}
