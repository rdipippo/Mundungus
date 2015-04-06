package org.deadsimple.mundungus.mock;

import org.bson.types.ObjectId;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.Cursor;
import com.mongodb.DBObject;
import com.mongodb.ServerAddress;
import org.deadsimple.mundungus.collection.TestCollection;

public class MockCursor implements Cursor {

    public boolean hasNext() {
        // TODO Auto-generated method stub
        return false;
    }

    public DBObject next() {
        return TestCollection.generateDBO();
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
