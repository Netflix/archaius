package com.netflix.archaius.persisted2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.archaius.config.polling.PollingResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JsonPersistedV2ReaderTest {
    @Test
    public void idFieldReturnedWhenPresent() throws Exception {
        List<TestProperty> propertyList = new ArrayList<>();
        propertyList.add(new TestProperty("key1", "value3", "id3", "app1", ""));
        // The next two properties are the only two that are actually resolved, as the other two are overridden due to
        // the presence of the region field.
        propertyList.add(new TestProperty("key1", "value1", "id1", "app1", "region1"));
        propertyList.add(new TestProperty("key2", "value2", "id2", "app1", "region1"));
        propertyList.add(new TestProperty("key2", "value4", "id4", "app1", ""));
        TestPropertyList properties = new TestPropertyList(propertyList);

        JsonPersistedV2Reader reader =
                JsonPersistedV2Reader.builder(
                        () -> new ByteArrayInputStream(
                                new ObjectMapper().writeValueAsBytes(properties)))
                .withPath("propertiesList")
                .withReadIdField(true)
                .build();

        PollingResponse response = reader.call();
        Map<String, String> props = response.getToAdd();
        assertEquals(2, props.size());
        assertEquals("value1", props.get("key1"));
        assertEquals("value2", props.get("key2"));
        Map<String, String> propIds = response.getNameToIdsMap();
        assertEquals(2, propIds.size());
        assertEquals("id1", propIds.get("key1"));
        assertEquals("id2", propIds.get("key2"));
    }
    @Test
    public void idFieldAbsent() throws Exception {
        List<TestProperty> propertyList = new ArrayList<>();
        propertyList.add(new TestProperty("key1", "value3", "id3", "app1", ""));
        // The next two properties are the only two that are actually resolved, as the other two are overridden due to
        // the presence of the region field.
        propertyList.add(new TestProperty("key1", "value1", "id1", "app1", "region1"));
        propertyList.add(new TestProperty("key2", "value2", "id2", "app1", "region1"));
        propertyList.add(new TestProperty("key2", "value4", "id4", "app1", ""));
        TestPropertyList properties = new TestPropertyList(propertyList);

        JsonPersistedV2Reader reader =
                JsonPersistedV2Reader.builder(
                                () -> new ByteArrayInputStream(
                                        new ObjectMapper().writeValueAsBytes(properties)))
                        .withPath("propertiesList")
                        .build();

        PollingResponse response = reader.call();
        Map<String, String> props = response.getToAdd();
        assertEquals(2, props.size());
        assertEquals("value1", props.get("key1"));
        assertEquals("value2", props.get("key2"));
        assertTrue(response.getNameToIdsMap().isEmpty());
    }

    public static class TestPropertyList {
        public List<TestProperty> propertiesList;
        public TestPropertyList(List<TestProperty> propertiesList) {
            this.propertiesList = propertiesList;
        }
    }

    public static class TestProperty {
        public String key;
        public String value;
        public String propertyId;
        public String appId;
        public String region;

        public TestProperty(String key, String value, String propertyId, String appId, String region) {
            this.key = key;
            this.value = value;
            this.propertyId = propertyId;
            this.appId = appId;
            this.region = region;
        }
    }
}
