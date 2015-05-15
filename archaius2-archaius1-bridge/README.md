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

The bridge calls ConfigurationManager.install() to configure the legacy API with an 
AbstractConfiguration implementation that bridges the static API with the Guice created 
Archaius2 top level Config binding.  Note that this implementation can only work if 
ConfigurationManager is allowed to bootstrap itself with the default AbstractConfiguration 
instance.  The bridge is instantiated via static injection which occurs very early in 
the Guice bootstrapping process, specifically before most singletons are created.  
