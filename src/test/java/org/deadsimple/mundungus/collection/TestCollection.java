package org.deadsimple.mundungus.collection;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.bson.types.ObjectId;
import org.deadsimple.mundungus.annotations.Collection;
import org.deadsimple.mundungus.annotations.SubCollection;

@Collection
public class TestCollection {
    String testField;
    
    @SubCollection(String.class)
    List<String> testListField;
    
    ObjectId id;
    
    ObjectId reference;
    
    InnerTestCollection collection;

    public ObjectId getId() {
        return this.id;
    }

    public void setId(final ObjectId id) {
        this.id = id;
    }

    public String getTestField() {
        return this.testField;
    }

    public void setTestField(final String testField) {
        this.testField = testField;
    }

    public List<String> getTestListField() {
        return this.testListField;
    }

    public void setTestListField(final List<String> testListField) {
        this.testListField = testListField;
    }
    
    public InnerTestCollection getCollection() {
        return this.collection;
    }
    
    public void setCollection(final InnerTestCollection tc) {
        this.collection = tc;
    }

    public ObjectId getReference() {
        return this.reference;
    }

    public void setReference(final ObjectId reference) {
        this.reference = reference;
    }
    
    public static TestCollection generateTestCollection() {
        final TestCollection tc = new TestCollection();
        tc.setTestField("test");
        
        final List<String> testList = new ArrayList<String>();
        testList.add("test1");
        testList.add("test2");
        tc.setTestListField(testList);
        
        tc.setReference(new ObjectId("ffffffffffffffffffffffff"));
        //tc.setId("abcdeabcdeabcdeabcdeabcd");
        
        final InnerTestCollection itc = new InnerTestCollection();
        itc.setTestField(tc.getTestField());
        itc.setTestListField(tc.getTestListField());
        itc.setId(new ObjectId("abcdeabcdeabcdeabcdeabcd"));
        
        tc.setCollection(itc);
        return tc;
    }
    
    @Override
    public boolean equals(final Object other) {
        final TestCollection tc = (TestCollection)other;
        
        return new EqualsBuilder().append(this.id, tc.getId())
                                  .append(this.testField, tc.getTestField())
                                  .append(this.testListField, tc.getTestListField())
                                  .append(this.reference, tc.getReference())
                                  .append(this.collection, tc.getCollection())
                                  .isEquals();
    }
}
