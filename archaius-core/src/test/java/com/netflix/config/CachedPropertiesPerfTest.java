package com.netflix.config;

/**
 * Simple benchmark impl to compare the caching of primitive boolean value in CachedDynamicBooleanProperty with
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
        DynamicBooleanProperty newProp = new CachedDynamicBooleanProperty("zuul.test.cachedprops.new", true);
        runDynamicBooleanPropertyTest(newProp, 10000);
        long durationPrimitive = runDynamicBooleanPropertyTest(newProp, loopCount);

        // Warmup and then run benchmark.
        DynamicBooleanProperty oldProp = new DynamicBooleanProperty("zuul.test.cachedprops.original", true);
        runDynamicBooleanPropertyTest(oldProp, 10000);
        long durationOriginal = runDynamicBooleanPropertyTest(oldProp, loopCount);

        System.out.println("#####################");
        System.out.println("DynamicBooleanProperty totalled " + durationOriginal + " ms.");
        System.out.println("CachedDynamicBooleanProperty totalled " + durationPrimitive + " ms.");
    }

    private static long runDynamicBooleanPropertyTest(DynamicBooleanProperty prop, long loopCount)
    {
        long startTime = System.currentTimeMillis();

        boolean value;
        for (long i=0; i<loopCount; i++) {
            value = prop.get();
        }

        return System.currentTimeMillis() - startTime;
    }
}
