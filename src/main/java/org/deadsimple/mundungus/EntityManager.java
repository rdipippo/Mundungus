package org.deadsimple.mundungus;

import com.mongodb.*;
import com.mongodb.util.JSON;
import org.bson.types.ObjectId;
import org.deadsimple.mundungus.annotations.Collection;
import org.deadsimple.mundungus.annotations.MapKey;
import org.deadsimple.mundungus.exception.MappingException;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.MessageFormat;
import java.util.*;

public class EntityManager {
	private static final String MUNDUNGUS_PROPERTIES = "mundungus.properties";

	private static final String DB_PROPERTY = "database";

	private static final String HOST_PROPERTY = "host";

	static final String ID_FIELD_NAME = "_id";

	private MongoClient client = null;

	private DB database = null;
	
	public EntityManager() {
	    this(MUNDUNGUS_PROPERTIES);
	}
	
	@Override
	public void finalize() {
	    this.client.close();
	}
	
	public EntityManager(final String configFile) {
	    try {
            final Properties properties = new Properties();
            final InputStream propertiesStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(configFile);
            
            if (propertiesStream != null) {
               properties.load(propertiesStream);
            } else {
                throw new IllegalArgumentException("Could not find properties file " + configFile);
            }
            
            final String host = properties.getProperty(HOST_PROPERTY);
            this.validateHost(host);
            
            this.client = new MongoClient(host);
            this.database = this.client.getDB(properties.getProperty(DB_PROPERTY));
        } catch (final UnknownHostException e) {
            throw new IllegalArgumentException("Unknown host specified in " + configFile, e);
        } catch (final IOException e) {
            throw new IllegalArgumentException("Error loading " + configFile, e);
        }
	}

	public DB getDbConnection() {
		return this.database;
	}

    private void validateHost(final String host) throws UnknownHostException {
        InetAddress.getByName(host);
    }

	public ObjectId persist(final Object obj) {
	    final BasicDBObject dbo;

        final Collection annotation = obj.getClass().getAnnotation(Collection.class);
        if (annotation == null) {
            throw new MappingException(MessageFormat.format("Class {0} is not mapped by Mundungus.", obj.getClass().getSimpleName()));
        }

	    try {
            Method idSetter = obj.getClass().getMethod("setId", ObjectId.class);
            idSetter.invoke(obj, new ObjectId());
	        dbo = ReflectionUtils.mapJavaObjectToDBO(obj);
	    } catch(final IllegalAccessException e) {
	        throw new MappingException(e);
	    } catch (final InvocationTargetException e) {
	        throw new MappingException(e);
	    } catch (NoSuchMethodException e) {
            throw new MappingException(e);
        }

        final DB db = this.getDbConnection();
		db.getCollection(ReflectionUtils.mapClassNameToCollectionName(obj.getClass())).save(dbo);

		return (ObjectId)dbo.get(ID_FIELD_NAME);
	}

    public <S> List<S> get(final List<S> searchList) {
        final ArrayList<S> retList = new ArrayList<S>();

        for (S item : searchList) {
            retList.add(get(item).nextEntity());
        }

        return retList;
    }

	@SuppressWarnings("unchecked")
	public <T> EntityCursor<T> get(T searchInstance) {
		final DB db = this.getDbConnection();
		final Class<T> searchInstanceClass = (Class<T>)searchInstance.getClass();
		DBCollection collection = null;

		final Collection annotation = searchInstanceClass.getAnnotation(Collection.class);
		if (annotation != null) {
			collection = db.getCollection(ReflectionUtils.mapClassNameToCollectionName(searchInstanceClass));
		} else {
		    throw new MappingException(MessageFormat.format("Class {0} is not mapped by Mundungus.", searchInstance.getClass().getSimpleName()));
		}

        BasicDBObject dbo = null;

        try {
            dbo = ReflectionUtils.mapJavaObjectToDBO(searchInstance);
        } catch (final IllegalArgumentException e) {
            throw new MappingException(e);
        }

		DBCursor cursor = null;

		cursor = collection.find(dbo);
		return new EntityCursor<T>(cursor, searchInstanceClass);
	}
	
