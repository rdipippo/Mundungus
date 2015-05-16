package org.deadsimple.mundungus;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.ClassUtils;
import org.bson.types.ObjectId;
import org.deadsimple.mundungus.annotations.SubCollection;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.deadsimple.mundungus.annotations.Transient;
import org.deadsimple.mundungus.exception.MappingException;

class ReflectionUtils {
   private static final String TYPE_FIELD = "_type";

   static boolean isGetter(final Method m) {
       final String methodName = m.getName();
       return (methodName.startsWith("get") || methodName.startsWith("is"))
               && ! isTransient(m)
               && !methodName.equals("getClass") && !m.getDeclaringClass().getName().equals("java.lang.Enum");
   }

   static boolean isSetter(final Method m) {
      final String methodName = m.getName();
      return methodName.startsWith("set") && ! isTransient(m);
   }

   static boolean isTransient(Class clazz, String fieldName) {
       if (fieldName.equals(EntityManager.ID_FIELD_NAME)) {
           return false;
       }

       Field f = null;
       try {
           f = clazz.getDeclaredField(fieldName);
       } catch (NoSuchFieldException e) {
           return true;
       }
       return f.getAnnotation(Transient.class) != null;
   }

   static boolean isTransient(Method m) {
       return m.getAnnotation(Transient.class) != null;
   }

   static String getFieldName(final Method m) {
      int numChars = (m.getName().startsWith("get") || m.getName().startsWith("set")) ? 3 : 2;
      final String fieldName = Introspector.decapitalize(m.getName().substring(numChars));
      if (fieldName.equals("id")) {
         return EntityManager.ID_FIELD_NAME;
      } else {
         return fieldName;
      }
   }
   
   static <T> T mapDBOToJavaObject(final Class<T> clazz, final BasicDBObject dbo) throws MappingException {
       String fieldName = null;

       try {
           final Method[] methodDescriptors = clazz.getMethods();
           final T newInstance = clazz.newInstance();

           for (final Method md : methodDescriptors) {
               if (isSetter(md)) {
                   fieldName = ReflectionUtils.getFieldName(md);

                   if (! ReflectionUtils.isTransient(clazz, fieldName)) {
                       final Class<?> parameterClazz = md.getParameterTypes()[0];

                       if (fieldName.equals(EntityManager.ID_FIELD_NAME)) {
                           final ObjectId oid = (ObjectId) dbo.get(fieldName);
                           md.invoke(newInstance, oid);
                       } else if (parameterClazz.isAssignableFrom(List.class)) {
                           final Object listObj = dbo.get(fieldName);

                           if (listObj != null) {
                               md.invoke(newInstance, mapDBListToJavaList((BasicDBList) dbo.get(fieldName)));
                           }
                       } else if (ClassUtils.isPrimitiveOrWrapper(parameterClazz) || parameterClazz.equals(String.class) || parameterClazz.equals(ObjectId.class)) {

                           md.invoke(newInstance, dbo.get(fieldName));
                       } else if (parameterClazz.isEnum()) {
                           final BasicDBObject enumDBO = (BasicDBObject) dbo.get(fieldName);
                           if (enumDBO != null && enumDBO.get("value") != null) {
                               md.invoke(newInstance, Enum.valueOf((Class<Enum>) parameterClazz, (String) enumDBO.get("value")));
                           }
                       } else {
                           if (dbo.get(fieldName) instanceof BasicDBObject) {
                               final Object obj = mapDBOToJavaObject(parameterClazz, (BasicDBObject) dbo.get(fieldName));
                               md.invoke(newInstance, obj);
                           } else if (dbo.get(fieldName) instanceof ObjectId) {
                               final ObjectId o = (ObjectId) dbo.get(fieldName);
                               if (o != null) {
                                   List<ObjectId> list = new ArrayList<ObjectId>();
                                   list.add(o);
                                   final EntityCursor find = new EntityManager().find(parameterClazz, list);
                                   md.invoke(newInstance, find.nextEntity());
                               }
                           }
                       }
                   }
               }
           }

           return newInstance;
       } catch (IllegalAccessException e) {
           throw new MappingException(clazz.getName(), fieldName, e);
       } catch (InstantiationException e) {
           throw new MappingException(clazz.getName(), e);
       } catch (InvocationTargetException e) {
           throw new MappingException(clazz.getName(), fieldName, e);
       } catch (IllegalArgumentException e) {
           throw new MappingException(clazz.getName(), fieldName, e);
       }
   }
   
