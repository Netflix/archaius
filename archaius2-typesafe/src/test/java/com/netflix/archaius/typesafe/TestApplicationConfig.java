package com.netflix.archaius.typesafe;

import com.netflix.archaius.api.annotations.PropertyName;

import java.util.List;
import java.util.Map;

/**
 * Created by Milan Baran on 5/26/17.
 */
public interface TestApplicationConfig {

    @PropertyName(name = "module_with_some_plugins")
    List<String> getModuleWithSomePlugins();

    @PropertyName(name = "module_with_no_plugins")
    List<String> getModuleWithNoPlugins();

    @PropertyName(name = "module_with_non_existing_plugins")
    List<String> getModuleWithNonExistingPlugins();

    @PropertyName(name = "module_with_some_matrix")
    Map<String,List<String>> getModuleWithSomePluginsMatrix();

    @PropertyName(name = "module_with_no_matrix")
    Map<String,List<String>> getModuleWithNoPluginsMatrix();

    @PropertyName(name = "module_with_sub_config")
    SubConfig getModuleWithSubConfig();
}