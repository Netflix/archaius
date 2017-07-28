## Overview

This module provides a bridge from Archaius 0.x to Archaius 2 so that the Archaius 0.x
API will continue to function as expected but will be backed by Archaius 2.  The bridge
allows configurations loaded into both the legacy and new API to be accessible via both
APIs.  

## Getting Started

Note that bridging Archaius 0.x and Archaius 2 uses 'advanced' binding features of Guice.  
It should however be possible to implement this bridge for other DI frameworks as well.

To enable the bridge simply install StaticArchaiusBridgeModule in addition to ArchaiusModule 
from Archaius 2.

```java
Injector injector = Guice.createInjector(
    new ArchaiusModule(),
    new StaticArchaiusBridgeModule()
)
```

## How does it work

The bridge calls ConfigurationManager.install() to configure the legacy API with an AbstractConfiguration implementation that bridges the static API with the Guice created Archaius2 top level Config binding.  Note that this implementation can only work if ConfigurationManager is allowed to bootstrap itself with the default AbstractConfiguration instance.  The bridge is instantiated via static injection which occurs very early in the Guice bootstrapping process, specifically before most singletons are created.  

## Troubleshooting

The static bridge can be a bit fragile as it tries to 'fix' the static usage pattern of the old API with dependency injection.  As such static access of ConfigurationManager before Guice has finished bootstrapping will result in an IllegalStateException.  

### IllegalStateException("Archaius2 bridge not usable because ConfigurationManager was initialized too early.  See stack trace below.")

This happens when the legacy ConfigurationManager is accessed before StaticArchaiusBridgeModule is installed and the necessary System properties set to enable this bridge.  This is most likely the result of either calling ConfigurationManager.getInstance() in a Guice module or creating objects in a Guice module's configure method.  Take a look at the accompanying stack trace to identify areas in your code where one of the above is done.  Make sure to replace usages of toInstance() bindings with @Provides method.  For example,

Instead of
```java
public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Foo.class).toInstance(new FooImpl());
    }
}
```

Do,
```java
public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
    }
    
    @Provides
    public Foo getFoo() {
        return new FooImpl());
    }
}
```

### IllegalStateException("Not using expected bridge!!! ...)

This happens when multiple Guice injectors are created in the same JVM.  Due to the nature of the static bridge the first injector installing the StaticArchaiusBridgeModule will set up ConfigurationManager's static state which cannot be changed.  The error indicates that a second attempt to set up the static bridge failed.  

To solve this make sure to fork the JVM for each test like so,

```groovy
test {
    forkEvery = 1
}

```

Note that if this error occurs in a custom test task you will need to specify a similar block for each task
```groovy
smokeTest {
    forkEvery = 1
}
```

Also, do not create multiple injectors in the same test class.  Either create one injector for all your test methods or split the tests into multiple classes such that each class has exactly one injector.
