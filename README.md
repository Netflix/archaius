Archaius is a configuration library for accessing a mixture of static as well
as dynamic configurations as a single configuration unit.

## Features
* Lock-free reads
* Non-static
* DI friendly

## 2.x Changes
* **Not backwards compatible**
* Clean separation of API and backing configurations (i.e. commons-configuration, typesafe-configuration, etc)
* Minimal external dependencies
* Improved bootstrapping process that doesn't rely on class loading

## Getting Started

Archaius provides a set of specialized configuration classes that may be combined
using CompositeConfig into a specific override structure.  For convenience you may 
also use DefaultAppConfig as a best practices combination of runtime overrides,
remote configuration, environment, system, application and library configuration 
levels.  All access to the configuration in application should go through an instance 
of AppConfig.  The remainder of this document describes the DefaultAppConfig API.  

```java
// Create the config.  This will load configuration from config.properties
AppConfig config = DefaultAppConfig.createDefault();

// Read a property
config.getString("propertyname");

// Load configuration into the libraries level
config.addConfig(config.newLoader().load("library");

// Get a property
Property<String> prop = config.createProperty("propertyname").asString();

```

## Accessing configuration

All Config derived classes provide access to their underlying configuration via the numerous 
getString(), getInt(), getBoolean() methods.  In additional to basic primitives and collections
Config will allow parsing to any type that has a constructor that takes a single String 
argument or a static valueOf(String.class) method.  

## Replacements

Archaius supports standard variable replacement syntax such as ${other.property.name}.  Note that
once added to AppConfig replacement values may exist in any of the configurations and are
resolved in first seen order.  

## Configuration loaders

Archaius has a default built in loader for .properties files but can also be extended with custom
property specifications such as HOCON.  In addition multiple contextual overrides for a single 
configuration resource name may be derived using a CascadeStrategy.  The strategy may also specify
replacements from already loaded configurations (such as System and Environment properties).

For example, the following cascade policy will attempt to load the following permuatations in addition
to the base resource name, basename: basename-${env}, basename-${env}-${region}.  

``` java
AppConfig appConfig = DefaultAppConfig.builder()
    .withConfigLoader(new TypesafeLoader())
    .withDefaultCascadeStrategy(new ConcatCascadeStrategy("${env}", "${region}"))
    ...
```

## Dynamic Properties

One of the core differentiators between Archaius and other configuration libraries
is its support for dynamically changing configuration.  Tranditionaly applications 
require a restart whenever configuration changes.  This can result in unncessary 
service interruption during minor configuration changes, such as timeout values.  Through
Archaius, code can have direct access to the most recent configuration without the need to 
restart.  In addition, code can react to configuration changes by registering a change
handler.  

Dynamic configuration support is split into the configuration loading and property
value resolution.

### Configuration loading

When adding a DynamicConfig derived configuration AppConfig will automatically register for
configuration change notifications and incorporate new values into the main configuration.  
Archaius provides a base PollingDynamicConfig for use with configuration sources that are
polled frequently to refresh the entire configuration snapshot.  Implement DynamicConfig 
directly for fine grained configuration sources, such as ZooKeeper, which support notification 
on a per property granularity.

```java
appConfig.addConfig(new PollingDynamicConfg(
            "REMOTE", 
            new URLConfigReader("http://remoteconfigservice/snapshot"), 
            new FixedPollingStrategy(30, TimeUnit.SECONDS)) 
```

### Property access

Use the Property API for optimized access to any property that is expected to be updated at
runtime.  Access to dynamic configuration follows two access patterns.  The first (and most common)
is to get the most recent value.  The second, and more advanced access pattern is to react
to property value changes via a PropertyObserver. 

To create a Property object

```java
Property<Integer> timeout = appConfig.observeProperty("server.timeout").asInteger(DEFAULT_TIMEOUT_VALUE);
```

To access the cached property value
```java
Thread.sleep(timeout.get(DEFAULT_TIMEOUT_VALUE));
```

To react to property change notification

```java
Property<Integer> timeout = appConfig
    .observeProperty("server.timeout")
    .asInteger() 
    .addObserver(new PropertyObserver<Integer>() {
        public void onChange(Integer value) {
            socket.setReadTimeout(value);
        }
        
        public void onError(Throwable error) {
        }
    });
```

Note that before addObserver() returns the PropertyObserver will be called with the current value of the property
or null if the property does not exist in the configuration.

## Setting property override from code

In addition to sourced DynamicConfig and static file based configurations AppConfig offers a mechanism to 
manually set runtime overrides from code. 

The example below shows an override layer being added as the first Config in a CompositeConfig so that any
overrides take precedence.  Note that this structure is

To set an override
```java
appConfig.setProperty("service.timeout", 100);
```

### Integration with DI frameworks

The API discussed thus far implies that configuration is loaded directly through code.  This approach makes testing
difficult as it requires properties to be set in order to test code instead of using more power techniques
such as mocking.  Archaius-core provides a set of classes and annotations to be integrated into DI frameworks
so that configuration may be mapped to object at creation type as part of lifecycle management.  There is also
a feature to create dynamic proxies from configuration annotated classes.

### Injecting config,

```java
public class MyService {
    private final int timeout;

    @Inject
    public MyService(Config config) {
        timeout = config.getInteger("prefix.timeout");
    }
}
```

### Configuration mapping

With configuration mapping the DI framework (such as Guice) will 'inject' the configuration into the class 
after the class has been instantiated.  When using Governator configuration will be injected prior to 
@PostConstruct being invoked.  Outside of governator a postConfig method can be specified on the 
@Configuration annotation.  Tighter integration with DI frameworks other than Guice can make sure configuration
is mapped before @PostConstruct is called.

```java
@Configuration(prefix="prefix", postConfig="postConfig")
public class MyService {
    private int timeout = 1000;
    
    @Inject
    public MyService() {
    }

    // Will be set from property 'prefix.timeout'
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    private void postConfig() {
    }
}
```

### Configuration proxy

The simplest approach to configuration mapping is to use the proxy feature where an annotated interface 
can proxy directly to fast properties.

```java
@Singleton
public class MyService {
    @Inject
    public MyService(MyConfig config) {
    }
}

@Configuration(prefix="prefix")
public interface MyConfig {
    @DefaultValue("10000")
    int getTimeout();
}
```

When using archaius-guice create a binding for the proxy interface like this (a more terse API will be provided later)

```java
new AbstractModule() {
    protected void configure() {
        bind(MyConfig.class).toProvider(Providers.guicify(ArchaiusModule.forProxy(MyConfig.class));
    }
}
```




