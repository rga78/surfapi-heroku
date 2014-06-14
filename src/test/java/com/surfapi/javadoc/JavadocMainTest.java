package com.surfapi.javadoc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.List;
import java.util.Map;

import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.MongoDBService;
import com.surfapi.junit.CaptureSystemOutRule;
import com.surfapi.junit.DropMongoDBRule;
import com.surfapi.junit.MongoDBProcessRule;

/**
 * 
 */
public class JavadocMainTest {
    
    /**
     * Executed before and after the entire collection of tests (like @BeforeClass/@AfterClass).
     * 
     * Ensures a mongodb process is started.
     */
    @ClassRule
    public static MongoDBProcessRule mongoDBProcessRule = new MongoDBProcessRule();
    
    /**
     * Drops the given db before/after each test.
     */
    @Rule
    public DropMongoDBRule dropMongoDBRule = new DropMongoDBRule( mongoDBProcessRule, "test1" );
    
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
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        MongoDBService.setDbName( "test1" );
        
        String libraryId = "/java/com.surfapi/1.0";
        
        new JavadocMain()
                .setDocletPath( JavadocMain.buildDocletPath() )
                .setDirFilter( TrueFileFilter.INSTANCE )
                .consumeArgs( new String[] { libraryId, "src/main/java/com/surfapi/proc", "src/test/java/com/surfapi/test" } )
                .go( );
        
        List<Map> docs = MongoDBService.getDb().find( libraryId, new MapBuilder());
        
        assertFalse( docs.isEmpty() );
       
        // Verify the two packages are in there.
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "com.surfapi.proc" ) ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "com.surfapi.test" ) ) );

        // Verify the library has been added to the libraries collection
        List<Map> libs = MongoDBService.getDb().getLibraryList( "java" );
        assertFalse( libs.isEmpty() );
        assertNotNull( Cawls.findFirst( libs, new MapBuilder().append( "_id", libraryId ) ) );

        assertNotNull( MongoDBService.getDb().getLibrary(libraryId) );
    }

    /**
     * 
     */
    @Test
    public void testOptionAll() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        MongoDBService.setDbName( "test1" );
        
        String libraryId = "/java/com.surfapi/1.0";
        
        JavadocMain.main( new String[] { "--all", libraryId, "src/main/java/com/surfapi/proc", "src/test/java/com/surfapi/test" } );
       
        List<Map> docs = MongoDBService.getDb().find( libraryId, new MapBuilder());
        
        assertFalse( docs.isEmpty() );
       
        // Verify the two packages are in there.
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "com.surfapi.proc" ) ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "com.surfapi.test" ) ) );

        // Verify the library has been added to the libraries collection
        List<Map> libs = MongoDBService.getDb().getLibraryList( "java" );
        assertFalse( libs.isEmpty() );
        assertNotNull( Cawls.findFirst( libs, new MapBuilder().append( "_id", libraryId ) ) );

        assertNotNull( MongoDBService.getDb().getLibrary(libraryId) );
    }
    
    /**
     * 
     */
    @Test
    public void testFilterOutTest() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        MongoDBService.setDbName( "test1" );
        
        String libraryId = "/java/com.surfapi/1.0";
        
        List<Map> docs = MongoDBService.getDb().find( libraryId, new MapBuilder());
        
        assertTrue( docs.isEmpty() );
        
        JavadocMain.main( new String[] { libraryId, "src/main/java/com/surfapi/proc", "src/test/java/com/surfapi/test" } );
        
        docs = MongoDBService.getDb().find( libraryId, new MapBuilder());
        
        assertFalse( docs.isEmpty() );
       
        // Verify the proc package is there but not the test.
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "com.surfapi.proc" ) ) );
        assertNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "com.surfapi.test" ) ) );

        // Verify the library has been added to the libraries collection
        List<Map> libs = MongoDBService.getDb().getLibraryList( "java" );
        assertFalse( libs.isEmpty() );
        assertNotNull( Cawls.findFirst( libs, new MapBuilder().append( "_id", libraryId ) ) );

        assertNotNull( MongoDBService.getDb().getLibrary(libraryId) );
    }

    /**
     * 
     */
    @Test
    public void testOnJdk16() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        MongoDBService.setDbName( "test1" );
        
        String libraryId = "/java/java-sdk/1.6";
        
        JavadocMain.main( new String[] { libraryId,
                                    "/fox/tmp/javadoc/jdk6.src/jdk/src/share/classes/java/lang",
                                    "/fox/tmp/javadoc/jdk6.src/jdk/src/share/classes/java/net",
                                    "/fox/tmp/javadoc/jdk6.src/jdk/src/share/classes/java/util",
                                    "/fox/tmp/javadoc/jdk6.src/jdk/src/share/classes/java/io"
                                  });
        
        List<Map> docs = MongoDBService.getDb().find( libraryId, new MapBuilder());
        
        assertFalse( docs.isEmpty() );
        
        // Verify the java.lang package is in there.
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "java.lang" ) ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "java.net" ) ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "java.util" ) ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "java.util.concurrent" ) ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "java.io" ) ) );

        // Verify the library has been added to the libraries collection
        List<Map> libs = MongoDBService.getDb().getLibraryList( "java" );
        assertFalse( libs.isEmpty() );
        assertNotNull( Cawls.findFirst( libs, new MapBuilder().append( "_id", libraryId ) ) );

        assertNotNull( MongoDBService.getDb().getLibrary(libraryId) );
    }
}
