package org.deadsimple.mundungus;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.AnnotationMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import org.bson.types.ObjectId;
import org.deadsimple.mundungus.annotations.Collection;
import org.deadsimple.mundungus.collection.InnerTestCollection;
import org.deadsimple.mundungus.collection.TestCollection;
import org.deadsimple.mundungus.collection.TestEnum;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class ReflectionUtilsTest extends MongoTest {
    // TODO we should have a version of this test that takes a proxy.
    // TODO we should split this into individual tests.
    @Test
    public void testMapToDBO() throws Exception {
        final TestCollection tc = TestCollection.generateTestCollection();

        final BasicDBObject dbo = ReflectionUtils.mapJavaObjectToDBO(tc);
        Assert.assertEquals("test", (String) dbo.get("testField"));
        Assert.assertEquals(tc.getTestListField(), (List<String>) dbo.get("testListField"));
        Assert.assertNull(dbo.get("_id"));
        Assert.assertEquals("ffffffffffffffffffffffff", ((ObjectId) dbo.get("reference")).toString());
        Assert.assertNotNull(dbo.get("enumeratedValue"));

        BasicDBObject enumDBO = (BasicDBObject)dbo.get("enumeratedValue");
        Assert.assertEquals(Integer.valueOf(tc.getEnumeratedValue().ordinal()), (Integer)enumDBO.get("ordinal"));
        Assert.assertEquals(tc.getEnumeratedValue().name(), (String)enumDBO.get("value"));
        Assert.assertEquals(Integer.valueOf(6), (Integer)dbo.get("objIntField"));

        final BasicDBObject innerDbo = ReflectionUtils.mapJavaObjectToDBO(tc.getCollection());
        Assert.assertEquals(innerDbo, (BasicDBObject)dbo.get("collection"));

        BasicDBList complexList = (BasicDBList)dbo.get("complexListField");
        Assert.assertEquals(1, complexList.size());
        Assert.assertEquals(innerDbo.get("_id"), ((BasicDBObject)complexList.get(0)).get("_id"));
        Assert.assertEquals(innerDbo.get("testListField"), ((BasicDBObject)complexList.get(0)).get("testListField"));
        Assert.assertEquals(innerDbo.get("testField"), ((BasicDBObject) complexList.get(0)).get("testField"));
        Assert.assertEquals("org.deadsimple.mundungus.collection.InnerTestCollection", ((BasicDBObject) complexList.get(0)).get("_type"));

        // Proxies should be stored inthe db as ObjectIds.
        Assert.assertTrue(dbo.get("proxyCollection") instanceof ObjectId);
        Assert.assertTrue(dbo.get("lazyProxyCollection") instanceof ObjectId);

        // Transient field should not be stored.
        Assert.assertNull(dbo.get("transientField"));
    }

    // TODO we should split this into individual tests.
    @Test
    public void testMapToJava() throws Exception {
        BasicDBObject dbo = TestCollection.generateDBO();
        final TestCollection tc = ReflectionUtils.mapDBOToJavaObject(dbo);
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

        // test that proxyCollection was eagerly loaded (field should not be null)
        // and that lazyProxyCollection is lazily loaded (field should be null but getter should return value).
        Assert.assertNotNull(tc.proxyCollection);
        Assert.assertEquals("proxy!", tc.getProxyCollection().getTestField());
        Assert.assertNull(tc.lazyProxyCollection);
        Assert.assertEquals("lazy proxy!", tc.getLazyProxyCollection().getTestField());
        Assert.assertEquals("lazy proxy!", tc.getLazyProxyCollection().getTestField());
    }

    @Test
    public void testMapToJavaNull() throws NoSuchFieldException, InstantiationException, IllegalAccessException, InvocationTargetException {
        BasicDBObject bdo = new BasicDBObject();
        bdo.put("_type", TestCollection.class.getName());
        TestCollection testCollection = ReflectionUtils.mapDBOToJavaObject(bdo);
        Assert.assertNotNull(testCollection);
        Assert.assertNull(testCollection.getEnumeratedValue());
        Assert.assertNull(testCollection.getTestField());
        Assert.assertNull(testCollection.getId());
        Assert.assertNull(testCollection.getObjIntField());
        Assert.assertNull(testCollection.getTestListField());
        Assert.assertNull(testCollection.getCollection());
        Assert.assertNull(testCollection.getComplexListField());
    }

    @Test
    public void testMapToDBONull() throws InvocationTargetException, IllegalAccessException {
        TestCollection tc = new TestCollection();
        BasicDBObject bdo = ReflectionUtils.mapJavaObjectToDBO(tc);
        Assert.assertNotNull(bdo);
        // only field should be the _type field.
        Assert.assertEquals(1, bdo.size());
    }

    @Test
    public void testGetGetter() throws Exception {
        Method setCollection = TestCollection.class.getMethod("setCollection", InnerTestCollection.class);
        Method getter = ReflectionUtils.getGetter(setCollection);
        Assert.assertEquals("getCollection", getter.getName());
    }

    @Test
    public void testMapClassNameToCollectionCustomName() throws Exception {
        ClassPool cp = ClassPool.getDefault();
        CtClass ctClass = cp.makeClass("org.deadsimple.mundungus.collection.ProxyTestCollection");
        ClassFile classFile = ctClass.getClassFile();
        ConstPool constpool = classFile.getConstPool();
        AnnotationsAttribute attribute = new AnnotationsAttribute(classFile.getConstPool(), AnnotationsAttribute.visibleTag);
        final Annotation annotation = new Annotation("org.deadsimple.mundungus.annotations.Collection", constpool);
        annotation.addMemberValue("name", new StringMemberValue("proxy!", constpool));
        attribute.addAnnotation(annotation);
        classFile.addAttribute(attribute);
        final Class aClass = cp.toClass(ctClass);

        final String s = ReflectionUtils.mapClassNameToCollectionName(aClass);
        Assert.assertEquals("proxy!", s);
    }

    @Test
    public void testMapClassNameToCollectionDefaultName() throws Exception {
        final String s2 = ReflectionUtils.mapClassNameToCollectionName(TestCollection.class);
        Assert.assertEquals("TestCollection", s2);
    }

    @Test
    public void testMapClassNameToCollectionAnnotationOnSuperClass() throws Exception {
        ClassPool cp = ClassPool.getDefault();
        CtClass ctClass = cp.makeClass("org.deadsimple.mundungus.collection.ProxyTestCollection2", cp.get("org.deadsimple.mundungus.collection.TestCollection"));
        ClassFile classFile = ctClass.getClassFile();
        final Class aClass = cp.toClass(ctClass);

        final String s = ReflectionUtils.mapClassNameToCollectionName(aClass);
        Assert.assertEquals("TestCollection", s);
    }

    @Test
    public void testTypeField() throws Exception {
        ClassPool cp = ClassPool.getDefault();
        CtClass ctClass = cp.makeClass("org.deadsimple.mundungus.collection.ProxyTestCollection3", cp.get("org.deadsimple.mundungus.collection.TestCollection"));
        ClassFile classFile = ctClass.getClassFile();
        final Class aClass = cp.toClass(ctClass);

        final Object o = aClass.newInstance();
        final BasicDBObject dbo = ReflectionUtils.mapJavaObjectToDBO(o);
        Assert.assertEquals("org.deadsimple.mundungus.collection.ProxyTestCollection3", dbo.get("_type"));
        final TestCollection testCollection = ReflectionUtils.mapDBOToJavaObject(dbo);
        Assert.assertEquals("org.deadsimple.mundungus.collection.ProxyTestCollection3", testCollection.getClass().getSuperclass().getName());
    }
}
