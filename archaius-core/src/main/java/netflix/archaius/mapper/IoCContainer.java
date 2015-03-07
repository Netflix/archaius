package netflix.archaius.mapper;

/**
 * Interface used by ConfigBinder to integrate with a DI framework that 
 * allows for named injection.  This integration enables binding a string
 * value for a type to a DI bound instance.
 * 
 * @author elandau
 *
 */
public interface IoCContainer {
    /**
     * @param name
     * @param type
     * @return Return the instance for type T bound to 'name'
     */
    public <T> T getInstance(String name, Class<T> type);
}
