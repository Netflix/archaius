## Overview

This module integrates Archaius2 with Guice to specify the override hierarchy as well as enable injection of properties into objects instantiated through guice. 

## Getting Started

To enable Archaius2 in Guice just add the ArchaiusModule when creating the injector.

```java
Injector injector = Guice.createInjector(new ArchaiusModule());
```

ArchaiusModule specifies a complete set of default bindings to enable all archaius features.  The default bindings  may be replaced using Guice's Modules.override() mechanims.

```java
Injector injector = Guice.createInjector(Modules
    .override(new ArchaiusModule()),
    .with(new AbstractModule() {
        void configure() {
           // Load 'config.properties' instead of 'application.properties'
           bind(String.class).annotatedWith(ApplicationLayer.class).to("config");
           // Use MyCascadeStrategy instead of the default CascadeStrategy.
           bind(CascadeStrategy.class).to(MyCascadeStrategy.class);
        }
    });
    
```

Once installed the following types may be injected

### Config

Top level config for the override hierarchy.  

```java
public class SomeClass {
    @Inject
    public SomeClass(Config config) {
        config.getInteger("some.property");
    }
}
```

By default Config is set up with the following override layers,
* @RuntimeLayer
* @OverrideLayer
* @SystemLayer
* @EnvironmentLayer
* @ApplicationLayer
* @LibrariesLayer

Note that each of these layers is a singleton and the default Config is simply a CompositeConfig
of these layers.  It is therefore possible to define additional override structures that share
these instances.  Each layer may also be accessed directly by injecting the layer as described below.  

### @RuntimeLayer SettableConfig

The runtime layer allows properties to be set from code.

```java
@Inject
public SomeClass(@RuntimeLayer SettablConfig config) {
   config.setProperty("foo", "bar");
}
```

### @OverrideLayer Config

The override layer is normally used to provide configuration overrides that are loaded dynamically
from an external source (see archaius2-persisted2).  By default this layer is bound to the EmptyConfig and as such is actually ignored when setting up the top level Config.  

The following binding in an ArchaiusModule override may be specified to enable the Persisted2 client (Persisted2 service is internal to Netflix and not yet open source).
```java 
bind(Persisted2ClientConfig.class).toInstance(clientConfig);
bind(Config.class).annotatedWith(OverrideLayer.class).toProvider(Persisted2ConfigProvider.class).in(Scopes.SINGLETON);
```

### @SystemLayer Config

Configuration wrapping the system layer

### @EnvironmentLayer Config

Configuration wrapping the environment variables

### @LibrariesLayer CompositeConfig

Layer consisting of all libraries.  Any library added to this layer takes precedence over previously 
added libraries.  While @LibrariesLayer CompositeConfig may be injected and added to directly it is best to use the @ConfigurationSource annotation.

### @ApplicationLayer Config

Layer consisting of all application properties.  Application properties are loaded at runtime using cascade loading of the resoure called 'application'.  The resource name may be overridden with the following overide binding

```java
bind(String.class).annotatedWith(ApplicationLayer.class).to("alternative-name-to-application");
```

The entire application layer may be customized using the following override bindgin
```
@Provides
@Singleton
@ApplicationLayer
Config getMyCustomApplicationConfig() {
    CompositeConfig config = new CompositeConfig();
    // ... add child configurations to config
    return config;
}

### ConfigLoader loader

This is the default configuration loader which is tied to a default CascadeStrategy and the top level
Config for replacements within the CascadeStrategy.  This config loader is used when processing 
@ConfigurationSource annotations.  Note that @ConfigurationSource processing will add configurations to
the @LibrariesLayer layer of the override structure.  Loading configuration from ConfigLoader directly
will not autoload the configuration into the @LibrariesLayer.

By default the ConfigLoader will support loading of properties files.  Support for additional file formats
may be added using Guice's multibinding mechanism

```java
Multibinder.newSetBinder(binder(), ConfigReader.class)
    .addBinding().to(TypesafeConfigReader.class);
```

The default CascadeStrategy may be overridden using the following bindings

```java
bind(CascadeStrategy.class).to(MyCascadeStrategy.class);
```

## Loading configuration

You can load configuration into the LibrariesLayer by annotating an injectable class with @ConfigurationSource.
Note that @ConfigurationSource is processed AFTER the constructor is called so code cannot expect the 
configuration to be available in the constructor.  To address this limitation it is recommended that application
code inject a @ConfigurationSource annotated configuration class instead of mixing configuration with functional
code.

For example,

```java
// Class where configuration is loaded
@Singleton
@ConfigurationSource({"serviceA"})
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

All @ConfigurationSource annotated classes will load configuration using the default cascading strategy.
However, a per class override strategy may be specified as well.

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
