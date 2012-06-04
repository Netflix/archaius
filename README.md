Pablo
=====

Pablo includes a set of configuration management APIs used by Netflix. It provides the following functionalities:

* Allow configurations to change dynamically at runtime. This enable production systems to take configuration changes without having to restart.
* Add a cache layer that contains desired properties on top of the configuration to provide typed, fast and thread-safe access to the property. 
* Create configuration composed of sub-configurations and determine the final property value in a simple, fast and thread-safe manner.
