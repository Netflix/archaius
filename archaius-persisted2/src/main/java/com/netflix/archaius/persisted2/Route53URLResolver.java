package com.netflix.archaius.persisted2;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

public class Route53URLResolver implements Callable<URL> {

    private final String url;
    
    public Route53URLResolver(String region, String env, LinkedHashMap<String, String> scopes) {
        this("platformservice", region, env, "netflix.net", 7001, "/platformservice/REST/v1/props/allprops", scopes);
    }
    
    public Route53URLResolver(String serviceName, String region, String env, String domain, int port, String path, LinkedHashMap<String, String> scopes) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("http://%s.%s.dyn%s.%s:%d/%s", serviceName, region, env, domain, port, path))
          .append("?");
        
        for (Entry<String, String> scope : scopes.entrySet()) {
            sb.append(scope.getKey()).append("=").append(scope.getValue()).append("&");
        }
        
        this.url = sb.toString();
    }
    
    @Override
    public URL call() throws MalformedURLException {
        return new URL(url);
    }
}
