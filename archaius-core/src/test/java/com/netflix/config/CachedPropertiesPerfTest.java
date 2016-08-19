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
        double avgPrimitiveNs = (durationPrimitive * 1000d * 1000d) / loopCount;

        // Warmup and then run benchmark.
        DynamicBooleanProperty oldProp = new DynamicBooleanProperty("zuul.test.cachedprops.original", true);
        runDynamicBooleanPropertyTest(oldProp, 10000);
        long durationOriginal = runDynamicBooleanPropertyTest(oldProp, loopCount);
        double avgOriginalNs = (durationOriginal * 1000d * 1000d) / loopCount;

        System.out.println("#####################");
        System.out.println("DynamicBooleanProperty: " + loopCount + " calls. Total = " + durationOriginal + " ms. Avg = " + avgOriginalNs + " nanos.");
        System.out.println("CachedDynamicBooleanProperty: " + loopCount + " calls. Total = " + durationPrimitive + " ms. Avg = " + avgPrimitiveNs + " nanos.");
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
