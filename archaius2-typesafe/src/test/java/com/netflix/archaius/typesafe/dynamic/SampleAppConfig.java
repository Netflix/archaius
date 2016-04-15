package com.netflix.archaius.typesafe.dynamic;

import com.netflix.archaius.api.annotations.Configuration;
import com.typesafe.config.ConfigMemorySize;

import java.time.Duration;
import java.util.List;

@Configuration(prefix = "app", allowFields = true)
public class SampleAppConfig {

    public String name;
    public Boolean flag;
    public Integer number;
    public List<Integer> list;
    public Duration duration;
    public ConfigMemorySize size;
}
