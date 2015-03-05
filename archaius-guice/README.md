## Overview

This module integrates Archaius Config with Guice to enable custom injection of properties
into objects created by guice.

## Getting Started

To enable create Guice bindings for Config and AppConfig and install a single guice module.

```java
Injector injector = Guice.createInjector(
    new AbstractModule() {
        @Override
        protected void configure() {
            AppConfig config = AppConfig.createDefaultConfig();
            bind(Config.class).toInstance(config);
            bind(AppConfig.class).toInstance(config);
        }
    },
    new ArchaiusModule()
    );
```

## Loading configuration

You can load configuration into AppConfig by annotating an injectable class with @ConfigurationSource.
Note that @ConfigurationSource is processed AFTER the constructor is called so you cannot assume the 
configuration has been loaded in the constructor.  To address this limitation it's recommended that classes
inject a @ConfigurationSource annotated class.  (A future integration may use Govnerator's lifecycle states to 
ensure that configuration is injected prior to @PostConstruct being called)

For example,

```java
// Class where configuration is load
@Singleton
@ConfigurationSource({"serviceA"}
public class ServiceAConfiguration {
}

// Class where configuration is used
@Singleton
public class ServiceA {
    @Inject
    public ServiceA(ServiceAConfiguration config) {
        config.getTimeout();  // Will return the loaded configuration value
    }
}

```

## Cascade loading

All @ConfigurationSource annotated classes will load configuration using the default cascading strategy 
configured on AppConfig.  However, a per class override strategy may be specified as well.

For example,
```java
@Singleton
@ConfigurationSource(value={"serviceA"}, cascading=MyCascadingStrategy.class)
public class ServiceAConfiguration {
}

@Singleton
public static class MyCascadingStrategy extends ConcatCascadeStrategy {
    public MyCascadingStrategy() {
        super(new String[]{"${env}", "${dc}", "${stack}");
    }
}
```

## Configuration binding

Configuration binding is triggered by annotating a class with @Configuration.

```java
@Configuration(prefix="serviceA")
public class ServiceAConfiguration {
    // Can inject configuration into field
    private Integer timeout;
    
    // Can inject configuration into setter methods
    public void setTimeout(Integer timeout) {
    }
    
    // Can inject into withXXX methods used by builders
    public void withTimeout(Integer timeout) {
    }
}
```

```properties
serviceA.timeout=10000
```

## Configuration based named injection

TBD