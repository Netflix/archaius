package com.netflix.config;

/**
 * Simple benchmark impl to compare the caching of primitive boolean value in DynamicBooleanProperty with
 * the original implementation.
 *
 * @author Mike Smith
 * Date: 11/25/15
 */
public class CachedPropertiesPerfTest
{
    public static void main(String[] args)
    {
        try {
            long loopCount = 4000000000l;

            // Run twice.
            runTest(loopCount);
            runTest(loopCount);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    private static void runTest(long loopCount)
    {
        // Warmup and then run benchmark.
        runNewDynamicBooleanPropertyTest(10000);
        long durationPrimitive = runNewDynamicBooleanPropertyTest(loopCount);

        // Warmup and then run benchmark.
        runOldDynamicBooleanPropertyTest(10000);
        long durationOriginal = runOldDynamicBooleanPropertyTest(loopCount);

        System.out.println("#####################");
        System.out.println("Original DynamiceBooleanProperty totalled " + durationOriginal + " ms.");
        System.out.println("New DynamicBooleanProperty totalled " + durationPrimitive + " ms.");
    }

    private static long runOldDynamicBooleanPropertyTest(long loopCount)
    {
        OriginalDynamicBooleanProperty prop = new OriginalDynamicBooleanProperty("zuul.test.cachedprops.original", true);

        long startTime = System.currentTimeMillis();

        for (long i=0; i<loopCount; i++) {
            prop.get();
        }

        return System.currentTimeMillis() - startTime;
    }

    private static long runNewDynamicBooleanPropertyTest(long loopCount)
    {
        DynamicBooleanProperty prop =
                new DynamicBooleanProperty("zuul.test.cachedprops.new", true);

        long startTime = System.currentTimeMillis();

        for (long i=0; i<loopCount; i++) {
            prop.get();
        }

        return System.currentTimeMillis() - startTime;
    }

    /**
     * This is a copy of the DynamicBooleanProperty class from before I made the performance optimization to it.
     * It's here so I can still do a back-to-back comparison of performance.
     */
    static class OriginalDynamicBooleanProperty extends PropertyWrapper<Boolean> {

        public OriginalDynamicBooleanProperty(String propName, boolean defaultValue) {
            super(propName, Boolean.valueOf(defaultValue));
        }

        public boolean get() {
            return prop.getBoolean(defaultValue).booleanValue();
        }

        @Override
        public Boolean getValue() {
            return get();
        }
    }
}
