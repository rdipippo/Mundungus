package org.deadsimple.mundungus.exception;

public class MappingException extends RuntimeException {
    public MappingException(final String message) {
       super(message);
    }
    
    public MappingException(final Exception e) {
        super(e);
    }

    public MappingException(String className, String fieldName, Exception e) {
        super("Error mapping field: " + fieldName + " on object type " + className, e);
    }

    public MappingException(String className, Exception e) {
        super("Error instantiating class: " + className, e);
    }
}
