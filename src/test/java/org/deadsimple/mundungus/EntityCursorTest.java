package org.deadsimple.mundungus;

import org.deadsimple.mundungus.collection.TestCollection;
import org.deadsimple.mundungus.mock.MockCursor;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class EntityCursorTest extends MongoTest {

   // TODO this fails from the command line with maven.
   // but when we attach debugger it works again.
   // ReflectionUtils gets error when it calls next() on cursor that fetches from the db.
    // seems like we might be trying to connect to flapdoodle mongo before it's fully setup?
    // https://github.com/lordofthejars/nosql-unit/issues/47 (check out nojournaling option)
   // @Ignore
   @Test
   public void testCursor() {
       final EntityCursor<TestCollection> cursor = new EntityCursor<TestCollection>(new MockCursor());
       final TestCollection tc = cursor.nextEntity();
       Assert.assertNotNull(tc);
   }
}
