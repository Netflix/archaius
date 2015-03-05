/*
 * Copyright 2014 Netflix, Inc.
 *
 * Copyright 2013 Netflix, Inc.
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
package com.netflix.config;

import java.util.Set;

public class DynamicStringSetProperty extends DynamicSetProperty<String> {
    public DynamicStringSetProperty(String propName, String defaultValue) {
        super(propName, defaultValue);
    }

    public DynamicStringSetProperty(String propName, Set<String> defaultValue) {
        super(propName, defaultValue);
    }

    public DynamicStringSetProperty(String propName, String defaultValue, String listDelimiterRegex) {
        super(propName, defaultValue, listDelimiterRegex);
    }

    public DynamicStringSetProperty(String propName, Set<String> defaultValue, String listDelimiterRegex) {
        super(propName, defaultValue, listDelimiterRegex);
    }

    @Override
    protected String from(String value) {
        return value;
    }
}
