package com.netflix.archaius;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.annotations.PropertyName;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.config.DefaultSettableConfig;
import com.netflix.archaius.config.EmptyConfig;

public class ProxyFactoryTest {
    public enum TestEnum {
        NONE,
        A, 
        B,
        C
    }
    
    @Configuration(immutable=true)
    public interface ImmutableConfig {
        @DefaultValue("default")
        String getValueWithDefault();
        
        String getValueWithoutDefault1();
        
        String getValueWithoutDefault2();
    }
    
    public interface BaseConfig {
        @DefaultValue("basedefault")
        String getStr();
        
        Boolean getBaseBoolean();
        
        @Nullable
        Integer getNullable();
    }
    
    public interface RootConfig extends BaseConfig {
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

        @DefaultValue("")
        Integer[] getIntArray();

        int getRequiredValue();
    }
    
    public interface SubConfig {
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
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
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
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        
        RootConfig a = proxy.newProxy(RootConfig.class);
        
        assertThat(a.getStr(),                          equalTo("default"));
        assertThat(a.getInteger(),                      equalTo(0));
        assertThat(a.getEnum(),                         equalTo(TestEnum.NONE));
        assertThat(a.getSubConfig().str(),              equalTo("default"));
        assertThat(a.getSubConfigFromString().part1(),  equalTo("default1"));
        assertThat(a.getSubConfigFromString().part2(),  equalTo("default2"));
        assertThat(a.getNullable(),                     nullValue());
        assertThat(a.getBaseBoolean(),                  nullValue());
        assertThat(a.getIntArray(),                     equalTo(new Integer[]{}));
    }
    
    @Test
    public void testDefaultValuesImmutable() {
        Config config = EmptyConfig.INSTANCE;

        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        
        RootConfig a = proxy.newProxy(RootConfig.class, "", true);
        
        assertThat(a.getStr(),                          equalTo("default"));
        assertThat(a.getInteger(),                      equalTo(0));
        assertThat(a.getEnum(),                         equalTo(TestEnum.NONE));
        assertThat(a.getSubConfig().str(),              equalTo("default"));
        assertThat(a.getSubConfigFromString().part1(),  equalTo("default1"));
        assertThat(a.getSubConfigFromString().part2(),  equalTo("default2"));
        assertThat(a.getNullable(),                     nullValue());
        assertThat(a.getBaseBoolean(),                  nullValue());
        assertThat(a.getIntArray(),                     equalTo(new Integer[]{}));
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
        config.setProperty("prefix.intArray", "0,1,2,3");

        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        
        RootConfig a = proxy.newProxy(RootConfig.class, "prefix");
        
        assertThat(a.getStr(),                          equalTo("str1"));
        assertThat(a.getInteger(),                      equalTo(1));
        assertThat(a.getEnum(),                         equalTo(TestEnum.A));
        assertThat(a.getSubConfig().str(),              equalTo("str2"));
        assertThat(a.getSubConfigFromString().part1(),  equalTo("a"));
        assertThat(a.getSubConfigFromString().part2(),  equalTo("b"));
        assertThat(a.getBaseBoolean(),                  equalTo(true));
        assertThat(a.getIntArray(),                     equalTo(new Integer[]{0,1,2,3}));

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
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        WithArguments withArgs = proxy.newProxy(WithArguments.class);
        
        Assert.assertEquals("value1",  withArgs.getProperty("a", 1));
        Assert.assertEquals("value2",  withArgs.getProperty("b", 2));
        Assert.assertEquals("default", withArgs.getProperty("a", 2));
    }
    
    public interface ConfigWithMaps {
        @PropertyName(name="map")
        default Map<String, Long> getStringToLongMap() { return Collections.singletonMap("default", 0L); }

        @PropertyName(name="map2")
        default Map<Long, String> getLongToStringMap() { return Collections.singletonMap(0L, "default"); }
    }
    
    @Test
    public void testWithLongMap() {
        SettableConfig config = new DefaultSettableConfig();
        config.setProperty("map", "a=123,b=456");
        config.setProperty("map2", "1=a,2=b");

        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        ConfigWithMaps withMaps = proxy.newProxy(ConfigWithMaps.class);
        
        long sub1 = withMaps.getStringToLongMap().get("a");
        long sub2 = withMaps.getStringToLongMap().get("b");

        Assert.assertEquals(123, sub1);
        Assert.assertEquals(456, sub2);
        
        config.setProperty("map", "a=123,b=789");
        sub2 = withMaps.getStringToLongMap().get("b");
        Assert.assertEquals(789, sub2);

        Assert.assertEquals("a", withMaps.getLongToStringMap().get(1L));
        Assert.assertEquals("b", withMaps.getLongToStringMap().get(2L));
    }
    
