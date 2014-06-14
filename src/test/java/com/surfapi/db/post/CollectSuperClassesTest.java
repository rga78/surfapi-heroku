
package com.surfapi.db.post;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
public class CollectSuperClassesTest {


    /**
     * Capture and suppress stdout unless the test fails.
     */
    @Rule
    public CaptureSystemOutRule systemOutRule  = new CaptureSystemOutRule( );

    /**
     *
     */
    @Test
    public void testGetSuperclasses() throws Exception {

        DB db = new DBImpl();
        new DBLoader().inject(db).loadFile( new File("src/test/resources/com.surfapi_1.0.json") );

        String libraryId = "/java/com.surfapi/1.0";
        
        // Build the reference name query
        new ReferenceNameQuery().inject(db).buildIndex();
        
        Map doc = db.read(libraryId + "/com.surfapi.test.DemoJavadocSubClass");
        assertNotNull(doc);

        List<Map> superclasses = new CollectSuperClasses().getSuperclasses( db, doc );

        assertEquals(2, superclasses.size());
        Map superclass = superclasses.get(0);
        assertEquals( "java.lang.Object", JavadocMapUtils.getQualifiedName(superclass) );
        superclass = superclasses.get(1);
        assertEquals( "com.surfapi.test.DemoJavadoc", JavadocMapUtils.getQualifiedName(superclass) );

        // Verify getSuperclasses returns nothing when it should return nothing
        doc = db.read(libraryId + "/com.surfapi.test.DemoJavadocSubClass.call()");
        assertNotNull(doc);

        superclasses = new CollectSuperClasses().getSuperclasses( db, doc );
        assertTrue( superclasses.isEmpty() );
    }


    /**
     *
     */
    @Test
    public void testGetInterfaces() throws Exception {

        DB db = new DBImpl();
        new DBLoader().inject(db).loadFile( new File("src/test/resources/com.surfapi_1.0.json") );

        String libraryId = "/java/com.surfapi/1.0";

        // Build the reference name query
        new ReferenceNameQuery().inject(db).buildIndex();
   
        Map doc = db.read(libraryId + "/com.surfapi.test.DemoJavadocSubClass");
        assertNotNull(doc);
        assertTrue( ((List)doc.get("interfaces")).isEmpty() );
        assertNull(doc.get( JavadocMapUtils.InterfacesFieldName ) );

        List<Map> interfaces = new CollectSuperClasses().getInterfaces( db, doc );

        assertEquals(2, interfaces.size());
        
        assertNotNull( Cawls.findFirst( interfaces, new MapBuilder().append( "qualifiedTypeName", "java.util.concurrent.Callable") ) );
        assertNotNull( Cawls.findFirst( interfaces, new MapBuilder().append( "qualifiedTypeName", "com.surfapi.test.DemoInterface") ) );
    
        // Verify getInterfaces returns nothing when it should return nothing
        doc = db.read(libraryId + "/com.surfapi.test.DemoJavadocSubClass.call()");
        assertNotNull(doc);

        interfaces = new CollectSuperClasses().getInterfaces( db, doc );
        assertTrue( interfaces.isEmpty() );
    }
    
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

        db.forAll( Arrays.asList(libraryId), new CollectSuperClasses() );
        db.forAll( Arrays.asList(libraryId), new CollectSuperClasses() ); // MUST BE IDEMPOTENT!
        
        Map doc = db.read(libraryId + "/com.surfapi.test.DemoJavadocSubClass");
        assertNotNull(doc);

        // Verify superclasses.
        List<Map> superclasses = (List<Map>) doc.get( JavadocMapUtils.SuperclassesFieldName) ;
        assertNotNull(superclasses);

        assertEquals(2, superclasses.size());
        Map superclass = superclasses.get(0);
        assertEquals( "java.lang.Object", JavadocMapUtils.getQualifiedName(superclass) );
        superclass = superclasses.get(1);
        assertEquals( "com.surfapi.test.DemoJavadoc", JavadocMapUtils.getQualifiedName(superclass) );

        // Verify interfaces...
        List<Map> interfaces = (List<Map>) doc.get( JavadocMapUtils.InterfacesFieldName) ;
        assertNotNull(interfaces);

        assertEquals(2, interfaces.size());
        assertNotNull( Cawls.findFirst( interfaces, new MapBuilder().append( "qualifiedTypeName", "java.util.concurrent.Callable") ) );
        assertNotNull( Cawls.findFirst( interfaces, new MapBuilder().append( "qualifiedTypeName", "com.surfapi.test.DemoInterface") ) );

        // Verify _superclasses and _interfaces is null when it should be null
        doc = db.read(libraryId + "/com.surfapi.test.DemoJavadocSubClass.call()");
        assertNotNull(doc);
        assertNull( doc.get( JavadocMapUtils.SuperclassesFieldName ) );
        assertNull( doc.get( JavadocMapUtils.InterfacesFieldName) );
    }



}


