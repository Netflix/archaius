package com.netflix.archaius.persisted2;

import java.util.List;
import java.util.Map;

import com.netflix.archaius.annotations.Configuration;

@Configuration(prefix="archaius.persisted")
public interface Persisted2ClientConfig {
    int getRefreshRate();
    
    /**
     * @return Priority ordered list of scopes to be evaluated on the client
     */
    List<String> getPrioritizedScopes();
    
    /**
     * @return List of scopes to which this instance belongs
     */
    Map<String, String> getScopes();
    
    /**
     * @return List of query scopes to 'and' and possible values to 'or'
     */
    Map<String, List<String>> getQueryScopes();
    
    /**
     * When set to true the server will match only properties for which the list of 
     * scopes matches exactly the query scope.  Otherwise the server will match 
     * properties for which the query scopes is an subset.
     * @return
     */
    boolean getSkipPropsWithExtraScopes();
    
    /**
     * URL of persisted2 format service
     * @return
     */
    String getServiceUrl();
}
