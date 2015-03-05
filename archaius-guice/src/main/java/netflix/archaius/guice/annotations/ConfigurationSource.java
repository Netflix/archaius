package netflix.archaius.guice.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Identifier for a configuration source as well as a customizable policy for
 * loading cascaded (or different name variations) of the source.
 * 
 * {@code
 * @ConfigurationSource(value="foo")
 * class Foo {
 * 
 * }
 * @author elandau
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigurationSource {
    /**
     * List of named sources to load.  This could be a simple name, like 'foo' that is resolved by the 
     * property loaders or including a type, 'properties:foo.properties'.
     * 
     * @return
     */
    String[] value();

    /**
     * Policy for creating variations of the configuration source names to be loaded.
     */
//    Class<? extends CascadePolicy> cascading = null;
}
