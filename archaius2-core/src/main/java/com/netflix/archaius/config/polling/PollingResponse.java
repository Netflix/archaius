package com.netflix.archaius.config.polling;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public abstract class PollingResponse {
    public static PollingResponse forSnapshot(final Map<String, Object> values) {
        return new PollingResponse() {
            @Override
            public Map<String, Object> getToAdd() {
                return values;
            }

            @Override
            public Collection<String> getToRemove() {
                return Collections.emptyList();
            }

            @Override
            public boolean hasData() {
                return true;
            }
            
        };
    }
    
    public static PollingResponse noop() {
        return new PollingResponse() {
            @Override
            public Map<String, Object> getToAdd() {
                return Collections.emptyMap();
            }

            @Override
            public Collection<String> getToRemove() {
                return Collections.emptyList();
            }

            @Override
            public boolean hasData() {
                return false;
            }
        };
    }
    
    public abstract Map<String, Object> getToAdd();
    public abstract Collection<String> getToRemove();
    public abstract boolean hasData(); 
}
