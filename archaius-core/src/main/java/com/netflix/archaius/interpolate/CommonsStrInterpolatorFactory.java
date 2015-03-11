package com.netflix.archaius.interpolate;

import org.apache.commons.lang3.text.StrLookup;
import org.apache.commons.lang3.text.StrSubstitutor;

import com.netflix.archaius.Config;
import com.netflix.archaius.StrInterpolator;
import com.netflix.archaius.StrInterpolatorFactory;

public class CommonsStrInterpolatorFactory implements StrInterpolatorFactory {
    
    public static CommonsStrInterpolatorFactory INSTANCE = new CommonsStrInterpolatorFactory();
    
    @Override
    public StrInterpolator create(final Config rootConfig) {
        return new InternalInterpolator(rootConfig);
    }
    
    class InternalInterpolator extends StrLookup implements StrInterpolator {
        private final Config config;
        private final StrSubstitutor sub;
        
        public InternalInterpolator(Config config) {
            this.config = config;
            this.sub = new StrSubstitutor(this, "${", "}", '$');
        }
        
        @Override
        public Object resolve(String key) {
            Object obj = sub.replace(key);
            return (obj == null) ? key : obj;
        }

        @Override
        public String lookup(String key) {
            Object obj = config.getRawProperty(key);
            return (obj == null) ? key : obj.toString();
        }
    }
}
