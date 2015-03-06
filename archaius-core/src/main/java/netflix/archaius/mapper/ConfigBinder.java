package netflix.archaius.mapper;

import netflix.archaius.exceptions.MappingException;

public interface ConfigBinder {

    void bindConfig(Object injectee) throws MappingException;

}
