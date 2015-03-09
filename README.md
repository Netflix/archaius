# Overview

Archaius is a configuration library for accessing a mixture of static as well
as dynamic configuration as a single configuration unit.

Archaius has minimal external dependencies, is non-static, and DI friendly.

# Getting Started

Archaius provides a set of specialized configuration objects that may be combined
using CompositeConfig into a specific override structure.  For convenience you may 
also use AppConfig as a best practices combination of runtime overrides,
remote configuration, environment, system, application and library configuration 
levels.  All access to the configuration in application should go through an instance 
of AppConfig.  

```java
// Create the config.  This will load configuration from config.properties
AppConfig config = DefaultAppConfig.createDefault();

// Read a property
config.getString("propertyname");

// Load configuration into the libraries level
config.addConfig(config.newLoader().load("library");

// Get a fast property
config.createProperty("propertyname").asString();

```

# Reading configuration

All Config derived classes provide access to their underlying configuration via the numerous 
getString(), getInt(), getBoolean() methods.  In additional to basic primitives and collections
Config will allow creation of any class that has a constructor that takes a single String parameter.

# Replacements

Archaius supports standard variable replacement syntax such as ${other.property.name}.  Note that
once added to the ConfigManager replacement values may exist in any of the configurations and are
resolved in first seen order.

# Configuration loaders

Archaius has a default built in loader for .properties files but can also be extended with custom
property specifications such as HOCON.  In addition multiple contextual overrides for a single 
configuration resource name may be derived using a CascadeStrategy.  The strategy may also specify
replacements from already loaded configurations (such as System and Environment properties).

For example, the following cascade policy will attempt to load the following permuatations in addition
to the base resource name:  basename, basename-${env}, basename-${env}-${region}.  

``` java
DefaultConfigLoader loader = DefaultConfigLoader.builder()
    .withStrInterpolator(rootConfig.getStrInterpolator())
    .withConfigLoader(new TypesafeLoader())
    .withDefaultCascadeStrategy(new ConcatCascadeStrategy("${env}", "${region}"))
    ...
```


```java    
// libraryConfigs is a layer that tracks configurations at the libraries layer
CompositeConfig libraryConfigs = new CompositeConfig("LIBRARIES");
...
// Load a configuration, using the default cascading, for the resource name 'foo'
libraryConfigs.addConfigFirst(loader.newLoader().load("foo"));
```

# Dynamic Properties

Dynamic configuration support is split into the configuration loading and property value resolution.

## Configuration loading

A dynamic configuration implements DynamicConfig in addition to the core Config interface.  When adding
a DynamicConfig derived configuration ConfigManager will automatically register for configuration 
change notifications and incorporate new values into the main configuration.  Archaius provides a base
PollingDynamicConfg for use with configuration sources that are polled frequently to refresh the entire
configuration snapshot.  Implement Config and DynamicConfig directly For better optimized configuration 
sources, such as ZooKeeper, which offer per property notification.

## Property access

Use the Property API to provide optimized access for dynamic configuration.  Access to dynamic configuration
usually follows two access patterns.  The first is to react to changes in property values (similar to pub-sub).
The second is getting the most up to date value of a property, such as buffer size limits, in a hot execution path.
The Property implementation in archaius reacts to configuration changes and caches the resolved configuration
so that the hot execution path accesses the cache instead of constantly resolving and interpolating values.

To create a Property object

```java
Property<Integer> timeout = configManager.observe("server.timeout").asInteger(DEFAULT_TIMEOUT_VALUE);
```

To access the cached property value
```java
socket.setReadTimeout(timeout.get());
```

To react to property change notification

```java
Property<Integer> timeout = configManager.observe("server.timeout").asInteger(DEFAULT_TIMEOUT_VALUE, 
    new PropertyObserver<Integer>() {
        public void onChange(Integer value) {
            socket.setReadTimeout(value);
        }
        
        public void onError(Throwable error) {
        }
    });
```

Note that before asInteger returns the PropertyObserver will be call with the current value of the property
or the specified default if the property does not existing in the configuration.

# Setting property override from code

In addition to sourced DynamicConfig and static file based configurations Archaius offers a mechanism to 
manually set runtime overrides.  

The example below shows an override layer being added as the first Config in ConfigManager so that any
overrides take precedence.  

```java
CompositeConfig manager = CompositeConfig.builder() 
    .addConfig(overrides = new SimpleDynamicConfig())
    .addConfig(new PollingDynamicConfg(
            "REMOTE", 
            new URLConfigReader("http://remoteconfigservice/snapshot"), 
            new FixedPollingStrategy(30, TimeUnit.SECONDS)) 
    .addConfig(application = new CompositeConfig())
    .addConfig(library = new CompositeConfig())
    .addConfig(new SystemConfig()) 
    .addConfig(new EnvironmentConfig())
    .build();
```

To set an override
```java
overrides.setProperty("service.timeout", 100);
```

