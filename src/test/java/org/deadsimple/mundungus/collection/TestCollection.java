package org.deadsimple.mundungus.collection;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.bson.types.ObjectId;
import org.deadsimple.mundungus.annotations.Collection;
import org.deadsimple.mundungus.annotations.Transient;

import java.util.ArrayList;
import java.util.List;

@Collection
public class TestCollection {
    String testField;
    
    List<String> testListField;
    
    ObjectId id;
    
    ObjectId reference;
    
    InnerTestCollection collection;

    List<InnerTestCollection> complexListField;

    TestEnum enumeratedValue;

    Integer objIntField;

    @Transient
    String transientField;

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

    public TestEnum getEnumeratedValue() {
        return enumeratedValue;
    }

    public void setEnumeratedValue(TestEnum enumeratedValue) {
        this.enumeratedValue = enumeratedValue;
    }

    public Integer getObjIntField() {
        return objIntField;
    }

    public void setObjIntField(Integer objIntField) {
        this.objIntField = objIntField;
    }

    public List<InnerTestCollection> getComplexListField() {
        return complexListField;
    }

    public String getTransientField() {
        return transientField;
    }

    @Transient
    public String getNothing() {
        return "nothing";
    }

    @Transient
    public void setNothing(String nothing) {

    }

    public void setTransientField(String transientField) {
        this.transientField = transientField;
    }

    public void setComplexListField(List<InnerTestCollection> complexListField) {
        this.complexListField = complexListField;
    }

    public static BasicDBObject generateDBO() {
        final BasicDBObject dbo = new BasicDBObject();
        dbo.put("testField", "test");

        final BasicDBList dbl = new BasicDBList();
        dbl.add("test1");
        dbl.add("test2");

        dbo.put("testListField", dbl);
        dbo.put("reference", new ObjectId("ffffffffffffffffffffffff"));
        dbo.put("objIntField", Integer.valueOf(6));

        BasicDBObject enumDBO = new BasicDBObject();
        enumDBO.put("ordinal", 0);
        enumDBO.put("value", "VALUE1");
        dbo.put("enumeratedValue", enumDBO);

        final BasicDBObject innerCollection = new BasicDBObject();
        innerCollection.put("testField", "innerTest");
        innerCollection.put("testListField", dbl);
        innerCollection.put("_id", new ObjectId("abcdeabcdeabcdeabcdeabcd"));

        BasicDBObject innerCollectionListMember = new BasicDBObject(innerCollection);
        innerCollectionListMember.put("_type", InnerTestCollection.class.getName());

        BasicDBList complexList = new BasicDBList();
        complexList.add(innerCollectionListMember);

        dbo.put("complexListField", complexList);

        dbo.put("collection", innerCollection);
        dbo.put("_id", new ObjectId("abcdeabcdeabcdeabcdeabcd"));

        return dbo;
    }

    public static TestCollection generateTestCollection() {
        final TestCollection tc = new TestCollection();
        tc.setTestField("test");
        tc.setObjIntField(6);
        
        final List<String> testList = new ArrayList<String>();
        testList.add("test1");
        testList.add("test2");
        tc.setTestListField(testList);
        
        tc.setReference(new ObjectId("ffffffffffffffffffffffff"));

        final InnerTestCollection itc = new InnerTestCollection();
        itc.setTestField(tc.getTestField());
        itc.setTestListField(tc.getTestListField());
        itc.setId(new ObjectId("abcdeabcdeabcdeabcdeabcd"));

        tc.setEnumeratedValue(TestEnum.VALUE1);
        
        tc.setCollection(itc);
        List<InnerTestCollection> itcList = new ArrayList<InnerTestCollection>();
        itcList.add(itc);
        tc.setComplexListField(itcList);

        tc.setTransientField("This field should not be stored in the database.");
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
