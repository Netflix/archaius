package com.netflix.archaius.cascade;

import java.util.ArrayList;
import java.util.List;

import com.netflix.archaius.api.CascadeStrategy;
import com.netflix.archaius.api.StrInterpolator;
import com.netflix.archaius.api.StrInterpolator.Lookup;

public abstract class InterpolatingCascadeStrategy implements CascadeStrategy {
    @Override
    public final List<String> generate(String resource, StrInterpolator interpolator, Lookup lookup) {
        List<String> result = new ArrayList<>();
        for (String option : getPermutations()) {
            result.add(interpolator.create(lookup).resolve(String.format(option, resource)));
        }
        return result;
    }

    protected abstract List<String> getPermutations();
}
