package org.deadsimple.mundungus;

import com.mongodb.BasicDBObject;
import org.deadsimple.mundungus.collection.InnerTestCollection;
import org.deadsimple.mundungus.collection.TestCollection;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

public abstract class MongoTest {
    EntityManager em = new EntityManager();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @BeforeClass
    public static void beforeClass() {
        EmbeddedMongo.start();
    }

    @Before
    public void beforeEachTest() {
        this.em.getCollection(TestCollection.class).remove(new BasicDBObject());
        this.em.getCollection(InnerTestCollection.class).remove(new BasicDBObject());
    }

    @AfterClass
    public static void afterClass() {
        EmbeddedMongo.stop();
    }
}
