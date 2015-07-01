package org.deadsimple.mundungus.exception;

public class MappingException extends RuntimeException {
    public MappingException(final String message) {
       super(message);
    }
    
    public MappingException(final Exception e) {
        super(e);
    }

    public MappingException(String fieldName, Class clazz, Exception e) {
        super("Error mapping field: " + fieldName + " on object type " + clazz.getName(), e);
    }

    public MappingException(Class clazz, Exception e) {
        super("Error instantiating class: " + clazz.getName(), e);
    }

    public MappingException(String message, Exception e) {
        super(message, e);
    }
}
