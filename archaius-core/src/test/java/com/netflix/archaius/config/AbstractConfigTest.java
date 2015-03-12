package com.netflix.archaius.config;

import java.util.Collections;
import java.util.Iterator;

import org.junit.Assert;
import org.junit.Test;

public class AbstractConfigTest {

  private final AbstractConfig config = new AbstractConfig("test") {
    @Override public boolean containsProperty(String key) {
      return "foo".equals(key);
    }

    @Override public boolean isEmpty() {
      return false;
    }

    @Override public Iterator<String> getKeys() {
      return Collections.singletonList("foo").iterator();
    }

    @Override public Object getRawProperty(String key) {
      return "bar";
    }
  };

  @Test
  public void testGet() throws Exception {
    Assert.assertEquals("bar", config.get(String.class, "foo"));
  }
}