    @Test
    public void testWithLongMapDefaultValue() {
        SettableConfig config = new DefaultSettableConfig();
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        ConfigWithMaps withArgs = proxy.newProxy(ConfigWithMaps.class);
        
        Assert.assertEquals(Collections.singletonMap("default", 0L), withArgs.getStringToLongMap());
        
        config.setProperty("map", "foo=123");
        
        Assert.assertEquals(Collections.singletonMap("foo", 123L), withArgs.getStringToLongMap());
    }
    
    public interface ConfigWithCollections {
        List<Integer> getList();
        
        Set<Integer> getSet();
        
        SortedSet<Integer> getSortedSet();
        
        LinkedList<Integer> getLinkedList();
    }
    
    @Test
    public void testCollections() {
        SettableConfig config = new DefaultSettableConfig();
        config.setProperty("list", "5,4,3,2,1");
        config.setProperty("set", "1,2,3,5,4");
        config.setProperty("sortedSet", "5,4,3,2,1");
        config.setProperty("linkedList", "5,4,3,2,1");
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        ConfigWithCollections withCollections = proxy.newProxy(ConfigWithCollections.class);
        
        Assert.assertEquals(Arrays.asList(5,4,3,2,1), new ArrayList<>(withCollections.getLinkedList()));
        Assert.assertEquals(Arrays.asList(5,4,3,2,1), new ArrayList<>(withCollections.getList()));
        Assert.assertEquals(Arrays.asList(1,2,3,5,4), new ArrayList<>(withCollections.getSet()));
        Assert.assertEquals(Arrays.asList(1,2,3,4,5), new ArrayList<>(withCollections.getSortedSet()));
        
        config.setProperty("list", "6,7,8,9,10");
        Assert.assertEquals(Arrays.asList(6,7,8,9,10), new ArrayList<>(withCollections.getList()));
    }

    @Test
    public void testCollectionsImmutable() {
        SettableConfig config = new DefaultSettableConfig();
        config.setProperty("list", "5,4,3,2,1");
        config.setProperty("set", "1,2,3,5,4");
        config.setProperty("sortedSet", "5,4,3,2,1");
        config.setProperty("linkedList", "5,4,3,2,1");
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        ConfigWithCollections withCollections = proxy.newProxy(ConfigWithCollections.class, "", true);
        
        Assert.assertEquals(Arrays.asList(5,4,3,2,1), new ArrayList<>(withCollections.getLinkedList()));
        Assert.assertEquals(Arrays.asList(5,4,3,2,1), new ArrayList<>(withCollections.getList()));
        Assert.assertEquals(Arrays.asList(1,2,3,5,4), new ArrayList<>(withCollections.getSet()));
        Assert.assertEquals(Arrays.asList(1,2,3,4,5), new ArrayList<>(withCollections.getSortedSet()));
        
        config.setProperty("list", "4,7,8,9,10");
        Assert.assertEquals(Arrays.asList(5,4,3,2,1), new ArrayList<>(withCollections.getList()));
    }

    @Test
    public void emptyNonStringValuesIgnoredInCollections() {
        SettableConfig config = new DefaultSettableConfig();
        config.setProperty("list", ",4, ,2,1");
        config.setProperty("set", ",2, ,5,4");
        config.setProperty("sortedSet", ",4, ,2,1");
        config.setProperty("linkedList", ",4, ,2,1");
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        ConfigWithCollections withCollections = proxy.newProxy(ConfigWithCollections.class);
        
        Assert.assertEquals(Arrays.asList(4,2,1), new ArrayList<>(withCollections.getLinkedList()));
        Assert.assertEquals(Arrays.asList(4,2,1), new ArrayList<>(withCollections.getList()));
        Assert.assertEquals(Arrays.asList(2,5,4), new ArrayList<>(withCollections.getSet()));
        Assert.assertEquals(Arrays.asList(1,2,4), new ArrayList<>(withCollections.getSortedSet()));
    }
    
