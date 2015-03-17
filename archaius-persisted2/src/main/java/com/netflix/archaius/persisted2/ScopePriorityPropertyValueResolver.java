package com.netflix.archaius.persisted2;

import java.util.Iterator;
import java.util.List;

/**
 * PropertyValueResolver that picks the first PropervyValue that has a higher 
 * scope value.  
 * 
 * Each PropertyValue is assumed to have the same number of scopes and scopes are 
 * assumed to be in the same index location for all PropervyValue instances.
 * A PropertyValue should not contain different scope values since those would
 * have been filtered out by the predicate (see JsonPersistedV2Poller) and all properties
 * for an instance can only match one scope value.  Any deviation from the above
 * assumptions will result either in NoSuchElementException or undefined behavior.
 * 
 * For example,
 * 
 *    value1 :  cluster="", app="foo"
 *    value2 :  cluster="foo-1", app="foo"
 * 
 * The resolver will choose value2 since cluster is a higher scope and value2 has 
 * a value for it.
 * 
 * @author elandau
 */
public class ScopePriorityPropertyValueResolver implements ScopedValueResolver  {
    @Override
    public String resolve(String propName, List<ScopedValue> variations) {
        Iterator<ScopedValue> iter = variations.iterator();
        ScopedValue p1 = iter.next();
        while (iter.hasNext()) {
            ScopedValue p2 = iter.next();
            
            Iterator<String> s1 = p1.getScopes().values().iterator();
            Iterator<String> s2 = p2.getScopes().values().iterator();
            
            while (s1.hasNext()) {
                String v1 = s1.next();
                String v2 = s2.next();
                if (v1.isEmpty() && !v2.isEmpty()) {
                    p1 = p2;
                    break;
                }
                else if (!v1.isEmpty() && v2.isEmpty()) {
                    break;
                }
            }
        }
                
        return p1.getValue();
    }
}
