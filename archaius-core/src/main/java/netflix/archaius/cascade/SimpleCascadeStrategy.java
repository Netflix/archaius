package netflix.archaius.cascade;

import java.util.ArrayList;
import java.util.List;

import netflix.archaius.CascadeStrategy;
import netflix.archaius.StrInterpolator;

/**
 * Default 'noop' cascade strategy returns only the original resource name
 * 
 * @author elandau
 *
 */
public class SimpleCascadeStrategy implements CascadeStrategy {
    @Override
    public List<String> generate(String name, StrInterpolator interpolator) {
        List<String> list = new ArrayList<String>();
        list.add(interpolator.resolve(name).toString());
        return list;
    }
}