    public static interface ConfigWithStringCollections {
        List<String> getList();
        
        Set<String> getSet();
        
        SortedSet<String> getSortedSet();
        
        LinkedList<String> getLinkedList();
    }

    @Test
    public void emptyStringValuesAreAddedToCollection() {
        SettableConfig config = new DefaultSettableConfig();
        config.setProperty("list", ",4, ,2,1");
        config.setProperty("set", ",2, ,5,4");
        config.setProperty("sortedSet", ",4, ,2,1");
        config.setProperty("linkedList", ",4, ,2,1");
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        ConfigWithStringCollections withCollections = proxy.newProxy(ConfigWithStringCollections.class);
        
        Assert.assertEquals(Arrays.asList("", "4","", "2","1"), new ArrayList<>(withCollections.getLinkedList()));
        Assert.assertEquals(Arrays.asList("", "4","", "2","1"), new ArrayList<>(withCollections.getList()));
        Assert.assertEquals(Arrays.asList("" ,"2","5","4"), new ArrayList<>(withCollections.getSet()));
        Assert.assertEquals(Arrays.asList("", "1","2","4"), new ArrayList<>(withCollections.getSortedSet()));
    }
    
    @Test
    public void collectionsReturnEmptySetsByDefault() {
        SettableConfig config = new DefaultSettableConfig();
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        ConfigWithStringCollections withCollections = proxy.newProxy(ConfigWithStringCollections.class);
        
        Assert.assertTrue(withCollections.getLinkedList().isEmpty());
        Assert.assertTrue(withCollections.getList().isEmpty());
        Assert.assertTrue(withCollections.getSet().isEmpty());
        Assert.assertTrue(withCollections.getSortedSet().isEmpty());
    }

    @Test
    public void testCollectionsWithoutValue() {
        SettableConfig config = new DefaultSettableConfig();

        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        ConfigWithCollections withCollections = proxy.newProxy(ConfigWithCollections.class);

        System.out.println(withCollections.toString());
        Assert.assertTrue(withCollections.getLinkedList().isEmpty());
        Assert.assertTrue(withCollections.getList().isEmpty());
        Assert.assertTrue(withCollections.getSet().isEmpty());
        Assert.assertTrue(withCollections.getSortedSet().isEmpty());
    }
    
    public interface ConfigWithCollectionsWithDefaultValueAnnotation {
        @DefaultValue("")
        LinkedList<Integer> getLinkedList();
    }
    
    @Test(expected=RuntimeException.class)
    public void testCollectionsWithDefaultValueAnnotation() {
        SettableConfig config = new DefaultSettableConfig();
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        proxy.newProxy(ConfigWithCollectionsWithDefaultValueAnnotation.class);
    }
    
    public interface ConfigWithDefaultStringCollections {
        default List<String> getList() { return Collections.singletonList("default"); }
        
        default Set<String> getSet() { return Collections.singleton("default"); }
        
        default SortedSet<String> getSortedSet() { return new TreeSet<>(Collections.singleton("default")); }
    }

    @Test
    public void interfaceDefaultCollections() {
        SettableConfig config = new DefaultSettableConfig();
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        ConfigWithDefaultStringCollections withCollections = proxy.newProxy(ConfigWithDefaultStringCollections.class);
        
        Assert.assertEquals(Arrays.asList("default"), new ArrayList<>(withCollections.getList()));
        Assert.assertEquals(Arrays.asList("default"), new ArrayList<>(withCollections.getSet()));
        Assert.assertEquals(Arrays.asList("default"), new ArrayList<>(withCollections.getSortedSet()));
    }

    public static interface FailingError {
        default String getValue() { throw new IllegalStateException("error"); }
    }
    
    @Test(expected=RuntimeException.class)
    public void interfaceWithBadDefault() {
        SettableConfig config = new DefaultSettableConfig();
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        FailingError c = proxy.newProxy(FailingError.class);
        c.getValue();
    }
    
    @Test
    public void testObjectMethods() {
        SettableConfig config = new DefaultSettableConfig();
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        WithArguments withArgs = proxy.newProxy(WithArguments.class);
        
        Assert.assertEquals("WithArguments[${0}.abc.${1}='null']", withArgs.toString());
        Assert.assertNotNull(withArgs.hashCode());
        Assert.assertTrue(withArgs.equals(withArgs));
    }
}
