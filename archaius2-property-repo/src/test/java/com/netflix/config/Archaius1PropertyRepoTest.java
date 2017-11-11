package com.netflix.config;

public class Archaius1PropertyRepoTest extends AbstractPropertyRepoTest<Archaius1PropertyRepo> {

    public Archaius1PropertyRepoTest() {
        super(properties -> {
            ConfigurationManager.loadProperties(properties);
            return new Archaius1PropertyRepo();
        }, (k, v) -> {
            ConfigurationManager.getConfigInstance().setProperty(k, v);
        });
    }

}
