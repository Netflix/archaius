package netflix.archaius;

import java.io.File;
import java.net.URL;

/**
 * Contract for a configuration file loader.  The loader 
 * 
 * @author elandau
 *
 */
public interface ConfigLoader {
    Config load(String name);
    
    Config load(URL name);
    
    Config load(File file);
    
    boolean canLoad(String name);
    
    boolean canLoad(URL uri);
    
    boolean canLoad(File file);
}
