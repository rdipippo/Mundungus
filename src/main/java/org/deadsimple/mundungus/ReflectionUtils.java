package org.deadsimple.mundungus;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.lang3.ClassUtils;
import org.bson.types.ObjectId;
import org.deadsimple.mundungus.annotations.Collection;
import org.deadsimple.mundungus.annotations.LoadType;
import org.deadsimple.mundungus.annotations.Reference;
import org.deadsimple.mundungus.annotations.Transient;
import org.deadsimple.mundungus.exception.MappingException;

import java.beans.Introspector;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class ReflectionUtils {
   private static final String TYPE_FIELD = "_type";

   private static final CacheManager cm = CacheManager.getInstance();

   static {
       cm.addCache("ObjectIds");
   }

   static boolean isGetter(final Method m) {
       final String methodName = m.getName();
       return (methodName.startsWith("get") || methodName.startsWith("is"))
               && ! isTransient(m)
               && !methodName.equals("getHandler") && !methodName.equals("getClass") && !m.getDeclaringClass().getName().equals("java.lang.Enum");
   }

   static boolean isSetter(final Method m) {
      final String methodName = m.getName();
      return methodName.startsWith("set") && ! isTransient(m) && !methodName.equals("setHandler");
   }

   static boolean isTransient(Class clazz, String fieldName) {
       if (fieldName.equals(EntityManager.ID_FIELD_NAME)) {
           return false;
       }

       Field f = null;
       try {
           f = clazz.getDeclaredField(fieldName);
       } catch (NoSuchFieldException e) {
           try {
               f = clazz.getSuperclass().getDeclaredField(fieldName);
           } catch (NoSuchFieldException e1) {
               return true;
           }
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
   
   static <T> T mapDBOToJavaObject(final BasicDBObject dbo) throws MappingException {
       String fieldName = null;
       Object newInstance;

       String type = (String)dbo.get(TYPE_FIELD);
       Class<?> clazz = null;

       try {
           clazz = Class.forName(type);
       } catch (ClassNotFoundException e) {
           e.printStackTrace();
       }

       try {
           final Method[] methodDescriptors = clazz.getMethods();
           if (ProxyObject.class.isAssignableFrom(clazz)) {
               newInstance = clazz.newInstance();
           } else {
               ProxyFactory factory = new ProxyFactory();
               factory.setSuperclass(clazz);
               newInstance = factory.createClass().newInstance();

               MethodHandler handler = new MethodHandler() {
                   public Object invoke(Object self, Method overridden, Method forwarder,
                                        Object[] args) throws Throwable {
                       final Reference reference = overridden.getAnnotation(Reference.class);
                       if (reference != null && reference.loadType().equals(LoadType.LAZY)) {
                           String fieldName = ReflectionUtils.getFieldName(overridden);
                           final ObjectId oid = (ObjectId) dbo.get(fieldName);

                           if (cm.getCache("ObjectIds").isKeyInCache(oid)) {
                               Element el = cm.getCache("ObjectIds").get(oid);
                               return el.getObjectValue();
                           } else {
                               EntityManager em = new EntityManager();
                               final Object o = em.find(overridden.getReturnType(), oid);
                               cm.getCache("ObjectIds").put(new Element(oid, o));
                               return o;
                           }
                       }
                       return forwarder.invoke(self, args);
                   }
               };

               ((ProxyObject) newInstance).setHandler(handler);
           }

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
                               BasicDBObject subDBO = (BasicDBObject)dbo.get(fieldName);

                               final Object obj = mapDBOToJavaObject((BasicDBObject) dbo.get(fieldName));
                               md.invoke(newInstance, obj);
                           } else {
                               if (dbo.get(fieldName) instanceof ObjectId) {
                                   // check if object id should be eagerly loaded
                                   Method getter = getGetter(md);
                                   final Reference annotation = getter.getAnnotation(Reference.class);

                                   if (annotation != null && annotation.loadType().equals(LoadType.EAGER)) {
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
               }
           }

           return (T)newInstance;
       } catch (IllegalAccessException e) {
           throw new MappingException(fieldName, clazz, e);
       } catch (InstantiationException e) {
           throw new MappingException(clazz, e);
       } catch (InvocationTargetException e) {
           throw new MappingException(fieldName, clazz,  e);
       } catch (IllegalArgumentException e) {
           throw new MappingException(fieldName, clazz,  e);
       }
   }

   static Method getGetter(Method setter) {
       String getterName = setter.getName().replaceFirst("s", "g");

       try {
           return setter.getDeclaringClass().getMethod(getterName, null);
       } catch (NoSuchMethodException e) {
           throw new MappingException("Could not find getter method " + getterName + " corresponding to setter " + setter.getName(), e);
       }
   }

   static List mapDBListToJavaList(final BasicDBList list) throws MappingException {
	   final List retList = new ArrayList();
	   final Iterator iter = list.iterator();
	   while(iter.hasNext()) {
		   final Object obj = iter.next();
		   if (obj instanceof BasicDBObject) {
               retList.add(mapDBOToJavaObject((BasicDBObject)obj));
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
               if (ProxyObject.class.isAssignableFrom(obj.getClass())) {
                   listMemberDBO.put(TYPE_FIELD, obj.getClass().getSuperclass().getName());
               } else {
                   listMemberDBO.put(TYPE_FIELD, obj.getClass().getName());
               }

               dbo.add(listMemberDBO);
           }
       }

       return dbo;
   }

   public static BasicDBObject mapJavaObjectToDBO(final Object obj) throws MappingException {
	   Method[] methodDescriptors = null;

       if (obj instanceof ProxyObject) {
           methodDescriptors = obj.getClass().getSuperclass().getMethods();
       } else {
           methodDescriptors = obj.getClass().getMethods();
       }

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
                       throw new MappingException(fieldName, obj.getClass(), e);
                   }

                   final Class<?> parameterClazz = md.getReturnType();

                   if (md.getAnnotation(Reference.class) != null && val != null) {
                       Method idGetter = null;
                       try {
                           idGetter = val.getClass().getMethod("getId");
                           ObjectId id = (ObjectId)idGetter.invoke(val, null);

                           if (id != null) {
                               dbo.put(fieldName, id);
                           }
                       } catch (NoSuchMethodException e) {
                          throw new MappingException("No getId method on object of type " + obj.getClass().getSimpleName(), e);
                       } catch (InvocationTargetException e) {
                           e.printStackTrace();
                       } catch (IllegalAccessException e) {
                           e.printStackTrace();
                       }
                   } else if (val instanceof List) {
                       try {
                           final BasicDBList listDBO = ReflectionUtils.mapJavaListToDBList((List) val);
                           dbo.put(fieldName, listDBO);
                       } catch (Exception e) {
                           throw new MappingException(fieldName, obj.getClass(), e);
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

       if (ProxyObject.class.isAssignableFrom(obj.getClass())) {
           dbo.put(TYPE_FIELD, obj.getClass().getSuperclass().getName());
       } else {
           dbo.put(TYPE_FIELD, obj.getClass().getName());
       }

	   return dbo;
   }
   
   public static String mapClassNameToCollectionName(Class clazz) {
       if (ProxyObject.class.isAssignableFrom(clazz)) {
           clazz = clazz.getSuperclass();
       }

       if (clazz.getAnnotation(Collection.class) == null) {
           clazz = clazz.getSuperclass();
       }

       Collection annotation = (Collection)clazz.getAnnotation(Collection.class);

       String userSpecifiedName = annotation.name();

       return userSpecifiedName.isEmpty() ? clazz.getSimpleName() : userSpecifiedName;
   }

   public static boolean isCollection(Class clazz) {
       if (ProxyObject.class.isAssignableFrom(clazz)) {
           return clazz.getSuperclass().getAnnotation(Collection.class) != null;
       } else {
           return clazz.getAnnotation(Collection.class) != null;
       }
   }
}
