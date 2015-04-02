package org.deadsimple.mundungus;

import java.util.ArrayList;
import java.util.List;
import org.bson.types.ObjectId;
import org.deadsimple.mundungus.collection.InnerTestCollection;
import org.deadsimple.mundungus.collection.TestCollection;
import org.junit.Assert;
import org.junit.Test;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class ReflectionUtilsTest {
    @Test
    public void testMapToDBO() throws Exception {
        final TestCollection tc = TestCollection.generateTestCollection();
        
        final BasicDBObject dbo = ReflectionUtils.mapJavaObjectToDBO(tc);
        Assert.assertEquals("test", dbo.get("testField"));
        
        Assert.assertEquals(tc.getTestListField(), dbo.get("testListField"));
        Assert.assertNull(dbo.get("_id"));
        Assert.assertEquals("ffffffffffffffffffffffff", ((ObjectId)dbo.get("reference")).toString());
        
        final BasicDBObject innerDbo = ReflectionUtils.mapJavaObjectToDBO(tc.getCollection());
        Assert.assertEquals(innerDbo, dbo.get("collection"));
    }
    
    @Test
    public void testMapToJava() throws Exception {
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
        
        final TestCollection tc = ReflectionUtils.mapDBOToJavaObject(TestCollection.class, dbo);
        Assert.assertEquals("test", tc.getTestField());
        Assert.assertEquals("abcdeabcdeabcdeabcdeabcd", tc.getId().toString());
        Assert.assertEquals("ffffffffffffffffffffffff", tc.getReference().toString());
        
        final InnerTestCollection itc = tc.getCollection();
        Assert.assertEquals("abcdeabcdeabcdeabcdeabcd", itc.getId().toString());
        Assert.assertEquals("innerTest", itc.getTestField());
        
        final List<String> testList = new ArrayList<String>();
        testList.add("test1");
        testList.add("test2");
        
        Assert.assertEquals(testList, itc.getTestListField());
        Assert.assertEquals(testList, tc.getTestListField());
    }
}
