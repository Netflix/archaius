package com.netflix.archaius.config.polling;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public abstract class PollingResponse {

    public static PollingResponse forSnapshot(final Map<String, String> values, final Map<String, String> ids) {
        return new PollingResponse() {
            @Override
            public Map<String, String> getToAdd() {
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

            @Override
            public Map<String, String> getNameToIdsMap() {
                return ids;
            }
        };
    }

    public static PollingResponse forSnapshot(final Map<String, String> values) {
        return new PollingResponse() {
            @Override
            public Map<String, String> getToAdd() {
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
            public Map<String, String> getToAdd() {
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
    
    public abstract Map<String, String> getToAdd();
    public abstract Collection<String> getToRemove();
    public abstract boolean hasData();
    public Map<String, String> getNameToIdsMap() {
        return Collections.emptyMap();
    }
}
