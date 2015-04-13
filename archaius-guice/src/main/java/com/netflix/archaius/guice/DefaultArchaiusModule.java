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
package com.netflix.archaius.guice;

import com.google.inject.Provides;
import com.netflix.archaius.AppConfig;
import com.netflix.archaius.DefaultAppConfig;
import com.netflix.archaius.mapper.ConfigMapper;
import com.netflix.archaius.mapper.DefaultConfigMapper;

import javax.inject.Singleton;

/**
 * Standalone module that gets archaius ready to use with the default bindings for
 * {@link ConfigMapper} and {@link AppConfig}.
 */
public class DefaultArchaiusModule extends ArchaiusModule {

    @Provides
    @Singleton
    protected ConfigMapper createConfigMapper() {
        return new DefaultConfigMapper();
    }

    @Provides
    @Singleton
    protected AppConfig createAppConfig() {
        return DefaultAppConfig.builder().build();
    }
}
