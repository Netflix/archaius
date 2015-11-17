package com.netflix.archaius;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.netflix.archaius.interpolate.CommonsStrInterpolator;
import com.netflix.archaius.readers.PropertiesConfigReader;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author David Liu
 */
public class Archaius2ApiTest {

    private static final String PROP_NAME = "foo.bar";

    private PropertyFactory propertyFactory;

    @Before
    public void setUp() throws Exception {
        propertyFactory = getPropertyFactory();
    }

    @Test
    public void testPropertyLoading() {
        assertThat(propertyFactory.getConfig().getInteger(PROP_NAME), is(123));
    }

    @Test
    public void testPropertyListen() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicInteger result = new AtomicInteger(0);
        propertyFactory
                .getProperty(PROP_NAME)
                .asInteger(result.get())
                .addListener(new PropertyListener<Integer>() {
                    @Override
                    public void onChange(Integer value) {
                        latch.countDown();
                        result.set(value);
                    }

                    @Override
                    public void onParseError(Throwable error) {

                    }
                });

        assertThat(latch.await(1, TimeUnit.SECONDS), is(true));
        assertThat(result.get(), is(123));
    }


    // code from archaius2-core should only live here
    private static PropertyFactory getPropertyFactory() throws Exception {
        PropertiesConfigReader reader = new PropertiesConfigReader();
        Config config = reader.load(null, "application", CommonsStrInterpolator.INSTANCE, new StrInterpolator.Lookup() {
            @Override
            public String lookup(String key) {
                return null;
            }
        });

        return DefaultPropertyFactory.from(config);

    }
}
