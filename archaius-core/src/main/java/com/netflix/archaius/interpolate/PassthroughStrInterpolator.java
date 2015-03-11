package com.netflix.archaius.interpolate;

import com.netflix.archaius.StrInterpolator;

public class PassthroughStrInterpolator implements StrInterpolator {
    @Override
    public Object resolve(String key) {
        return key;
    }
}
