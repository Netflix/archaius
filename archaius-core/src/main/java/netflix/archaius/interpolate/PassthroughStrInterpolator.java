package netflix.archaius.interpolate;

import netflix.archaius.StrInterpolator;

public class PassthroughStrInterpolator implements StrInterpolator {
    @Override
    public Object resolve(String key) {
        return key;
    }
}
