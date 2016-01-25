package com.netflix.archaius.guice;

import com.google.inject.CreationException;
import com.google.inject.Guice;
import org.junit.Test;

public class InternalArchaiusModuleTest {

    @Test(expected=CreationException.class)
    public void failOnDuplicateInstall() {
        Guice.createInjector(
                new InternalArchaiusModule(),
                new InternalArchaiusModule());
    }
}
