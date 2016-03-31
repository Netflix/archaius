package com.netflix.archaius.guice;

import com.google.inject.Guice;
import org.junit.Test;

public class InternalArchaiusModuleTest {

    @Test
    public void succeedOnDuplicateInstall() {
        Guice.createInjector(
                new InternalArchaiusModule(),
                new InternalArchaiusModule());
    }
}
