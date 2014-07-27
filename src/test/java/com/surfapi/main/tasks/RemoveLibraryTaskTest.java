package com.surfapi.main.tasks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.MongoDBImpl;
import com.surfapi.db.post.AllKnownSubclassesQuery;
import com.surfapi.javadoc.SimpleJavadocProcess;
import com.surfapi.junit.CaptureSystemOutRule;
import com.surfapi.junit.MongoDBProcessRule;

public class RemoveLibraryTaskTest {

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
    @Test
    public void testRemoveLibrary() throws Exception {
        
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
        
        // Add the library to all indexes
        new BuildIndexTask().inject(db).handleTask( new String[] { "--libraryId=" + libraryId } );
        
        // Verify it's there 
        assertNotNull( db.read( libraryId + "/com.surfapi.proc.ProcessHelper") );
        
        // Verify it's in the indexes (just checking one index)
        List<Map> subclasses = new AllKnownSubclassesQuery().inject(db).query("java.io.IOException");
        assertFalse( subclasses.isEmpty() );
        assertNotNull( Cawls.findFirst( subclasses, new MapBuilder().append( "name", "ProcessException" ) ) );
        
        // Verify it's in the libraries list
        assertTrue( db.getLibraryIds("java").contains(libraryId) );
        
        // Now remove
        new RemoveLibraryTask().inject(db).handleTask( new String[] { "--libraryId=" + libraryId } );
        
        // Verify it's gone
        assertNull( db.read( libraryId + "/com.surfapi.proc.ProcessHelper") );
        
        // Verify it's gone from the indexes
        subclasses = new AllKnownSubclassesQuery().inject(db).query("java.io.IOException");
        assertNull( Cawls.findFirst( subclasses, new MapBuilder().append( "name", "ProcessException" ) ) );
        
        // Verify it's not in the libraries list
        assertFalse( db.getLibraryIds("java").contains(libraryId) );
        
    }
}
