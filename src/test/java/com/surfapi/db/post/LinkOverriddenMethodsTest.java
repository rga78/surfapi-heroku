package com.surfapi.db.post;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.db.DB;
import com.surfapi.db.DBImpl;
import com.surfapi.db.DBLoader;
import com.surfapi.junit.CaptureSystemOutRule;

/**
 * 
 */
public class LinkOverriddenMethodsTest {


    /**
     * Capture and suppress stdout unless the test fails.
     */
    @Rule
    public CaptureSystemOutRule systemOutRule  = new CaptureSystemOutRule( );

    /**
     *
     */
    @Test
    public void testOverrides() throws Exception {

        DB db = new DBImpl();
        new DBLoader().inject(db).loadFile( new File("src/test/resources/com.surfapi_1.0.json") );
        
        String libraryId = "/java/com.surfapi/1.0";
        
        Map doc = db.read(libraryId + "/com.surfapi.test.DemoJavadocSubClass.parse(java.net.URL,java.util.List)");
        assertNotNull(doc);
        
        // Verify initially that the _overrides and _implements field is null. 
        assertNull( doc.get(JavadocMapUtils.OverridesFieldName) );
        assertNull( doc.get(JavadocMapUtils.ImplementsFieldName) );
        
        // Build the reference name query
        new ReferenceNameQuery().inject(db).buildIndex();
        
        // Run the post processor.
        db.forAll( Arrays.asList(libraryId), new SetStubIds() );
        db.forAll( Arrays.asList(libraryId), new CollectSuperClasses() );
        db.forAll( Arrays.asList(libraryId), new CollectInheritedMembers() );
        db.forAll( Arrays.asList(libraryId), new LinkOverriddenMethods() );
        
        // Re-fetch the document
        doc = db.read(libraryId + "/com.surfapi.test.DemoJavadocSubClass.parse(java.net.URL,java.util.List)");
        assertNotNull(doc);
        
        // Verify the _overrides field was set.
        Map overriddenMethod = (Map) doc.get(JavadocMapUtils.OverridesFieldName);
        assertNotNull( overriddenMethod );
        
        assertEquals( libraryId + "/com.surfapi.test.DemoJavadoc.parse(java.net.URL,java.util.List)", overriddenMethod.get("_id"));
        assertEquals( "com.surfapi.test.DemoJavadoc.parse", overriddenMethod.get("qualifiedName"));
        
        // Verify the _implements field is still null.
        assertNull( doc.get(JavadocMapUtils.ImplementsFieldName) );
    }
    
    
    /**
    *
    */
   @Test
   public void testImplements() throws Exception {

       DB db = new DBImpl();
       new DBLoader().inject(db).loadFile( new File("src/test/resources/com.surfapi_1.0.json") );
       
       String libraryId = "/java/com.surfapi/1.0";
       
       Map doc = db.read(libraryId + "/com.surfapi.test.DemoJavadocSubClass.interfaceMethod(java.lang.String)");
       assertNotNull(doc);
       
       // Verify initially that the _overrides and _implements field is null. 
       assertNull( doc.get(JavadocMapUtils.OverridesFieldName) );
       assertNull( doc.get(JavadocMapUtils.ImplementsFieldName) );
       
       // Build the reference name query
       new ReferenceNameQuery().inject(db).buildIndex();
       
       // Run the post processor.
       db.forAll( Arrays.asList(libraryId), new SetStubIds() );
       db.forAll( Arrays.asList(libraryId), new CollectSuperClasses() );
       db.forAll( Arrays.asList(libraryId), new CollectInheritedMembers() );
       db.forAll( Arrays.asList(libraryId), new LinkOverriddenMethods() );
       
       // Re-fetch the document
       doc = db.read(libraryId + "/com.surfapi.test.DemoJavadocSubClass.interfaceMethod(java.lang.String)");
       assertNotNull(doc);
       
       Map implementedMethod  = (Map) doc.get(JavadocMapUtils.ImplementsFieldName);
       assertNotNull( implementedMethod );
       
       assertEquals( libraryId + "/com.surfapi.test.DemoInterface.interfaceMethod(java.lang.String)", implementedMethod.get("_id"));
       assertEquals( "com.surfapi.test.DemoInterface.interfaceMethod", implementedMethod.get("qualifiedName"));
       
       // Verify the _overrides field is still null.
       assertNull( doc.get(JavadocMapUtils.OverridesFieldName) );
   }


}
