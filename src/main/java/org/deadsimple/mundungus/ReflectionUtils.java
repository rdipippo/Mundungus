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

class ReflectionUtils {
   static boolean isGetter(final Method m) {
      final String methodName = m.getName();
      return (methodName.startsWith("get") || methodName.startsWith("is"))
              && ! methodName.equals("getClass") && ! m.getDeclaringClass().getName().equals("java.lang.Enum");
   }

   static boolean isSetter(final Method m) {
      final String methodName = m.getName();
      return methodName.startsWith("set");
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
   
   static <T> T mapDBOToJavaObject(final Class<T> clazz, final BasicDBObject dbo) throws InvocationTargetException, IllegalAccessException, NoSuchFieldException, InstantiationException {
	   final Method [] methodDescriptors = clazz.getMethods();
	   final T newInstance = clazz.newInstance();
	   
	   for (final Method md : methodDescriptors) {
		   if (isSetter(md)) {
			   final String fieldName = ReflectionUtils.getFieldName(md);
			   final Class<?> parameterClazz = md.getParameterTypes()[0];

			   if (fieldName.equals(EntityManager.ID_FIELD_NAME)) {
				   final ObjectId oid = (ObjectId)dbo.get(fieldName);
				   md.invoke(newInstance, oid);
			   } else if (parameterClazz.isAssignableFrom(List.class)) {
				   final Object listObj = dbo.get(fieldName);
				   
				   if (listObj != null) {
					   final Field field = clazz.getDeclaredField(fieldName);
					   field.setAccessible(true);
					   final SubCollection a = field.getAnnotation(SubCollection.class);
					   md.invoke(newInstance, mapDBListToJavaList(a.value(), (BasicDBList)dbo.get(fieldName)));
				   }
			   } else if (ClassUtils.isPrimitiveOrWrapper(parameterClazz) || parameterClazz.equals(String.class) || parameterClazz.equals(ObjectId.class)) {
			       md.invoke(newInstance, dbo.get(fieldName));
			   } else if (parameterClazz.isEnum()) {
                   final BasicDBObject enumDBO = (BasicDBObject)dbo.get(fieldName);
                   md.invoke(newInstance, Enum.valueOf((Class<Enum>)parameterClazz, (String)enumDBO.get("value")));
               } else {
			       if (dbo.get(fieldName) instanceof BasicDBObject) {
			           final Object obj = mapDBOToJavaObject(parameterClazz, (BasicDBObject)dbo.get(fieldName));
			           md.invoke(newInstance, obj);
			       } else if (dbo.get(fieldName) instanceof ObjectId) {
			           final ObjectId o = (ObjectId)dbo.get(fieldName);
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
	   
	   return newInstance;
   }
   
   static List mapDBListToJavaList(final Class clazz, final BasicDBList list) throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchFieldException {
	   final List retList = new ArrayList();
	   final Iterator iter = list.iterator();
	   while(iter.hasNext()) {
		   final Object obj = iter.next();
		   if (obj instanceof BasicDBObject) {
			   retList.add(mapDBOToJavaObject(clazz, (BasicDBObject)obj));
		   } else {
		       retList.add(obj.toString());
		   }
	   }
	   
	   return retList;
   }

   public static BasicDBList mapJavaListToDBList(final List val) throws InvocationTargetException, IllegalAccessException {
	   final BasicDBList dbo = new BasicDBList();
	   
	   final Iterator iter = val.iterator();
	   
	   while(iter.hasNext()) {
          final Object obj = iter.next();
          final Class clazz = obj.getClass();
          
          if (ClassUtils.isPrimitiveOrWrapper(clazz) || clazz.equals(String.class) || clazz.equals(ObjectId.class)) {
              dbo.add(obj);
          } else {
             dbo.add(mapJavaObjectToDBO(obj));
          }
	   }

	   return dbo;
   }

   public static BasicDBObject mapJavaObjectToDBO(final Object obj) throws InvocationTargetException, IllegalAccessException {
	   final Method[] methodDescriptors = obj.getClass().getMethods();
	   final BasicDBObject dbo = new BasicDBObject();
	   
	   for (final Method md : methodDescriptors) {
		   if (ReflectionUtils.isGetter(md)) {
			   final String fieldName = ReflectionUtils.getFieldName(md);

			   final Object val = md.invoke(obj, (Object [])null);
			   final Class<?> parameterClazz = md.getReturnType();
			   			   
			   if (val instanceof List) {
				   final BasicDBList listDBO = ReflectionUtils.mapJavaListToDBList((List)val);
				   dbo.put(fieldName, listDBO);
			   } else if (ClassUtils.isPrimitiveOrWrapper(parameterClazz) || parameterClazz.equals(String.class) || parameterClazz.equals(ObjectId.class)) {
                   if (val != null) {
                       dbo.put(fieldName, val);
                   }
			   } else if (val instanceof java.lang.Enum) {
                   Enum e = (Enum)val;
                   BasicDBObject enumDBO = new BasicDBObject();
                   enumDBO.put("ordinal", e.ordinal());
                   enumDBO.put("value", e.name());
                   dbo.put(fieldName, enumDBO);
               } else if (val != null) {
                   dbo.put(fieldName, mapJavaObjectToDBO(val));
               }
		   }
	   }

	   return dbo;
   }
   
   public static String mapClassNameToCollectionName(final Class clazz) {
       return clazz.getSimpleName().toLowerCase();
   }
}
