package netflix.archaius.property;

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
