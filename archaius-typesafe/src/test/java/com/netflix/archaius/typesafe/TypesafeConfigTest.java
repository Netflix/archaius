package com.netflix.archaius.typesafe;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Assert;
import org.junit.Test;

public class TypesafeConfigTest {
    @Test
    public void testGetInteger() throws Exception {
        Config input = ConfigFactory.parseString("int = 42");
        TypesafeConfig config = new TypesafeConfig("test", input);
        Assert.assertEquals("42", config.getString("int"));
        Assert.assertEquals(Integer.valueOf(42), config.getInteger("int"));
    }

    @Test
    public void testGetDuration() throws Exception {
        Config input = ConfigFactory.parseString("duration = 42 seconds");
        TypesafeConfig config = new TypesafeConfig("test", input);
        Assert.assertEquals("42 seconds", config.getString("duration"));
        Assert.assertEquals(Integer.valueOf(42), config.getInteger("duration"));
    }

    @Test
    public void testGetList() throws Exception {
        Config input = ConfigFactory.parseString("list = [a, b, 42, { c = 21 }, [1,2,3]]");
        TypesafeConfig config = new TypesafeConfig("test", input);
        Assert.assertEquals("[a, b, 42, { c = 21 }, [1,2,3]]", config.getString("list"));
        Assert.assertEquals("???", config.getList("list"));
    }

    @Test
    public void testGetConfig() throws Exception {
        Config input = ConfigFactory.parseString("config { a = 1 }");
        TypesafeConfig config = new TypesafeConfig("test", input);
        Assert.assertEquals("1", config.getString("config.a"));
        Assert.assertEquals("1", config.subset("config").getString("a"));
    }
}
