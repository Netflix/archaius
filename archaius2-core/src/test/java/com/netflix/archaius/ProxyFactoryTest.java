package com.netflix.archaius;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.annotation.Nullable;

import com.netflix.archaius.api.Decoder;
import com.netflix.archaius.api.TypeConverter;
import com.netflix.archaius.config.MapConfig;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.PropertyFactory;
import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;
import com.netflix.archaius.api.annotations.PropertyName;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.config.DefaultSettableConfig;
import com.netflix.archaius.config.EmptyConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
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
    
    @SuppressWarnings("unused")
    public interface BaseConfig {
        @DefaultValue("basedefault")
        String getStr();
        
        Boolean getBaseBoolean();

        @Nullable
        Integer getNullable();

        default long getLongValueWithDefault() {
            return 42L;
        }
    }
    
    @SuppressWarnings("UnusedReturnValue")
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

        default long getOtherLongValueWithDefault() {
            return 43L;
        }
    }
    
    public interface SubConfig {
        @DefaultValue("default")
        String str();
    }
    
    public static class SubConfigFromString {
        private final String[] parts;

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
        assertThat(a.getLongValueWithDefault(),         equalTo(42L));
        assertThat(a.getOtherLongValueWithDefault(),    equalTo(43L));
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
            fail("should have failed with no value for requiredValue");
        }
        catch (Exception expected) {
        }
    }
    
    interface WithArguments {
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
        
        assertEquals("value1",  withArgs.getProperty("a", 1));
        assertEquals("value2",  withArgs.getProperty("b", 2));
        assertEquals("default", withArgs.getProperty("a", 2));
    }

    @Configuration(prefix = "foo.bar")
    interface WithArgumentsAndPrefix {
        @PropertyName(name="baz.${0}.abc.${1}")
        @DefaultValue("default")
        String getPropertyWithoutPrefix(String part0, int part1);

        // For backward compatibility, we need to accept PropertyNames that also include the prefix.
        @PropertyName(name="foo.bar.baz.${0}.abc.${1}")
        @DefaultValue("default")
        String getPropertyWithPrefix(String part0, int part1);
    }

    @Test
    public void testWithArgumentsAndPrefix() {
        SettableConfig config = new DefaultSettableConfig();
        config.setProperty("foo.bar.baz.a.abc.1", "value1");
        config.setProperty("foo.bar.baz.b.abc.2", "value2");

        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        WithArgumentsAndPrefix withArgs = proxy.newProxy(WithArgumentsAndPrefix.class);

        assertEquals("value1",  withArgs.getPropertyWithPrefix("a", 1));
        assertEquals("value1",  withArgs.getPropertyWithoutPrefix("a", 1));
        assertEquals("value2",  withArgs.getPropertyWithPrefix("b", 2));
        assertEquals("value2",  withArgs.getPropertyWithoutPrefix("b", 2));
        assertEquals("default", withArgs.getPropertyWithPrefix("a", 2));
        assertEquals("default", withArgs.getPropertyWithoutPrefix("a", 2));
    }


    @SuppressWarnings("unused")
    public interface WithArgumentsAndDefaultMethod {
        @PropertyName(name="${0}.abc.${1}")
        default String getPropertyWith2Placeholders(String part0, int part1) {
            return "defaultFor2";
        }

        @PropertyName(name="${0}.def")
        default String getPropertyWith1Placeholder(String part0) {
            return "defaultFor1";
        }
    }

    @Test
    public void testWithArgumentsAndDefaultMethod() {
        SettableConfig config = new DefaultSettableConfig();
        config.setProperty("a.abc.1", "value1");
        config.setProperty("b.abc.2", "value2");
        config.setProperty("c.def",   "value_c");

        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        WithArgumentsAndDefaultMethod withArgsAndDefM = proxy.newProxy(WithArgumentsAndDefaultMethod.class);

        assertEquals("value1",  withArgsAndDefM.getPropertyWith2Placeholders("a", 1));
        assertEquals("value2",  withArgsAndDefM.getPropertyWith2Placeholders("b", 2));
        assertEquals("defaultFor2", withArgsAndDefM.getPropertyWith2Placeholders("a", 2));

        assertEquals("value_c", withArgsAndDefM.getPropertyWith1Placeholder("c"));
        assertEquals("defaultFor1", withArgsAndDefM.getPropertyWith1Placeholder("q"));
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

        assertEquals(123, sub1);
        assertEquals(456, sub2);
        
        config.setProperty("map", "a=123,b=789");
        sub2 = withMaps.getStringToLongMap().get("b");
        assertEquals(789, sub2);

        assertEquals("a", withMaps.getLongToStringMap().get(1L));
        assertEquals("b", withMaps.getLongToStringMap().get(2L));
    }
    
    @Test
    public void testWithLongMapDefaultValue() {
        SettableConfig config = new DefaultSettableConfig();
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        ConfigWithMaps withArgs = proxy.newProxy(ConfigWithMaps.class);
        
        assertEquals(Collections.singletonMap("default", 0L), withArgs.getStringToLongMap());
        
        config.setProperty("map", "foo=123");
        
        assertEquals(Collections.singletonMap("foo", 123L), withArgs.getStringToLongMap());
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
        
        assertEquals(Arrays.asList(5,4,3,2,1), new ArrayList<>(withCollections.getLinkedList()));
        assertEquals(Arrays.asList(5,4,3,2,1), new ArrayList<>(withCollections.getList()));
        assertEquals(Arrays.asList(1,2,3,5,4), new ArrayList<>(withCollections.getSet()));
        assertEquals(Arrays.asList(1,2,3,4,5), new ArrayList<>(withCollections.getSortedSet()));
        
        config.setProperty("list", "6,7,8,9,10");
        assertEquals(Arrays.asList(6,7,8,9,10), new ArrayList<>(withCollections.getList()));
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
        
        assertEquals(Arrays.asList(5,4,3,2,1), new ArrayList<>(withCollections.getLinkedList()));
        assertEquals(Arrays.asList(5,4,3,2,1), new ArrayList<>(withCollections.getList()));
        assertEquals(Arrays.asList(1,2,3,5,4), new ArrayList<>(withCollections.getSet()));
        assertEquals(Arrays.asList(1,2,3,4,5), new ArrayList<>(withCollections.getSortedSet()));
        
        config.setProperty("list", "4,7,8,9,10");
        assertEquals(Arrays.asList(5,4,3,2,1), new ArrayList<>(withCollections.getList()));
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
        
        assertEquals(Arrays.asList(4,2,1), new ArrayList<>(withCollections.getLinkedList()));
        assertEquals(Arrays.asList(4,2,1), new ArrayList<>(withCollections.getList()));
        assertEquals(Arrays.asList(2,5,4), new ArrayList<>(withCollections.getSet()));
        assertEquals(Arrays.asList(1,2,4), new ArrayList<>(withCollections.getSortedSet()));
    }
    
    public interface ConfigWithStringCollections {
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
        
        assertEquals(Arrays.asList("", "4","", "2","1"), new ArrayList<>(withCollections.getLinkedList()));
        assertEquals(Arrays.asList("", "4","", "2","1"), new ArrayList<>(withCollections.getList()));
        assertEquals(Arrays.asList("" ,"2","5","4"), new ArrayList<>(withCollections.getSet()));
        assertEquals(Arrays.asList("", "1","2","4"), new ArrayList<>(withCollections.getSortedSet()));
    }
    
    @Test
    public void collectionsReturnEmptySetsByDefault() {
        SettableConfig config = new DefaultSettableConfig();
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        ConfigWithStringCollections withCollections = proxy.newProxy(ConfigWithStringCollections.class);
        
        assertTrue(withCollections.getLinkedList().isEmpty());
        assertTrue(withCollections.getList().isEmpty());
        assertTrue(withCollections.getSet().isEmpty());
        assertTrue(withCollections.getSortedSet().isEmpty());
    }

    @Test
    public void testCollectionsWithoutValue() {
        SettableConfig config = new DefaultSettableConfig();

        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        ConfigWithCollections withCollections = proxy.newProxy(ConfigWithCollections.class);

        System.out.println(withCollections.toString());
        assertTrue(withCollections.getLinkedList().isEmpty());
        assertTrue(withCollections.getList().isEmpty());
        assertTrue(withCollections.getSet().isEmpty());
        assertTrue(withCollections.getSortedSet().isEmpty());
    }
    
    @SuppressWarnings("unused")
    public interface ConfigWithCollectionsWithDefaultValueAnnotation {
        @DefaultValue("")
        LinkedList<Integer> getLinkedList();
    }
    
    @Test
    public void testCollectionsWithDefaultValueAnnotation() {
        SettableConfig config = new DefaultSettableConfig();
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        assertThrows(RuntimeException.class, () -> proxy.newProxy(ConfigWithCollectionsWithDefaultValueAnnotation.class));
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
        
        assertEquals(Collections.singletonList("default"), new ArrayList<>(withCollections.getList()));
        assertEquals(Collections.singletonList("default"), new ArrayList<>(withCollections.getSet()));
        assertEquals(Collections.singletonList("default"), new ArrayList<>(withCollections.getSortedSet()));
    }

    @SuppressWarnings("UnusedReturnValue")
    public interface FailingError {
        default String getValue() { throw new IllegalStateException("error"); }
    }
    
    @Test
    public void interfaceWithBadDefault() {
        SettableConfig config = new DefaultSettableConfig();
        
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        FailingError c = proxy.newProxy(FailingError.class);
        assertThrows(RuntimeException.class, c::getValue);
    }
    
    @Test
    public void testObjectMethods() {
        // These tests just ensure that toString, equals and hashCode have implementations that don't fail.
        SettableConfig config = new DefaultSettableConfig();
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        WithArguments withArgs = proxy.newProxy(WithArguments.class);
        
        assertEquals("WithArguments[${0}.abc.${1}='default']", withArgs.toString());
        //noinspection ObviousNullCheck
        assertNotNull(withArgs.hashCode());
        //noinspection EqualsWithItself
        assertEquals(withArgs, withArgs);
    }

    @Test
    public void testObjectMethods_ClassWithArgumentsAndDefaultMethod() {
        // These tests just ensure that toString, equals and hashCode have implementations that don't fail.
        SettableConfig config = new DefaultSettableConfig();
        PropertyFactory factory = DefaultPropertyFactory.from(config);
        ConfigProxyFactory proxy = new ConfigProxyFactory(config, config.getDecoder(), factory);
        WithArgumentsAndDefaultMethod withArgs = proxy.newProxy(WithArgumentsAndDefaultMethod.class);

        // Printing 'null' here is a compromise. The default method in the interface is being called with a bad signature.
        // There's nothing the proxy could return here that isn't a lie, but at least this is a mostly harmless lie.
        // This test depends implicitly on the iteration order for HashMap, which could change on future Java releases.
        assertEquals("WithArgumentsAndDefaultMethod[${0}.def='null',${0}.abc.${1}='null']", withArgs.toString());
        //noinspection ObviousNullCheck
        assertNotNull(withArgs.hashCode());
        //noinspection EqualsWithItself
        assertEquals(withArgs, withArgs);
    }

    @Disabled("Manual test. Output is just log entries, can't be verified by CI")
    @Test
    public void testLogExcessiveUse() {
        SettableConfig config = new DefaultSettableConfig();
        PropertyFactory propertyFactory = DefaultPropertyFactory.from(config);

        for (int i = 0; i < 5; i++) {
            new ConfigProxyFactory(config, config.getDecoder(), propertyFactory); // Last one should emit a log!
        }

        SettableConfig otherConfig = new DefaultSettableConfig();
        for (int i = 0; i < 4; i++) {
            new ConfigProxyFactory(otherConfig, config.getDecoder(), propertyFactory); // Should not log! It's only 4 and on a different config.
        }

        ConfigProxyFactory factory = new ConfigProxyFactory(config, config.getDecoder(), propertyFactory); // Should not log, because we only log every 5.
        for (int i = 0; i < 5; i++) {
            factory.newProxy(WithArguments.class, "aPrefix"); // Last one should emit a log
        }
        factory.newProxy(WithArguments.class, "somePrefix"); // This one should not log, because it's a new prefix.
    }

    interface ConfigWithNestedInterface {
        int intValue();

        CustomObject customValue();

        interface CustomObject {
            String value();
        }
    }

    @Test
    public void testNestedInterfaceWithCustomDecoder() {
        TypeConverter<ConfigWithNestedInterface.CustomObject> customObjectTypeConverter = value -> value::toUpperCase;
        TypeConverter.Factory customTypeConverterFactory = (type, registry) -> {
            if (type.equals(ConfigWithNestedInterface.CustomObject.class)) {
                return Optional.of(customObjectTypeConverter);
            }
            return Optional.empty();
        };
        Decoder customDecoder = CustomDecoder.create(Collections.singletonList(customTypeConverterFactory));
        Config config = MapConfig.builder()
                .put("intValue", "5")
                .put("customValue", "blah")
                .build();
        config.setDecoder(customDecoder);
        ConfigProxyFactory proxyFactory = new ConfigProxyFactory(config, config.getDecoder(), DefaultPropertyFactory.from(config));

        ConfigWithNestedInterface proxy = proxyFactory.newProxy(ConfigWithNestedInterface.class);
        assertEquals(5, proxy.intValue());
        assertEquals("BLAH", proxy.customValue().value());
    }
}
