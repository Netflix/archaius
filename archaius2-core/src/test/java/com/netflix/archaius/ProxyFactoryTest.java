package com.netflix.archaius;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.annotations.PropertyName;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.config.DefaultSettableConfig;
import com.netflix.archaius.config.EmptyConfig;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import javax.annotation.Nullable;

public class ProxyFactoryTest {
    public static enum TestEnum {
        NONE,
        A, 
        B,
        C
    }
    
    @Configuration(immutable=true)
    public static interface ImmutableConfig {
        @DefaultValue("default")
        String getValueWithDefault();
        
        String getValueWithoutDefault1();
        
        String getValueWithoutDefault2();
    }
    
    public static interface BaseConfig {
        @DefaultValue("basedefault")
        String getStr();
        
        Boolean getBaseBoolean();
        
        @Nullable
        Integer getNullable();
    }
    
    public static interface RootConfig extends BaseConfig {
        @DefaultValue("default")
        @Override
        String getStr();
        
        @DefaultValue("0")
        int getInteger();
        
        @DefaultValue("NONE")
        TestEnum getEnum();
        
        SubConfig getSubConfig();
        
        @DefaultValue("default1:default2")
        SubConfigFromString getSubConfigFromString();
        
        int getRequiredValue();
    }
    
    public static interface SubConfig {
        @DefaultValue("default")
        String str();
    }
    
    public static class SubConfigFromString {
        private String[] parts;

        public SubConfigFromString(String value) {
            this.parts = value.split(":");
        }
        
        public String part1() {
            return parts[0];
        }
        
        public String part2() {
            return parts[1];
        }
        
        @Override
        public String toString() {
            return "SubConfigFromString[" + part1() + ", " + part2() + "]";
        }
    }
    
    @Test
    public void testImmutableDefaultValues() {
        SettableConfig config = new DefaultSettableConfig();
        config.setProperty("valueWithoutDefault2", "default2");
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(new NoLibrariesConfig(), config, config.getDecoder(), factory);
        ImmutableConfig c = proxy.newProxy(ImmutableConfig.class);
        
        assertThat(c.getValueWithDefault(), equalTo("default"));
        assertThat(c.getValueWithoutDefault2(), equalTo("default2"));
        assertThat(c.getValueWithoutDefault1(), nullValue());
        
        config.setProperty("valueWithDefault", "newValue");
        assertThat(c.getValueWithDefault(), equalTo("default"));
    }
    
    @Test
    public void testDefaultValues() {
        Config config = EmptyConfig.INSTANCE;

        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(new NoLibrariesConfig(), config, config.getDecoder(), factory);
        
        RootConfig a = proxy.newProxy(RootConfig.class);
        
        assertThat(a.getStr(),                          equalTo("default"));
        assertThat(a.getInteger(),                      equalTo(0));
        assertThat(a.getEnum(),                         equalTo(TestEnum.NONE));
        assertThat(a.getSubConfig().str(),              equalTo("default"));
        assertThat(a.getSubConfigFromString().part1(),  equalTo("default1"));
        assertThat(a.getSubConfigFromString().part2(),  equalTo("default2"));
        assertThat(a.getNullable(),                     nullValue());
        assertThat(a.getBaseBoolean(), nullValue());
    }
    
    @Test
    public void testAllPropertiesSet() {
        SettableConfig config = new DefaultSettableConfig();
        config.setProperty("prefix.str", "str1");
        config.setProperty("prefix.integer", 1);
        config.setProperty("prefix.enum", TestEnum.A.name());
        config.setProperty("prefix.subConfigFromString", "a:b");
        config.setProperty("prefix.subConfig.str", "str2");
        config.setProperty("prefix.baseBoolean", true);
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(new NoLibrariesConfig(), config, config.getDecoder(), factory);
        
        RootConfig a = proxy.newProxy(RootConfig.class, "prefix");
        
        assertThat(a.getStr(),      equalTo("str1"));
        assertThat(a.getInteger(),  equalTo(1));
        assertThat(a.getEnum(),     equalTo(TestEnum.A));
        assertThat(a.getSubConfig().str(),      equalTo("str2"));
        assertThat(a.getSubConfigFromString().part1(), equalTo("a"));
        assertThat(a.getSubConfigFromString().part2(), equalTo("b"));

        config.setProperty("prefix.subConfig.str", "str3");
        assertThat(a.getSubConfig().str(),      equalTo("str3"));

        try {
            a.getRequiredValue();
            Assert.fail("should have failed with no value for requiredValue");
        }
        catch (Exception e) {
        }
    }
    
    static interface WithArguments {
        @PropertyName(name="${0}.abc.${1}")
        @DefaultValue("default")
        String getProperty(String part0, int part1);
    }
    
    @Test
    public void testWithArguments() {
        SettableConfig config = new DefaultSettableConfig();
        config.setProperty("a.abc.1", "value1");
        config.setProperty("b.abc.2", "value2");
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(new NoLibrariesConfig(), config, config.getDecoder(), factory);
        WithArguments withArgs = proxy.newProxy(WithArguments.class);
        
        Assert.assertEquals("value1",  withArgs.getProperty("a", 1));
        Assert.assertEquals("value2",  withArgs.getProperty("b", 2));
        Assert.assertEquals("default", withArgs.getProperty("a", 2));
    }
    
    public static interface ConfigWithMap {
        Map<String, SubConfig> getChildren();
    }
    
    @Test
    public void testWithMap() {
        SettableConfig config = new DefaultSettableConfig();
        config.setProperty("children.1.str", "value1");
        config.setProperty("children.2.str", "value2");
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(new NoLibrariesConfig(), config, config.getDecoder(), factory);
        ConfigWithMap withArgs = proxy.newProxy(ConfigWithMap.class);
        
        SubConfig sub1 = withArgs.getChildren().get("1");
        SubConfig sub2 = withArgs.getChildren().get("2");

        Assert.assertEquals("value1", sub1.str());
        Assert.assertEquals("value2", sub2.str());
        
        config.setProperty("children.2.str", "value3");
        Assert.assertEquals("value3", sub2.str());
    }
}
