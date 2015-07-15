package org.deadsimple.mundungus;

import java.lang.reflect.InvocationTargetException;

import javassist.util.proxy.ProxyObject;
import org.deadsimple.mundungus.exception.MappingException;
import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;

public class EntityCursor<T> {
   private Class<T> clazz;
    
   private Cursor cursor;
        
   public EntityCursor(final Cursor cursorArg) {
      this.cursor = cursorArg;
   }

   public T nextEntity() throws MappingException {
       return ReflectionUtils.mapDBOToJavaObject((BasicDBObject) this.cursor.next());
   }
   
   public boolean hasNext() {
	   return this.cursor.hasNext();
   }
}
