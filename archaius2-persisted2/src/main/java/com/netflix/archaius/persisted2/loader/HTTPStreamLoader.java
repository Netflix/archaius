package com.netflix.archaius.persisted2.loader;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;

public class HTTPStreamLoader implements Callable<InputStream> {

    private String lastEtag;
    private final URL url;
    
    public HTTPStreamLoader(URL url) {
        this.url = url;
    }
    
    @Override
    public InputStream call() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Accept",          "application/json");
        conn.setRequestProperty("Accept-Encoding", "gzip,deflate");
        if (lastEtag != null) {
            conn.setRequestProperty("If-None-Match", lastEtag);
        }
        
        conn.connect();
        
        // force a connection to test if the URL is reachable
        if (conn.getResponseCode() == 200) {
            lastEtag = conn.getHeaderField("ETag");
            
            InputStream input = conn.getInputStream();
            try {
                if ("gzip".equals(conn.getContentEncoding())) {
                    input = new GZIPInputStream(input);
                }
                return input;
            }
            finally {
                try {
                    input.close();
                }
                catch (Exception e) {
                    // OK to ignore
                }
            }
        
        }
        else {
            throw new RuntimeException("Failed to read input " + conn.getResponseCode());
        }
    }
}
