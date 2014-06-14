package com.surfapi.db.post;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.DB;
import com.surfapi.db.DBImpl;
import com.surfapi.db.DBLoader;
import com.surfapi.junit.CaptureSystemOutRule;

/**
 * 
 */
public class CollectInheritedMembersTest {


    /**
     * Capture and suppress stdout unless the test fails.
     */
    @Rule
    public CaptureSystemOutRule systemOutRule  = new CaptureSystemOutRule( );

    /**
     *
     */
    @Test
    public void test() throws Exception {

        DB db = new DBImpl();
        new DBLoader().inject(db).loadFile( new File("src/test/resources/com.surfapi_1.0.json") );
        
        String libraryId = "/java/com.surfapi/1.0";
        
        // Build the reference name query
        new ReferenceNameQuery().inject(db).buildIndex();
        
        Map doc = db.read(libraryId + "/com.surfapi.test.DemoJavadocSubClass");
        assertNotNull(doc);
        
        // Verify initially that the _inherited field is null. 
        assertNull( doc.get(JavadocMapUtils.InheritedFieldName) );
        
        // Now run CollectInhertedMembers. Note: must first run setStubIds.
        db.forAll( Arrays.asList(libraryId), new SetStubIds() );
        db.forAll( Arrays.asList(libraryId), new CollectInheritedMembers() );
        
        // Re-fetch the document
        doc = db.read(libraryId + "/com.surfapi.test.DemoJavadocSubClass");
        assertNotNull(doc);
        
        // Verify the _inherited field is set.
        List<Map> inherited = (List<Map>) doc.get(JavadocMapUtils.InheritedFieldName);
        assertNotNull( inherited );
        assertEquals( 1, inherited.size() );     // inherits from just one class (java.lang.Object isn't avaialble).
        
        List<Map> inheritedMethods  = (List<Map>) inherited.get(0).get("methods");
        assertNotNull( inheritedMethods );
        assertEquals( 3, inheritedMethods.size() );
        
        assertNotNull( Cawls.findFirst(inheritedMethods, new MapBuilder().append("name", "someStaticMethod") ) );
        assertNotNull( Cawls.findFirst(inheritedMethods, new MapBuilder().append("name", "methodWithTypes") ) );
        assertNull( Cawls.findFirst(inheritedMethods, new MapBuilder().append("name", "parse") ) );
        assertNull( Cawls.findFirst(inheritedMethods, new MapBuilder().append("name", "someAbstractMethod") ) );
        assertNull( Cawls.findFirst(inheritedMethods, new MapBuilder().append("name", "call") ) );
        
        // Log.trace(this, JSONTrace.prettyPrint( (List) doc.get(DocObject.InheritedFieldName) ) );
        
        // just for sanity's sake, verify inherited field is empty where it should be empty
        doc = db.read(libraryId + "/com.surfapi.test.DemoJavadoc");
        assertNotNull(doc);
        assertNull( doc.get(JavadocMapUtils.InheritedFieldName) );
        
        doc = db.read(libraryId + "/com.surfapi.test.DemoJavadoc.call()");
        assertNotNull(doc);
        assertNull( doc.get(JavadocMapUtils.InheritedFieldName) );
    }
    
    
//    /**
//     *
//     */
//    @Test
//    public void testOverrides() throws Exception {
//
//        DB db = new DBImpl();
//        new DBLoader().inject(db).loadFile( new File("src/test/resources/com.surfapi_1.0.json") );
//
//        String libraryId = "/java/com.surfapi/1.0";
//
//        Map doc = db.read(libraryId + "/com.surfapi.test.DemoJavadocSubClass.parse(java.net.URL,java.util.List)");
//        assertNotNull(doc);
//
//        // Verify initially that the _overrides and _implements field is null. 
//        assertNull( doc.get(JavadocMapUtils.OverridesFieldName) );
//        assertNull( doc.get(JavadocMapUtils.ImplementsFieldName) );
//
//        // Build the reference name query
//        new ReferenceNameQuery().inject(db).buildIndex();
//        
//        // Run SetStubIds 
//        db.forAll( Arrays.asList(libraryId), new SetStubIds() );
//        
//        // Function under test..
//        db.forAll( Arrays.asList(libraryId), new CollectInheritedMembers() );
//
//        // Re-fetch the document
//        doc = db.read(libraryId + "/com.surfapi.test.DemoJavadocSubClass.parse(java.net.URL,java.util.List)");
//        assertNotNull(doc);
//
//        // Verify the _overrides field was set.
//        Map overriddenMethod = (Map) doc.get(JavadocMapUtils.OverridesFieldName);
//        assertNotNull( overriddenMethod );
//
//        assertEquals( libraryId + "/com.surfapi.test.DemoJavadoc.parse(java.net.URL,java.util.List)", overriddenMethod.get("_id"));
//        assertEquals( "com.surfapi.test.DemoJavadoc.parse", overriddenMethod.get("qualifiedName"));
//
//        // Verify the _implements field is still null.
//        assertNull( doc.get(JavadocMapUtils.ImplementsFieldName) );
//    }
   


}
