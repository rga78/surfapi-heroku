
package com.surfapi.db.post;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.DB;
import com.surfapi.db.MongoDBImpl;
import com.surfapi.javadoc.SimpleJavadocProcess;
import com.surfapi.junit.CaptureSystemOutRule;
import com.surfapi.junit.DropMongoDBRule;
import com.surfapi.junit.MongoDBProcessRule;

/**
 * 
 */
public class CollectSuperClassesTest {
    
    /**
     * For connecting to the mongodb service
     */
    public static final String MongoDbName = "test1";
    public static final String MongoUri = "mongodb://localhost/" + MongoDbName;

    /**
     * Executed before and after the entire collection of tests (like @BeforeClass/@AfterClass).
     * 
     * Ensures a mongodb process is started.
     */
    @ClassRule
    public static MongoDBProcessRule mongoDBProcessRule = new MongoDBProcessRule();
    
    /**
     * Drops the given db before/after the entire collection of tests.
     */
    @ClassRule
    public static DropMongoDBRule dropMongoDBRule = new DropMongoDBRule( mongoDBProcessRule, MongoDbName);
    
    /**
     * Capture and suppress stdout unless the test fails.
     */
    @Rule
    public CaptureSystemOutRule systemOutRule  = new CaptureSystemOutRule( );

    /**
     * 
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        // Setup the db.
        
        String dbName = MongoDbName;
        String libraryId = "/java/com.surfapi/1.0";
        
        File baseDir = new File("src/test/java");
        
        SimpleJavadocProcess javadocProcess = new SimpleJavadocProcess()
                                                    .setMongoUri( MongoUri )
                                                    .setLibraryId( libraryId )
                                                    .setSourcePath( baseDir )
                                                    .setPackages( Arrays.asList( "com.surfapi.test" ) );
        javadocProcess.run();
        
        // Build the reference name query
        new ReferenceNameQuery().inject(new MongoDBImpl(MongoDbName)).buildIndex();
    }
    
    /**
     * 
     */
    @Before
    public void before() {
        assumeTrue(mongoDBProcessRule.isStarted());
    }

    /**
     *
     */
    @Test
    public void testGetSuperclasses() throws Exception {

        DB db = new MongoDBImpl(MongoDbName) ;

        String libraryId = "/java/com.surfapi/1.0";
          
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

        DB db = new MongoDBImpl(MongoDbName) ;
        String libraryId = "/java/com.surfapi/1.0";

        Map doc = db.read(libraryId + "/com.surfapi.test.DemoJavadocSubClass");
        assertNotNull(doc);
        // assertNull(doc.get( JavadocMapUtils.InterfacesFieldName ) );

        List<Map> interfaces = new CollectSuperClasses().getInterfaces( db, doc );

        assertEquals(3, interfaces.size());
        
        assertNotNull( Cawls.findFirst( interfaces, new MapBuilder().append( "qualifiedTypeName", "java.util.concurrent.Callable") ) );
        assertNotNull( Cawls.findFirst( interfaces, new MapBuilder().append( "qualifiedTypeName", "com.surfapi.test.DemoInterface") ) );
        assertNotNull( Cawls.findFirst( interfaces, new MapBuilder().append( "qualifiedTypeName", "java.io.Serializable") ) );
    
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

        DB db = new MongoDBImpl(MongoDbName) ;

        String libraryId = "/java/com.surfapi/1.0";

        db.forAll( Arrays.asList(libraryId), new CollectSuperClasses() );
        db.forAll( Arrays.asList(libraryId), new CollectSuperClasses() ); // MUST BE IDEMPOTENT!
        
        Map doc = db.read(libraryId + "/com.surfapi.test.DemoJavadocSubClass");
        assertNotNull(doc);

        // Verify superclasses.
        List<Map> superclasses = (List<Map>) doc.get( JavadocMapUtils.SuperclassesFieldName) ;
        assertNotNull(superclasses);

        assertEquals(2, superclasses.size());
        Map superclass = superclasses.get(0);
        assertEquals( "com.surfapi.test.DemoJavadoc", JavadocMapUtils.getQualifiedName(superclass) );
        
        superclass = superclasses.get(1);
        assertEquals( "java.lang.Object", JavadocMapUtils.getQualifiedName(superclass) );

        // Verify interfaces...
        List<Map> interfaces = (List<Map>) doc.get( JavadocMapUtils.InterfacesFieldName) ;
        assertNotNull(interfaces);

        assertEquals(3, interfaces.size());
        assertNotNull( Cawls.findFirst( interfaces, new MapBuilder().append( "qualifiedTypeName", "java.util.concurrent.Callable") ) );
        assertNotNull( Cawls.findFirst( interfaces, new MapBuilder().append( "qualifiedTypeName", "com.surfapi.test.DemoInterface") ) );
        assertNotNull( Cawls.findFirst( interfaces, new MapBuilder().append( "qualifiedTypeName", "java.io.Serializable") ) );
 
        // Verify _superclasses and _interfaces is null when it should be null
        doc = db.read(libraryId + "/com.surfapi.test.DemoJavadocSubClass.call()");
        assertNotNull(doc);
        assertNull( doc.get( JavadocMapUtils.SuperclassesFieldName ) );
        assertNull( doc.get( JavadocMapUtils.InterfacesFieldName) );
    }



}


