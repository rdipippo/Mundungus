package org.deadsimple.mundungus;

import org.deadsimple.mundungus.collection.TestCollection;
import org.deadsimple.mundungus.mock.MockCursor;
import org.junit.Assert;
import org.junit.Test;

public class EntityCursorTest {
   @Test
   public void testCursor() {
       final EntityCursor<TestCollection> cursor = new EntityCursor<TestCollection>(new MockCursor(), TestCollection.class);
       final TestCollection tc = cursor.nextEntity();
       Assert.assertNotNull(tc);
   }
}
