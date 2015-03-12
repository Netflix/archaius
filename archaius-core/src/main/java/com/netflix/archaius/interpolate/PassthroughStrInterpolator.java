package com.netflix.archaius.interpolate;

import com.netflix.archaius.StrInterpolator;

public class PassthroughStrInterpolator implements StrInterpolator {
    @Override
    public String resolve(String key) {
        return key;
    }
}