   static List mapDBListToJavaList(final BasicDBList list) throws MappingException {
	   final List retList = new ArrayList();
	   final Iterator iter = list.iterator();
	   while(iter.hasNext()) {
		   final Object obj = iter.next();
		   if (obj instanceof BasicDBObject) {
               String type = (String) ((BasicDBObject) obj).get(TYPE_FIELD);
               try {
                   Class<?> aClass = Class.forName(type);
                   retList.add(mapDBOToJavaObject(aClass, (BasicDBObject)obj));
               } catch (ClassNotFoundException e) {
                   e.printStackTrace();
               }
		   } else {
		       retList.add(obj.toString());
		   }
	   }
	   
	   return retList;
   }

   public static BasicDBList mapJavaListToDBList(final List val) throws MappingException {
       final BasicDBList dbo = new BasicDBList();

       final Iterator iter = val.iterator();

       while (iter.hasNext()) {
           final Object obj = iter.next();
           final Class clazz = obj.getClass();

           if (ClassUtils.isPrimitiveOrWrapper(clazz) || clazz.equals(String.class) || clazz.equals(ObjectId.class)) {
               dbo.add(obj);
           } else {
               BasicDBObject listMemberDBO = mapJavaObjectToDBO(obj);
               listMemberDBO.put(TYPE_FIELD, obj.getClass().getName());
               dbo.add(listMemberDBO);
           }
       }

       return dbo;
   }

   public static BasicDBObject mapJavaObjectToDBO(final Object obj) throws MappingException {
	   final Method[] methodDescriptors = obj.getClass().getMethods();
	   final BasicDBObject dbo = new BasicDBObject();
	   String fieldName = null;
       Object val = null;

	   for (final Method md : methodDescriptors) {
		   if (ReflectionUtils.isGetter(md)) {
			   fieldName = ReflectionUtils.getFieldName(md);

               if (! ReflectionUtils.isTransient(obj.getClass(), fieldName)) {
                   try {
                       val = md.invoke(obj, (Object[]) null);
                   } catch (Exception e) {
                       throw new MappingException(fieldName, obj.getClass().getName(), e);
                   }

                   final Class<?> parameterClazz = md.getReturnType();

                   if (val instanceof List) {
                       try {
                           final BasicDBList listDBO = ReflectionUtils.mapJavaListToDBList((List) val);
                           dbo.put(fieldName, listDBO);
                       } catch (Exception e) {
                           throw new MappingException(fieldName, obj.getClass().getName(), e);
                       }
                   } else if (ClassUtils.isPrimitiveOrWrapper(parameterClazz) || parameterClazz.equals(String.class) || parameterClazz.equals(ObjectId.class)) {
                       if (val != null) {
                           dbo.put(fieldName, val);
                       }
                   } else if (val instanceof Enum) {
                       Enum e = (Enum) val;
                       BasicDBObject enumDBO = new BasicDBObject();
                       enumDBO.put("ordinal", e.ordinal());
                       enumDBO.put("value", e.name());
                       dbo.put(fieldName, enumDBO);
                   } else if (val != null) {
                       dbo.put(fieldName, mapJavaObjectToDBO(val));
                   }
               }
           }
	   }

	   return dbo;
   }
   
   public static String mapClassNameToCollectionName(final Class clazz) {
       return clazz.getSimpleName().toLowerCase();
   }
}
