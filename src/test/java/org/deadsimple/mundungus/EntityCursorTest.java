package org.deadsimple.mundungus;

import org.deadsimple.mundungus.collection.TestCollection;
import org.deadsimple.mundungus.mock.MockCursor;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class EntityCursorTest extends MongoTest {
   @Test
   @Ignore
   // TODO this fails from the command line with maven.
   // but when we attach debugger it works again.
   // Somehow a real cursor is getting new'ed instead of a mock.
   public void testCursor() {
       final EntityCursor<TestCollection> cursor = new EntityCursor<TestCollection>(new MockCursor(), TestCollection.class);
       final TestCollection tc = cursor.nextEntity();
       Assert.assertNotNull(tc);
   }
}
