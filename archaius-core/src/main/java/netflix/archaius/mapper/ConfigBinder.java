package netflix.archaius.mapper;

import netflix.archaius.exceptions.MappingException;

public interface ConfigBinder {

    <T> void bindConfig(T injectee, IoCContainer ioc) throws MappingException;
    
    <T> void bindConfig(T injectee) throws MappingException;

}
