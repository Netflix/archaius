package netflix.archaius.loaders;

import java.util.Iterator;

import netflix.archaius.Config;
import netflix.archaius.exceptions.ConfigException;

import org.junit.Test;

public class PropertyConfigReaderTest {
    @Test
    public void readerTest() throws ConfigException{
        PropertiesConfigReader reader = new PropertiesConfigReader();
        Config config = reader.load("apps", "application");
        Iterator<String> iter = config.getKeys();
        while (iter.hasNext()) {
            String key = iter.next();
            System.out.println("Key : " + key + " " + config.getRawProperty(key));
        }
        
        System.out.println(config.getList("application.list"));
        System.out.println(config.getList("application.list2"));
        System.out.println(config.getList("application.map"));
        System.out.println(config.getList("application.set"));
        
        System.out.println(config.getBoolean("application.list"));
        System.out.println(config.getInteger("application.list"));
    }
}