	public <T> EntityCursor<T> findAll(final Class<T> clazz) {
	    final Collection annotation = clazz.getAnnotation(Collection.class);
        if (annotation != null) {
            final DBCollection collection = this.getDbConnection().getCollection(ReflectionUtils.mapClassNameToCollectionName(clazz));
            return new EntityCursor<T>(collection.find(), clazz);
        }
        
        throw new MappingException(MessageFormat.format("Class {0} is not mapped by Mundungus.", clazz.getClass().getSimpleName()));
	}
	
	public <S, T> Map<S, T> mapAll(final Class<T> clazz) {
	    final HashMap<S, T> map = new HashMap<S, T>();
	    final Collection annotation = clazz.getAnnotation(Collection.class);

        if (annotation != null) {
            final Field [] fields = clazz.getFields();
            Field keyField = null;
            
            for (final Field field : fields) {
                if (field.getAnnotation(MapKey.class) != null) {
                    keyField = field;
                    break;
                }
            }

            final DBCollection collection = this.getDbConnection().getCollection(ReflectionUtils.mapClassNameToCollectionName(clazz));
            final EntityCursor<T> cursor =  new EntityCursor<T>(collection.find(), clazz);
            
            while(cursor.hasNext()) {
                final T next = cursor.nextEntity();
                Field field;
                try {
                    field = next.getClass().getDeclaredField(keyField == null ?  "id" : keyField.getName());
                    field.setAccessible(true);
                    final S key = (S)field.get(next);
                    map.put(key, next);
                } catch (final SecurityException e) {
                    throw new MappingException(e);
                } catch (final NoSuchFieldException e) {
                    throw new MappingException(e);
                } catch (final IllegalArgumentException e) {
                    throw new MappingException(e);
                } catch (final IllegalAccessException e) {
                    throw new MappingException(e);
                }
            }
        }
        
	    return map;
	}

	public <T> EntityCursor<T> find(final Class<T> clazz, final List<ObjectId> ids) {
		final Collection annotation = clazz.getAnnotation(Collection.class);
		if (annotation != null) {
			final DBCollection collection = this.getDbConnection().getCollection(ReflectionUtils.mapClassNameToCollectionName(clazz));
			final QueryBuilder qb = QueryBuilder.start(ID_FIELD_NAME);
			qb.in(ids);
			return new EntityCursor<T>(collection.find(qb.get()), clazz);
		}

		throw new MappingException(MessageFormat.format("Class {0} is not mapped by Mundungus.", clazz.getClass().getSimpleName()));
	}
	
	public <T> T find(final Class<T> clazz, final ObjectId id) {
        List<ObjectId> list = new ArrayList<ObjectId>();
        list.add(id);
	    final EntityCursor<T> cursor = this.find(clazz, list);

        if (cursor.hasNext()) {
            return cursor.nextEntity();
        } else {
            return null;
        }
	}
	
	public <T> EntityCursor<T> find(final Class<T> clazz, final String jsonQuery) {
	    final Collection annotation = clazz.getAnnotation(Collection.class);
        if (annotation != null) {
            final DBCollection collection = this.getDbConnection().getCollection(ReflectionUtils.mapClassNameToCollectionName(clazz));
            final DBObject queryObj = (DBObject)JSON.parse(jsonQuery);
            
            return new EntityCursor<T>(collection.find(queryObj), clazz);
        }

        throw new MappingException(MessageFormat.format("Class {0} is not mapped by Mundungus.", clazz.getClass().getSimpleName()));
	}
	
	public void remove(final Object document) {
	    final Class<?> clazz = document.getClass();
	    final Collection annotation = clazz.getAnnotation(Collection.class);
	    
	    if (annotation != null) {
            final BasicDBObject objectToRemove = ReflectionUtils.mapJavaObjectToDBO(document);
            this.getDbConnection().getCollection(ReflectionUtils.mapClassNameToCollectionName(clazz)).remove(objectToRemove);
        }
	}
	
	public <T> DBCollection getCollection(final Class<T> clazz) {
	    return this.getDbConnection().getCollection(ReflectionUtils.mapClassNameToCollectionName(clazz));
	}
}
