package org.deadsimple.mundungus;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.bson.types.ObjectId;
import org.deadsimple.mundungus.collection.TestCollection;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class EntityManagerTest extends MongoTest {

   @Test
   public void testGetDbConnection() {
	   final DB db = this.em.getDbConnection();
	   Assert.assertNotNull(db);
   }
   
   @Test
   public void testBadHostInConfig() {
       this.thrown.expect(IllegalArgumentException.class);
       this.thrown.expectMessage("Unknown host specified in");
       new EntityManager("mundungus.properties.badhost");
   }
   
   @Test
   public void testBadConfigFileLocation() {
       this.thrown.expect(IllegalArgumentException.class);
       this.thrown.expectMessage("Could not find properties file");
       new EntityManager("nonexistent_properties_file");
   }
   
   @Test
   public void testPersist() {
       final TestCollection tc = TestCollection.generateTestCollection();
       ObjectId persistedId = this.em.persist(tc);

       Assert.assertNotNull(persistedId);
       Assert.assertEquals(persistedId, tc.getId());
       
       final TestCollection finder = new TestCollection();
       finder.setTestField(tc.getTestField());
       
       final EntityCursor<TestCollection> entityCursor = this.em.get(finder);
       final TestCollection fetchedTc = entityCursor.nextEntity();

       Assert.assertTrue(new EqualsBuilder().append(fetchedTc.getTestListField(), tc.getTestListField())
               .append(fetchedTc.getReference(), tc.getReference())
               .append(fetchedTc.getTestField(), tc.getTestField())
               .append(fetchedTc.getCollection(), tc.getCollection())
            .isEquals());

       final TestCollection tc2 = TestCollection.generateTestCollection();
       this.em.persist(tc2);

       Assert.assertNotEquals(fetchedTc.getId(), tc2.getId());
   }
   
   @Test
   public void testUpdate() {
       final TestCollection tc = TestCollection.generateTestCollection();
       this.em.persist(tc);
       
       final TestCollection finder = new TestCollection();
       finder.setTestField(tc.getTestField());
       
       EntityCursor<TestCollection> entityCursor = this.em.get(finder);
       TestCollection fetchedTc = entityCursor.nextEntity();
       fetchedTc.setTestField("updated value");
       this.em.persist(fetchedTc);
       
       finder.setTestField("updated value");
       entityCursor = this.em.get(finder);
       fetchedTc = entityCursor.nextEntity();
       
       Assert.assertNotEquals(fetchedTc, tc);
       Assert.assertTrue(new EqualsBuilder().append(fetchedTc.getTestListField(), tc.getTestListField())
                                            .append(fetchedTc.getReference(), tc.getReference())
                                            .append(fetchedTc.getCollection(), tc.getCollection())
                                         .isEquals());
       Assert.assertEquals("updated value", fetchedTc.getTestField());
       Assert.assertEquals(tc.getId(), fetchedTc.getId());
   }
   
   @Test
   public void testRemove() {
       final TestCollection tc = TestCollection.generateTestCollection();
       this.em.persist(tc);
       
       final TestCollection finder = new TestCollection();
       finder.setTestField(tc.getTestField());

       EntityCursor<TestCollection> entityCursor = this.em.get(finder);
       final TestCollection fetchedTc = entityCursor.nextEntity();
       this.em.remove(fetchedTc);
              
       entityCursor = this.em.get(finder);
       Assert.assertFalse(entityCursor.hasNext());
   }
   
   @Test
   public void testFindAll() {
       final TestCollection tc = TestCollection.generateTestCollection();
       this.em.persist(tc);
       
       final TestCollection tc2 = TestCollection.generateTestCollection();
       this.em.persist(tc2);
       
       final TestCollection tc3 = TestCollection.generateTestCollection();
       this.em.persist(tc3);
       
       final EntityCursor<TestCollection> findAll = this.em.findAll(TestCollection.class);
       
       int rowCount = 0;
       while(findAll.hasNext()) {
           rowCount++;
           findAll.nextEntity();
       }
       
       Assert.assertEquals(3, rowCount);
   }
   
   @Test
   public void testMapAllDefaultKey() {
       final TestCollection tc = TestCollection.generateTestCollection();
       tc.setTestField("1");
       this.em.persist(tc);
       
       final TestCollection tc2 = TestCollection.generateTestCollection();
       tc2.setTestField("2");
       this.em.persist(tc2);
       
       final TestCollection tc3 = TestCollection.generateTestCollection();
       tc.setTestField("3");
       this.em.persist(tc3);
       
       final Map<String, TestCollection> findAll = this.em.mapAll(TestCollection.class);
       
       Assert.assertEquals(3, findAll.size());
   }
}
