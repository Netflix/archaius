package com.netflix.config.sources;

import com.netflix.config.DynamicConfiguration;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.FixedDelayPollingScheduler;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class TypesafeConfigurationSourceTest
{
    private static DynamicPropertyFactory props;

    @BeforeClass
    public static void setup() {
        props = props();
    }

    @Test
    public void topLevel() {
        assertThat(props.getIntProperty("top", -1).get(), equalTo(3));
    }

    @Test
    public void topLevelDotted() {
        assertThat(props.getStringProperty("nest.dotted", "n/a").get(), equalTo("wxy"));
    }

    @Test
    public void variableSubstitution() {
        assertThat(props.getStringProperty("top-var", "n/a").get(), equalTo("3.14"));
    }

    @Test
    public void simpleNested() {
        assertThat(props.getIntProperty("nest.nested", -1).get(), equalTo(7));
    }

    @Test
    public void nestedMap() {
        assertThat(props.getStringProperty("nest.nested-map.inner", "n/a").get(), equalTo("abc"));
    }

    @Test
    public void willNotClobberWhenExpandingArrays() {
        assertThat(props.getIntProperty("an-unexpanded-array.length", -1).get(), equalTo(13));
        assertThat(props.getIntProperty("an-expanded-array.length", -1).get(), equalTo(7));

        assertThat(props.getStringProperty("an-unexpanded-array[0]", "n/a").get(), equalTo("n/a"));
        assertThat(props.getStringProperty("an-expanded-array[4]", "n/a").get(), equalTo("e"));
    }

    @Test
    public void nestedIntegerArray() {
        assertThat(props.getIntProperty("nest.nested-list.length", -1).get(), equalTo(4));
        assertThat(props.getIntProperty("nest.nested-list[0]", -1).get(), equalTo(3));
        assertThat(props.getIntProperty("nest.nested-list[1]", -1).get(), equalTo(5));
        assertThat(props.getIntProperty("nest.nested-list[2]", -1).get(), equalTo(7));
        assertThat(props.getIntProperty("nest.nested-list[3]", -1).get(), equalTo(11));
    }

    @Test
    public void nestedStringArray() {
        assertThat(props.getIntProperty("arrays.nesting.nested.length", -1).get(), equalTo(3));
        assertThat(props.getStringProperty("arrays.nesting.nested[0]", "n/a").get(), equalTo("abc"));
        assertThat(props.getStringProperty("arrays.nesting.nested[1]", "n/a").get(), equalTo("def"));
        assertThat(props.getStringProperty("arrays.nesting.nested[2]", "n/a").get(), equalTo("ghi"));
    }

    private static DynamicPropertyFactory props() {
        FixedDelayPollingScheduler scheduler = new FixedDelayPollingScheduler(0, 10, false);
        DynamicConfiguration configuration = new DynamicConfiguration(source(), scheduler);
        DynamicPropertyFactory.initWithConfigurationSource(configuration);

        return DynamicPropertyFactory.getInstance();
    }

    private static TypesafeConfigurationSource source() {
        return new TypesafeConfigurationSource() {
            @Override
            protected Config config() {
                return ConfigFactory.load("reference-test");
            }
        };
    }
}