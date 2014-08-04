
package com.surfapi.db.post;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
import com.surfapi.junit.MongoDBProcessRule;

/**
 * 
 */
public class AllKnownSubclassesQueryTest {
    
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
    public static MongoDBProcessRule mongoDBProcessRule = new MongoDBProcessRule(MongoDbName);
    
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
        assumeTrue(mongoDBProcessRule.isStarted());
        
        // Setup the db.
        String libraryId = "/java/com.surfapi/1.0";
        
        File baseDir = new File("src/test/java");
        
        new SimpleJavadocProcess()
               .setMongoUri( MongoUri )
               .setLibraryId( libraryId )
               .setSourcePath( baseDir )
               .setPackages( Arrays.asList( "com.surfapi.test" ) )
               .run();
        
        // add another version of the library
        new SimpleJavadocProcess()
                .setMongoUri( MongoUri )
                .setLibraryId( "/java/com.surfapi/0.9" )
                .setSourcePath( baseDir )
                .setPackages( Arrays.asList( "com.surfapi.test" ) )
                .run();
        
        // Build the all known subclasses index
        new AllKnownSubclassesQuery().inject(new MongoDBImpl(MongoDbName)).buildIndex();
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
    public void testGetSubclasses() throws Exception {

        DB db = new MongoDBImpl(MongoDbName) ;

        List<Map> subclasses = new AllKnownSubclassesQuery().inject(db).query("com.surfapi.test.DemoJavadoc");

        assertEquals(1, subclasses.size());
        assertEquals( "com.surfapi.test.DemoJavadocSubClass", JavadocMapUtils.getQualifiedName(subclasses.get(0)) );
       
        subclasses = new AllKnownSubclassesQuery().inject(db).query("com.surfapi.test.DemoJavadocSubClass");

        assertEquals(1, subclasses.size());
        assertEquals( "com.surfapi.test.DemoJavadocSubClass2", JavadocMapUtils.getQualifiedName(subclasses.get(0)) );
       
        // Verify it returns nothing when it should return nothing
        subclasses = new AllKnownSubclassesQuery().inject(db).query("com.surfapi.test.DemoJavadocSubClass2");
        assertTrue( subclasses.isEmpty() );
     
        // make sure it worked for interfaces too
        subclasses = new AllKnownSubclassesQuery().inject(db).query("com.surfapi.test.DemoInterface");
        assertEquals(1, subclasses.size());
        assertEquals( "com.surfapi.test.DemoInterfaceSubIntf", JavadocMapUtils.getQualifiedName(subclasses.get(0)) );
      
        subclasses = new AllKnownSubclassesQuery().inject(db).query("com.surfapi.test.DemoInterface2");
        assertEquals(1, subclasses.size());
        assertEquals( "com.surfapi.test.DemoInterfaceSubIntf", JavadocMapUtils.getQualifiedName(subclasses.get(0)) );

        subclasses = new AllKnownSubclassesQuery().inject(db).query("com.surfapi.test.DemoInterfaceSubIntf");
        assertTrue( subclasses.isEmpty() );
        
        subclasses = new AllKnownSubclassesQuery().inject(db).query("java.lang.Object");
        assertTrue( subclasses.isEmpty() );
    }

    
    /**
     * 
     */
    @Test
    public void testAddAndRemoveLibrary() throws Exception {
        
        assumeTrue(mongoDBProcessRule.isStarted());
        
        // add the library first
        String libraryId = "/java/com.surfapi.proc/1.0";
        
        File baseDir = new File("src/main/java");
        
        new SimpleJavadocProcess()
               .setMongoUri( MongoUri )
               .setLibraryId( libraryId )
               .setSourcePath( baseDir )
               .setPackages( Arrays.asList( "com.surfapi.proc" ) )
               .run();
        
        MongoDBImpl db = new MongoDBImpl(MongoDbName);
        
        // Add library to the index
        new AllKnownSubclassesQuery().inject(db).addLibraryToIndex(libraryId);
        
        // Verify it's there 
        List<Map> subclasses = new AllKnownSubclassesQuery().inject(db).query("java.io.IOException");
        assertFalse( subclasses.isEmpty() );
        assertNotNull( Cawls.findFirst( subclasses, new MapBuilder().append( "name", "ProcessException" ) ) );
        
        // Now remove
        new AllKnownSubclassesQuery().inject(db).removeLibrary(libraryId);
        
        // Verify it's gone
        subclasses = new AllKnownSubclassesQuery().inject(db).query("java.io.IOException");
        assertNull( Cawls.findFirst( subclasses, new MapBuilder().append( "name", "ProcessException" ) ) );
    }

    
    /**
     * 
     */
    @Test
    public void testRemoveLibraryMultipleVersions() throws Exception {
        
        assumeTrue(mongoDBProcessRule.isStarted());
        
        MongoDBImpl db = new MongoDBImpl(MongoDbName);
        
        // add the library first
        String libraryId1 = "/java/com.surfapi.proc/1.0";
        String libraryId2 = "/java/com.surfapi.proc/2.0";
        
        File baseDir = new File("src/main/java");
        
        new SimpleJavadocProcess()
               .setMongoUri( MongoUri )
               .setLibraryId( libraryId1 )
               .setSourcePath( baseDir )
               .setPackages( Arrays.asList( "com.surfapi.proc" ) )
               .run();
        
        new SimpleJavadocProcess()
               .setMongoUri( MongoUri )
               .setLibraryId( libraryId2 )
               .setSourcePath( baseDir )
               .setPackages( Arrays.asList( "com.surfapi.proc" ) )
               .run();


        
        // Add library to the index
        new AllKnownSubclassesQuery().inject(db).addLibraryToIndex(libraryId1);
        new AllKnownSubclassesQuery().inject(db).addLibraryToIndex(libraryId2);
        
        
        // Verify it's there 
        {
            List<Map> subclasses = new AllKnownSubclassesQuery().inject(db).query("java.io.IOException");
            assertFalse( subclasses.isEmpty() );
            Map subclass =  Cawls.findFirst( subclasses, new MapBuilder().append( "name", "ProcessException" ) ) ;
            assertNotNull( subclass );
            List<String> libraryVersions = (List<String>) subclass.get("_libraryVersions");
            assertNotNull( libraryVersions );
            assertEquals( 2, libraryVersions.size() );
            assertTrue( libraryVersions.contains( "1.0") );
            assertTrue( libraryVersions.contains( "2.0") );
        }
        
        // Now remove
        new AllKnownSubclassesQuery().inject(db).removeLibrary(libraryId1);
        
        // Verify it's STILL there, only with the library version removed.
        {
            List<Map> subclasses = new AllKnownSubclassesQuery().inject(db).query("java.io.IOException");
            assertFalse( subclasses.isEmpty() );
            Map subclass =  Cawls.findFirst( subclasses, new MapBuilder().append( "name", "ProcessException" ) ) ;
            assertNotNull( subclass );
            List<String> libraryVersions = (List<String>) subclass.get("_libraryVersions");
            assertNotNull( libraryVersions );
            assertEquals( 1, libraryVersions.size() );
            assertTrue( libraryVersions.contains( "2.0") );
        }
        
        // TODO: test removing libraryId1 again (need a contains clause)
        /// This should have no effect...
        new AllKnownSubclassesQuery().inject(db).removeLibrary(libraryId1);
        
        // Verify it's STILL there
        {
            List<Map> subclasses = new AllKnownSubclassesQuery().inject(db).query("java.io.IOException");
            assertFalse( subclasses.isEmpty() );
            Map subclass =  Cawls.findFirst( subclasses, new MapBuilder().append( "name", "ProcessException" ) ) ;
            assertNotNull( subclass );
            List<String> libraryVersions = (List<String>) subclass.get("_libraryVersions");
            assertNotNull( libraryVersions );
            assertEquals( 1, libraryVersions.size() );
            assertTrue( libraryVersions.contains( "2.0") );
        }
        
        // And remove again
        new AllKnownSubclassesQuery().inject(db).removeLibrary(libraryId2);
        
        // Verify it's gone
        {
            List<Map> subclasses = new AllKnownSubclassesQuery().inject(db).query("java.io.IOException");
            assertNull( Cawls.findFirst( subclasses, new MapBuilder().append( "name", "ProcessException" ) ) );
        }
    }

}


