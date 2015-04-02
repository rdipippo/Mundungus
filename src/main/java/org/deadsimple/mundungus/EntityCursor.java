package org.deadsimple.mundungus;

import java.lang.reflect.InvocationTargetException;
import org.deadsimple.mundungus.exception.MappingException;
import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;

public class EntityCursor<T> {
   private Class<T> clazz;
    
   private Cursor cursor;
        
   public EntityCursor(final Cursor cursorArg, final Class<T> clazzArg) {
      this.cursor = cursorArg;
      this.clazz = clazzArg;
   }

   public T nextEntity() {
       try {
           return ReflectionUtils.mapDBOToJavaObject(this.clazz, (BasicDBObject) this.cursor.next());
       } catch (final InvocationTargetException e) {
           throw new MappingException(e);
       } catch (final IllegalAccessException e) {
           throw new MappingException(e);
       } catch (final NoSuchFieldException e) {
           throw new MappingException(e);
       } catch (final InstantiationException e) {
           throw new MappingException(e);
       }
   }
   
   public boolean hasNext() {
	   return this.cursor.hasNext();
   }
}
