package com.netflix.archaius.persisted2;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.netflix.archaius.api.annotations.Configuration;

@Configuration(prefix="archaius.persisted")
public interface Persisted2ClientConfig {
    /**
     * @return True if the client is enabled.  This is checked only once at startup
     */
    boolean isEnabled();
    
    /**
     * @return Polling rate for getting updates
     */
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
    Map<String, Set<String>> getQueryScopes();
    
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

    /**
     * When set to true, the initial loading of persistent properties has to be successful. Otherwise,
     * the client will keep re-trying and Archaius initialization will not finish. In turn, the application's
     * initialization will not finish and application will not start up. This is going to be the default
     * behavior of persistent client.
     */
    boolean getSyncInit();
}
