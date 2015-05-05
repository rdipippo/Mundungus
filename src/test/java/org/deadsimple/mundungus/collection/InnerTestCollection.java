package org.deadsimple.mundungus.collection;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.bson.types.ObjectId;
import org.deadsimple.mundungus.annotations.Collection;
import org.deadsimple.mundungus.annotations.SubCollection;

import java.util.List;

@Collection
public class InnerTestCollection {
    String testField;

    List<String> testListField;
    
    ObjectId id;

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
    
    @Override
    public boolean equals(final Object other) {
        final InnerTestCollection tc = (InnerTestCollection)other;
        
        return new EqualsBuilder().append(this.id, tc.getId())
                                  .append(this.testField, tc.getTestField())
                                  .append(this.testListField, tc.getTestListField())
                                  .isEquals();
    }
}
