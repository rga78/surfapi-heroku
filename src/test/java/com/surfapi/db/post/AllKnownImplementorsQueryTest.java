
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
public class AllKnownImplementorsQueryTest {
    
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
        
        SimpleJavadocProcess javadocProcess = new SimpleJavadocProcess()
                                                    .setMongoUri( MongoUri )
                                                    .setLibraryId( libraryId )
                                                    .setSourcePath( baseDir )
                                                    .setPackages( Arrays.asList( "com.surfapi.test" ) );
        javadocProcess.run();
        
        // add another version of the library
        new SimpleJavadocProcess()
                .setMongoUri( MongoUri )
                .setLibraryId( "/java/com.surfapi/0.9" )
                .setSourcePath( baseDir )
                .setPackages( Arrays.asList( "com.surfapi.test" ) )
                .run();
        
        // Build the all known subclasses index
        new AllKnownImplementorsQuery().inject(new MongoDBImpl(MongoDbName)).buildIndex();
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
    public void testGetImpls() throws Exception {

        DB db = new MongoDBImpl(MongoDbName) ;
        
        String libraryId = "/java/com.surfapi/1.0";

        List<Map> impls = new AllKnownImplementorsQuery().inject(db).query("com.surfapi.test.DemoInterface");

        assertEquals(3, impls.size());
        assertNotNull( Cawls.findFirst( impls, new MapBuilder().append("qualifiedName", "com.surfapi.test.DemoJavadoc")) );
        assertNotNull( Cawls.findFirst( impls, new MapBuilder().append("qualifiedName", "com.surfapi.test.DemoJavadocSubClass")) );
        assertNotNull( Cawls.findFirst( impls, new MapBuilder().append("qualifiedName", "com.surfapi.test.DemoJavadocSubClass2")) );
        
        impls = new AllKnownImplementorsQuery().inject(db).query("com.surfapi.test.DemoInterface2");

        assertEquals(0, impls.size());
       
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
        new AllKnownImplementorsQuery().inject(db).addLibraryToIndex(libraryId);
        
        // Verify it's there 
        List<Map> impls = new AllKnownImplementorsQuery().inject(db).query("java.util.Observer");
        assertFalse( impls.isEmpty() );
        assertNotNull( Cawls.findFirst( impls, new MapBuilder().append( "name", "StreamCollector" ) ) );
        assertNotNull( Cawls.findFirst( impls, new MapBuilder().append( "name", "StreamPiper" ) ) );
        
        // Now remove
        new AllKnownImplementorsQuery().inject(db).removeLibrary(libraryId);
        
        // Verify it's gone
        impls = new AllKnownImplementorsQuery().inject(db).query("java.util.Observer");
        assertNull( Cawls.findFirst( impls, new MapBuilder().append( "name", "StreamCollector" ) ) );
        assertNull( Cawls.findFirst( impls, new MapBuilder().append( "name", "StreamPiper" ) ) );
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

        new AllKnownImplementorsQuery().inject(db).addLibraryToIndex(libraryId1);
        new AllKnownImplementorsQuery().inject(db).addLibraryToIndex(libraryId2);
        
        // Verify it's there 
        {
            List<Map> impls = new AllKnownImplementorsQuery().inject(db).query("java.util.Observer");
            assertFalse( impls.isEmpty() );
            assertNotNull( Cawls.findFirst( impls, new MapBuilder().append( "name", "StreamCollector" ) ) );
            assertNotNull( Cawls.findFirst( impls, new MapBuilder().append( "name", "StreamPiper" ) ) );
        
            Map impl =  Cawls.findFirst( impls, new MapBuilder().append( "name", "StreamCollector" ) ) ;
            assertNotNull( impl );
            List<String> libraryVersions = (List<String>) impl.get("_libraryVersions");
            assertNotNull( libraryVersions );
            assertEquals( 2, libraryVersions.size() );
            assertTrue( libraryVersions.contains( "1.0") );
            assertTrue( libraryVersions.contains( "2.0") );
        }
        
        // Now remove
        new AllKnownImplementorsQuery().inject(db).removeLibrary(libraryId1);
        
        // Verify it's still there, only with version removed 
        {
            List<Map> impls = new AllKnownImplementorsQuery().inject(db).query("java.util.Observer");
            assertFalse( impls.isEmpty() );
            assertNotNull( Cawls.findFirst( impls, new MapBuilder().append( "name", "StreamCollector" ) ) );
            assertNotNull( Cawls.findFirst( impls, new MapBuilder().append( "name", "StreamPiper" ) ) );
        
            Map impl =  Cawls.findFirst( impls, new MapBuilder().append( "name", "StreamCollector" ) ) ;
            assertNotNull( impl );
            List<String> libraryVersions = (List<String>) impl.get("_libraryVersions");
            assertNotNull( libraryVersions );
            assertEquals( 1, libraryVersions.size() );
            assertTrue( libraryVersions.contains( "2.0") );
        }
        
        // remove library1 again (this should have no effect)
        new AllKnownImplementorsQuery().inject(db).removeLibrary(libraryId1);
        
        // Verify it's still there, only with version removed 
        {
            List<Map> impls = new AllKnownImplementorsQuery().inject(db).query("java.util.Observer");
            assertFalse( impls.isEmpty() );
            assertNotNull( Cawls.findFirst( impls, new MapBuilder().append( "name", "StreamCollector" ) ) );
            assertNotNull( Cawls.findFirst( impls, new MapBuilder().append( "name", "StreamPiper" ) ) );
        
            Map impl =  Cawls.findFirst( impls, new MapBuilder().append( "name", "StreamCollector" ) ) ;
            assertNotNull( impl );
            List<String> libraryVersions = (List<String>) impl.get("_libraryVersions");
            assertNotNull( libraryVersions );
            assertEquals( 1, libraryVersions.size() );
            assertTrue( libraryVersions.contains( "2.0") );
        }
        
        // now remove again
        new AllKnownImplementorsQuery().inject(db).removeLibrary(libraryId2);
        
        
        // Verify it's gone
        {
            List<Map> impls = new AllKnownImplementorsQuery().inject(db).query("java.util.Observer");
            assertNull( Cawls.findFirst( impls, new MapBuilder().append( "name", "StreamCollector" ) ) );
            assertNull( Cawls.findFirst( impls, new MapBuilder().append( "name", "StreamPiper" ) ) );
        }
    }


}


