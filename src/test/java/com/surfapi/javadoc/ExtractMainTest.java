
package com.surfapi.javadoc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.MongoDBService;
import com.surfapi.junit.DropMongoDBRule;
import com.surfapi.junit.MongoDBProcessRule;
import com.surfapi.proc.ProcessHelper;

/**
 * 
 */
public class ExtractMainTest {

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
     * 
     */
    @Test
    public void testExtractMainWithJar() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        // First make the jarfile.
        File jarFile = createTestJar("test.jar");
        jarFile.deleteOnExit();
        
        // Setup mongodb service
        MongoDBService.setDbName( "test1" );
        
        String libraryId = "/java/com.surfapi/1.0";
        
        ExtractMain.main( new String[] { libraryId, jarFile.getCanonicalPath() } );

        // TODO: verify the temp directory was cleaned up.  How?
        
        List<Map> docs = MongoDBService.getDb().find( libraryId, new MapBuilder() );
        
        assertFalse( docs.isEmpty() );
        
        // Verify a few expected docs
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "com.surfapi.coll" ) ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "Cawls" ) ) );
    }
    
    /**
     * 
     */
    @Test
    public void testExtractMainWithTar() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        // First make the tar file.
        File tarFile = createTestTar("test.tar");
        tarFile.deleteOnExit();
        
        // Setup mongodb service
        MongoDBService.setDbName( "test1" );
        
        String libraryId = "/java/com.surfapi/1.0";
        
        ExtractMain.main( new String[] { libraryId, tarFile.getCanonicalPath() } );

        // TODO: verify the temp directory was cleaned up.  How?
        
        List<Map> docs = MongoDBService.getDb().find( libraryId, new MapBuilder());
        
        assertFalse( docs.isEmpty() );
        
        // Verify a few expected docs
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "com.surfapi.coll" ) ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "Cawls" ) ) );
    }
    
    private File createTestJar(String jarFileName) throws IOException, InterruptedException {
        
        Process p = new ProcessBuilder("jar", "-cf", jarFileName, "-C", "src/main/java", "com/surfapi/coll").start();
        new ProcessHelper(p).waitFor();
        
        return new File(jarFileName);
    }
    
    private File createTestTar(String tarFileName) throws IOException, InterruptedException {
        
        Process p = new ProcessBuilder("tar", "-cf", tarFileName, "src/main/java/com/surfapi/coll").start();
        new ProcessHelper(p).waitFor();
        
        return new File(tarFileName);
    }

}
