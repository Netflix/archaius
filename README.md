Archaius
=====

Features
-------

Archaius includes a set of configuration management APIs used by Netflix. It provides the following functionalities:

* Dynamic, Typed Properties
* High throughput and Thread Safe Configuration operations
* A polling framework that allows obtaining property changes of a Configuration Source
* A Callback mechanism that gets invoked on effective/"winning" property mutations (in the ordered hierarchy of Configurations)
* A JMX MBean that can be accessed via JConsole to inspect and invoke operations on properties
* Out of the box, Composite Configurations (With ordered hierarchy) for applications (and most web applications willing to use convention based property file locations)
* Implementations of dynamic configuration sources for URLs, JDBC and Amazon DynamoDB
* Scala dynamic property wrappers

Documentation
--------------
Please see [wiki] (https://github.com/Netflix/archaius/wiki) for detail documentations.

Origin
------
The code name for the project comes from an endangered species of Chameleons. We chose [Archaius](http://en.wikipedia.org/wiki/Archaius), as Chameleons are known for changing their color (a property) based on its environment and situation. This project was borne out of a strong desire to use dynamic property changes to effect runtime behaviors based on specific contexts.