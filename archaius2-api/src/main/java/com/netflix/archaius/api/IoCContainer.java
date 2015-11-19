/**
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.archaius.api;

/**
 * Interface used by ConfigBinder to integrate with a DI framework that 
 * allows for named injection.  This integration enables binding a string
 * value for a type to a DI bound instance.
 * 
 * @author elandau
 *
 */
public interface IoCContainer {
    /**
     * @param name
     * @param type
     * @return Return the instance for type T bound to 'name'
     */
    public <T> T getInstance(String name, Class<T> type);
}
