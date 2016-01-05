Archaius is a configuration library for accessing a mixture of static as well
as dynamic configurations as a single configuration unit. 

There are two key concepts:
1. Properties can be read by your code.
2. Configurations organize properties into objects you can bootstrap your application with.

## Features
* Lock-free property reads.
* Dependency Injection (i.e., Guice) friendly so you don't have to rely on static code execution.

## 2.x Changes
* **Not backwards compatible**
* Clean separation of API and backing configurations (i.e. commons-configuration, 
typesafe-configuration, etc).
* Minimal external dependencies.
* Improved bootstrapping process that doesn't rely on class loading.

## Getting Started

Archaius provides a set of specialized configuration classes that may be combined
using `com.netflix.archaius.api.config.CompositeConfig` into a specific override structure.  
Check out `com.netflix.archaius.ProxyFactoryTest` (under `archaius2-core/test/java/`) for an 
example on how to bootstrap a config and access dynamic properties from it.

## Accessing configuration

All `com.netflix.archaius.api.Config` (under `archaius2-api/`) derived classes provide access to 
their underlying configuration via the numerous `getString()`, `getInt()`, `getBoolean()` methods.  
In addition to basic primitives and collections `Config` will allow parsing to any type that has a 
constructor that takes a single String argument or a `static valueOf(String.class)` method.  

## Replacements

Archaius supports standard variable replacement syntax such as `${other.property.name}`.   

## Configuration loaders

Archaius has a default built in loader for `.properties` files but can also be extended with custom
property specifications such as HOCON.  In addition multiple contextual overrides for a single 
configuration resource name may be derived using a `com.netflix.archaius.api.CascadeStrategy`.
The strategy may also specify replacements from already loaded configurations (such as System and
 Environment properties).

## Dynamic Properties

One of the core differentiators between Archaius and other configuration libraries
is its support for dynamically changing configuration.  Traditionally applications 
require a restart whenever configuration changes.  This can result in unncessary 
service interruption during minor configuration changes, such as timeout values.  Through
Archaius, code can have direct access to the most recent configuration without the need to 
restart.  In addition, code can react to configuration changes by registering a change
handler.  

Dynamic configuration support is split into the configuration loading and property
value resolution.

### Configuration loading

When adding a DynamicConfig derived configuration CompositeConfig will automatically register for
configuration change notifications and incorporate new values into the main configuration.  
Archaius provides a base PollingDynamicConfig for use with configuration sources that are
polled frequently to refresh the entire configuration snapshot.  Implement Config 
directly for fine grained configuration sources, such as ZooKeeper, which support updates 
at the individual property granularity.

```java
config.addConfig(new PollingDynamicConfg(
            "REMOTE", 
            new URLConfigReader("http://remoteconfigservice/snapshot"), 
            new FixedPollingStrategy(30, TimeUnit.SECONDS)) 
```

### Property access

Use the Property API for optimized access to any property that is expected to be updated at
runtime.  Access to dynamic configuration follows two access patterns.  The first (and most common)
is to get the most recent value directly from a Property object.  
Property optimizes caching of the resolved property value (from the hierarchy) and is much more 
efficient than calling the Config object for frequently accessed properties.  
The second, and more advanced, access pattern is to react to property value changes via a 
`com.netflix.archaius.api.PropertyListener`.  

To create a fast property factory using any Config as the source
```java
DefaultPropertyFactory factory = DefaultPropertyFactory.from(config);
```

To create a `com.netflix.archaius.api.Property` object

```java
Property<Integer> timeout = factory.getProperty("server.timeout").asInteger(DEFAULT_TIMEOUT_VALUE);
```

To access the cached property value
```java
Thread.sleep(timeout.get(DEFAULT_TIMEOUT_VALUE));
```

To react to property change notification

```java
Property<Integer> timeout = factory
    .getProperty("server.timeout")
    .asInteger() 
    .addListener(new PropertyListener<Integer>() {
        public void onChange(Integer value) {
            socket.setReadTimeout(value);
        }
        
        public void onError(Throwable error) {
        }
    });
```



