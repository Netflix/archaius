package com.netflix.archaius;

import org.junit.Test;

import com.netflix.archaius.annotations.DefaultValue;
import com.netflix.archaius.config.EmptyConfig;
import com.netflix.archaius.config.MapConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ProxyFactoryTest {
    public static enum TestEnum {
        NONE,
        A, 
        B,
        C
    }
    
    public static interface RootConfig {
        @DefaultValue("default")
        String getStr();
        
        @DefaultValue("0")
        int getInteger();
        
        @DefaultValue("NONE")
        TestEnum getEnum();
        
        SubConfig getSubConfig();
        
        @DefaultValue("default1:default2")
        SubConfigFromString getSubConfigFromString();
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
    public void testDefaultValues() {
        Config config = EmptyConfig.INSTANCE;

        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config.getDecoder(), factory);
        
        RootConfig a = proxy.newProxy(RootConfig.class);
        
        assertThat(a.getStr(),                          equalTo("default"));
        assertThat(a.getInteger(),                      equalTo(0));
        assertThat(a.getEnum(),                         equalTo(TestEnum.NONE));
        assertThat(a.getSubConfig().str(),              equalTo("default"));
        assertThat(a.getSubConfigFromString().part1(),  equalTo("default1"));
        assertThat(a.getSubConfigFromString().part2(),  equalTo("default2"));
        System.out.println(a);
    }
    
    @Test
    public void testAllPropertiesSet() {
        Config config = MapConfig.builder()
                .put("prefix.str", "str1")
                .put("prefix.integer", 1)
                .put("prefix.enum", TestEnum.A.name())
                .put("prefix.subConfigFromString", "a:b")
                .put("prefix.subConfig.str", "str2")
                .build();
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config.getDecoder(), factory);
        
        RootConfig a = proxy.newProxy(RootConfig.class, "prefix");
        
        assertThat(a.getStr(),      equalTo("str1"));
        assertThat(a.getInteger(),  equalTo(1));
        assertThat(a.getEnum(),     equalTo(TestEnum.A));
        assertThat(a.getSubConfig().str(),      equalTo("str2"));
        assertThat(a.getSubConfigFromString().part1(), equalTo("a"));
        assertThat(a.getSubConfigFromString().part2(), equalTo("b"));
        
        System.out.println(a);
    }
}
