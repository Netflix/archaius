package com.netflix.archaius.persisted2.loader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.zip.GZIPInputStream;

public class HTTPStreamLoader implements Callable<InputStream> {

    private String lastEtag;
    private final URL url;
    private static final int BUFFER_SIZE = 8192;
    
    public HTTPStreamLoader(URL url) {
        this.url = url;
    }
    
    @Override
    public InputStream call() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        try {
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Accept-Encoding", "gzip");
            if (lastEtag != null) {
                conn.setRequestProperty("If-None-Match", lastEtag);
            }

            conn.connect();

            // force a connection to test if the URL is reachable
            final int status = conn.getResponseCode();
            if (status == 200) {
                lastEtag = conn.getHeaderField("ETag");

                InputStream input = conn.getInputStream();
                if ("gzip".equals(conn.getContentEncoding())) {
                    input = new GZIPInputStream(input);
                }

                try (InputStream in = input) { // input gets closed automatically
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int n = in.read(buffer);
                    while (n != -1) {
                        out.write(buffer, 0, n);
                        n = in.read(buffer);
                    }
                    return new ByteArrayInputStream(out.toByteArray());
                }
            }
            else if (status == 304) {
                // It is expected the reader will treat this as a noop response
                return null;
            }
            else {
                throw new RuntimeException("Failed to read input " + conn.getResponseCode());
            }
        }
        finally {
            conn.disconnect();
        }
    }
}
