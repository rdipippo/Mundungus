package org.deadsimple.mundungus.mock;

import org.bson.types.ObjectId;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DBObject;
import com.mongodb.ServerAddress;

public class MockCursor implements Cursor {

    public boolean hasNext() {
        // TODO Auto-generated method stub
        return false;
    }

    public DBObject next() {
        final BasicDBObject dbo = new BasicDBObject();
        dbo.put("testField", "test");
        
        final BasicDBList dbl = new BasicDBList();
        dbl.add("test1");
        dbl.add("test2");
        
        dbo.put("testListField", dbl);
        dbo.put("reference", new ObjectId("ffffffffffffffffffffffff"));
        
        final BasicDBObject innerCollection = new BasicDBObject();
        innerCollection.put("testField", "innerTest");
        innerCollection.put("testListField", dbl);
        innerCollection.put("_id", new ObjectId("abcdeabcdeabcdeabcdeabcd"));
        
        dbo.put("collection", innerCollection);
        dbo.put("_id", new ObjectId("abcdeabcdeabcdeabcdeabcd"));
        return dbo;
    }

    public void remove() {
        // TODO Auto-generated method stub
        
    }

    public long getCursorId() {
        // TODO Auto-generated method stub
        return 0;
    }

    public ServerAddress getServerAddress() {
        // TODO Auto-generated method stub
        return null;
    }

    public void close() {
        // TODO Auto-generated method stub
        
    }
    
}
