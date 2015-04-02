package org.deadsimple.mundungus.exception;

public class MappingException extends RuntimeException {
    public MappingException(final String message) {
       super(message);
    }
    
    public MappingException(final Exception e) {
        super(e);
    }
}
