package com.netflix.archaius.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.multibindings.Multibinder;
import com.netflix.archaius.api.ConfigReader;
import com.netflix.archaius.readers.PropertiesConfigReader;
import org.junit.Test;

public class InternalArchaiusModuleTest {

    @Test
    public void succeedOnDuplicateInstall() {
        Guice.createInjector(
            new AbstractModule() {
                @Override
                protected void configure() {
                    Multibinder.newSetBinder(this.binder(), ConfigReader.class).addBinding().to(PropertiesConfigReader.class).asEagerSingleton();
                }
            },
            new InternalArchaiusModule(),
            new InternalArchaiusModule()
        );
    }
}
