/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.config.util;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * There are a few features in platform and allied libraries which need to parse
 * a property of the form &lt;uriRegex&gt; OR &lt;HTTP Verb
 * name&gt;&lt;space&gt;&lt;uriRegex&gt;. This class provides the functionality
 * to represent such a property value including static methods to parse such
 * property values.
 * 
 * @author pkamath
 * 
 */
public class HttpVerbUriRegexPropertyValue {

    private static final String METHOD_SEPARATOR = " ";
    private Verb verb = Verb.ANY_VERB;
    private String uriRegex = null;

    public HttpVerbUriRegexPropertyValue(Verb verb, String uriRegex) {
        this.verb = verb;
        this.uriRegex = uriRegex;
    }

    /**
     * Expects property value to be of the form &lt;uriRegex&gt; OR &lt;HTTP
     * Verb name&gt;&lt;space&gt;&lt;uriRegex&gt;
     * 
     * @param propValue
     *            property value
     * @return {@link HttpVerbUriRegexPropertyValue} instance corresponding to
     *         propValue if propValue is not null, null otherwise
     */
    public static HttpVerbUriRegexPropertyValue getVerbUriRegex(String propValue) {
        HttpVerbUriRegexPropertyValue returnValue = null;
        if (propValue != null) {
            propValue = propValue.trim();
            int methodSeparatorIndex = propValue.indexOf(METHOD_SEPARATOR);
            String uriRegex = propValue; // to begin with
            Verb verb = Verb.ANY_VERB;
            if (methodSeparatorIndex != -1) {
                // user may have supplied a verb
                verb = getVerb(propValue.substring(0, methodSeparatorIndex));
                if (verb != Verb.ANY_VERB) {
                    // update uriRegex
                    uriRegex = propValue.substring(methodSeparatorIndex + 1);
                }
            }
            returnValue = new HttpVerbUriRegexPropertyValue(verb, uriRegex);
        }
        return returnValue;
    }

    private static Verb getVerb(String verbName) {
        if (verbName != null) {
            Verb v = Verb.get(verbName.trim().toUpperCase());
            return v == null ? Verb.ANY_VERB : v;
        }
        return Verb.ANY_VERB;
    }

    public Verb getVerb() {
        return verb;
    }

    public String getUriRegex() {
        return uriRegex;
    }

    public enum Verb {
        GET("GET"), PUT("PUT"), POST("POST"), DELETE("DELETE"), OPTIONS(
                "OPTIONS"), HEAD("HEAD"), ANY_VERB("ANY"); // catch-all -
                                                           // essentially any
                                                           // verb!

        private final String verb; // http method
        private static final Map<String, Verb> lookup = new HashMap<String, Verb>();

        static {
            for (Verb v : EnumSet.allOf(Verb.class))
                lookup.put(v.name(), v);
        }

        private Verb(String verb) {
            this.verb = verb;
        }

        public String toString() {
            return verb;
        }

        public static Verb get(String verbName) {
            return lookup.get(verbName.toUpperCase());
        }
    }

    public static String getMethodSeparator() {
        return METHOD_SEPARATOR;
    }
}
