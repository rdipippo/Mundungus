package org.deadsimple.mundungus;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import org.bson.types.ObjectId;
import org.deadsimple.mundungus.collection.InnerTestCollection;
import org.deadsimple.mundungus.collection.TestCollection;
import org.deadsimple.mundungus.collection.TestEnum;
import org.junit.Assert;
import org.junit.Test;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;

public class ReflectionUtilsTest {
    @Test
    public void testMapToDBO() throws Exception {
        final TestCollection tc = TestCollection.generateTestCollection();
        
        final BasicDBObject dbo = ReflectionUtils.mapJavaObjectToDBO(tc);
        Assert.assertEquals("test", (String)dbo.get("testField"));
        
        Assert.assertEquals(tc.getTestListField(), (List<String>)dbo.get("testListField"));
        Assert.assertNull(dbo.get("_id"));
        Assert.assertEquals("ffffffffffffffffffffffff", ((ObjectId)dbo.get("reference")).toString());

        Assert.assertNotNull(dbo.get("enumeratedValue"));
        BasicDBObject enumDBO = (BasicDBObject)dbo.get("enumeratedValue");
        Assert.assertEquals(Integer.valueOf(tc.getEnumeratedValue().ordinal()), (Integer)enumDBO.get("ordinal"));
        Assert.assertEquals(tc.getEnumeratedValue().name(), (String)enumDBO.get("value"));
        Assert.assertEquals(Integer.valueOf(6), (Integer)dbo.get("objIntField"));
        final BasicDBObject innerDbo = ReflectionUtils.mapJavaObjectToDBO(tc.getCollection());
        Assert.assertEquals(innerDbo, (BasicDBObject)dbo.get("collection"));
    }
    
    @Test
    public void testMapToJava() throws Exception {
        BasicDBObject dbo = TestCollection.generateDBO();
        final TestCollection tc = ReflectionUtils.mapDBOToJavaObject(TestCollection.class, dbo);
        Assert.assertEquals("test", tc.getTestField());
        Assert.assertEquals("abcdeabcdeabcdeabcdeabcd", tc.getId().toString());
        Assert.assertEquals("ffffffffffffffffffffffff", tc.getReference().toString());
        Assert.assertEquals(TestEnum.VALUE1, tc.getEnumeratedValue());
        Assert.assertEquals(Integer.valueOf(6), tc.getObjIntField());

        final InnerTestCollection itc = tc.getCollection();
        Assert.assertEquals("abcdeabcdeabcdeabcdeabcd", itc.getId().toString());
        Assert.assertEquals("innerTest", itc.getTestField());

        final List<String> testList = new ArrayList<String>();
        testList.add("test1");
        testList.add("test2");

        Assert.assertEquals(testList, itc.getTestListField());
        Assert.assertEquals(testList, tc.getTestListField());
    }

    @Test
    public void testMapToJavaNull() throws NoSuchFieldException, InstantiationException, IllegalAccessException, InvocationTargetException {
        BasicDBObject bdo = new BasicDBObject();
        TestCollection testCollection = ReflectionUtils.mapDBOToJavaObject(TestCollection.class, bdo);
        Assert.assertNotNull(testCollection);
        Assert.assertNull(testCollection.getEnumeratedValue());
        Assert.assertNull(testCollection.getTestField());
        Assert.assertNull(testCollection.getId());
        Assert.assertNull(testCollection.getObjIntField());
        Assert.assertNull(testCollection.getTestListField());
        Assert.assertNull(testCollection.getCollection());
    }

    @Test
    public void testMapToDBONull() throws InvocationTargetException, IllegalAccessException {
        TestCollection tc = new TestCollection();
        BasicDBObject bdo = ReflectionUtils.mapJavaObjectToDBO(tc);
        Assert.assertNotNull(bdo);
        Assert.assertEquals(0, bdo.size());
    }
}
