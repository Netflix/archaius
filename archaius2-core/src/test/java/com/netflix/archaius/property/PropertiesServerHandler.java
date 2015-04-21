/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.archaius.property;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * TestHandler to be used with TestHttpServer to simulate server responses
 * for property file polling
 * 
 * @author elandau
 *
 */
public class PropertiesServerHandler implements HttpHandler {

    private volatile Properties props = new Properties();
    private volatile int responseCode = 200;
    
    @Override
    public void handle(HttpExchange t) throws IOException {
        if (responseCode == 200) {
            // Output .properties file format
            ByteArrayOutputStream strm = new ByteArrayOutputStream();
            props.store(strm, null);
            
            // Send response
            OutputStream os = t.getResponseBody();
            t.sendResponseHeaders(200, strm.size());
            os.write(strm.toByteArray());
            os.close();
        }
        else {
            t.sendResponseHeaders(responseCode, 0);
        }
    }
    
    public void setProperties(Properties props) {
        this.props = props;
    }
    
    public Properties getProperties() {
        return this.props;
    }

    public void setResponseCode(int code) {
        this.responseCode = code;
    }
    
    public void clear() {
        this.props.clear();
    }
    
    public void remove(String key) {
        this.props.remove(key);
    }
    
    public <T> void setProperty(String key, T value) {
        this.props.setProperty(key, value.toString());
    }

    public boolean isEmpty() {
        return this.props.isEmpty();
    }
}
