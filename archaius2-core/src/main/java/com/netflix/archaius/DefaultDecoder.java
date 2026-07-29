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
package com.netflix.archaius;

import com.netflix.archaius.api.TypeConverter;
import com.netflix.archaius.converters.ArrayTypeConverterFactory;
import com.netflix.archaius.converters.DefaultCollectionsTypeConverterFactory;
import com.netflix.archaius.converters.DefaultTypeConverterFactory;
import com.netflix.archaius.converters.EnumTypeConverterFactory;

import jakarta.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Singleton
public class DefaultDecoder extends AbstractRegistryDecoder {
    public static final List<TypeConverter.Factory> DEFAULT_FACTORIES = Collections.unmodifiableList(Arrays.asList(
            DefaultTypeConverterFactory.INSTANCE,
            DefaultCollectionsTypeConverterFactory.INSTANCE,
            ArrayTypeConverterFactory.INSTANCE,
            EnumTypeConverterFactory.INSTANCE));
    public static final DefaultDecoder INSTANCE = new DefaultDecoder();

    private DefaultDecoder() {
        super(DEFAULT_FACTORIES);
    }
}
