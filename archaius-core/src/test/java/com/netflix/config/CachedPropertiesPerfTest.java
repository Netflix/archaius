package com.netflix.config;

/**
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
        // First warmup.
        runCachedBooleanPropertyTest(10000);
        runDynamicBooleanPropertyTest(10000);
        runDynamicPrimitiveBooleanPropertyTest(10000);

        // Now run benchmark.
        long durationExperiment = runCachedBooleanPropertyTest(loopCount);
        long durationOriginal = runDynamicBooleanPropertyTest(loopCount);
        long durationPrimitive = runDynamicPrimitiveBooleanPropertyTest(loopCount);

        System.out.println("#####################");
        System.out.println("CachedProperties totalled " + durationExperiment + " ms.");
        System.out.println("DynamicProperty totalled " + durationOriginal + " ms.");
        System.out.println("DynamicPrimitiveBooleanProperty totalled " + durationPrimitive + " ms.");
    }

    private static long runDynamicBooleanPropertyTest(long loopCount)
    {
        DynamicBooleanProperty prop = DynamicPropertyFactory.getInstance()
                .getBooleanProperty("zuul.test.cachedprops.original", true);

        long startTime = System.currentTimeMillis();

        for (long i=0; i<loopCount; i++) {
            prop.get();
        }

        return System.currentTimeMillis() - startTime;
    }

    private static long runCachedBooleanPropertyTest(long loopCount)
    {
        CachedProperties.Boolean prop =
                new CachedProperties.Boolean("zuul.test.cachedprops.original", true);

        long startTime = System.currentTimeMillis();

        for (long i=0; i<loopCount; i++) {
            prop.get();
        }

        return System.currentTimeMillis() - startTime;
    }


    private static long runDynamicPrimitiveBooleanPropertyTest(long loopCount)
    {
        DynamicPrimitiveBooleanProperty prop =
                new DynamicPrimitiveBooleanProperty("zuul.test.cachedprops.original", true);

        long startTime = System.currentTimeMillis();

        for (long i=0; i<loopCount; i++) {
            prop.get();
        }

        return System.currentTimeMillis() - startTime;
    }
}
