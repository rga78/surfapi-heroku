package com.surfapi.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.MongoDBImpl;
import com.surfapi.junit.CaptureSystemOutRule;
import com.surfapi.junit.DropMongoDBRule;
import com.surfapi.junit.MongoDBProcessRule;
import com.surfapi.log.Log;

/**
 * 
 */
public class SimpleJavadocProcessTest {

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
    public void testBuildCommand() throws Exception {
        
        File sourcePath = new File("src/test/java");
        SimpleJavadocProcess javadocProcess = new SimpleJavadocProcess()
                                                    .setMongoDBName( "test1" )
                                                    .setLibraryId( "/java/com.surfapi.test/1.0" )
                                                    .setSourcePath( sourcePath )
                                                    .setPackages( Arrays.asList( "com.surfapi.test", "com.surfapi.coll" ) );
        
        List<String> expectedCommand = new ArrayList<String>( Arrays.asList( new String[] { "javadoc", 
                                                                                            "-docletpath",
                                                                                            JavadocMain.buildDocletPath(),
                                                                                            "-doclet",
                                                                                            MongoDoclet.class.getCanonicalName(),
                                                                                            "-J-Xms1024m",
                                                                                            "-J-Xmx4096m",
                                                                                            "-J-Dcom.surfapi.mongo.db.name=test1",
                                                                                            "-J-Dcom.surfapi.mongo.library.id=/java/com.surfapi.test/1.0",
                                                                                            "-sourcepath",
                                                                                            sourcePath.getCanonicalPath(),
                                                                                            "com.surfapi.test",
                                                                                            "com.surfapi.coll"
                                                                                          } ) );
        assertEquals(expectedCommand, javadocProcess.buildCommand());
    }


    /**
     * 
     */
    @Test
    public void testBuildCommandWithSubpackages() throws Exception {
        
        File sourcePath = new File("src/test/java");
        SimpleJavadocProcess javadocProcess = new SimpleJavadocProcess()
                                                    .setMongoDBName( "test1" )
                                                    .setLibraryId( "/java/com.surfapi.test/1.0" )
                                                    .setSourcePath( sourcePath )
                                                    .setSubpackages( Arrays.asList( "com.surfapi.test", "com.surfapi.coll" ) );
        
        List<String> expectedCommand = new ArrayList<String>( Arrays.asList( new String[] { "javadoc", 
                                                                                            "-docletpath",
                                                                                            JavadocMain.buildDocletPath(),
                                                                                            "-doclet",
                                                                                            MongoDoclet.class.getCanonicalName(),
                                                                                            "-J-Xms1024m",
                                                                                            "-J-Xmx4096m",
                                                                                            "-J-Dcom.surfapi.mongo.db.name=test1",
                                                                                            "-J-Dcom.surfapi.mongo.library.id=/java/com.surfapi.test/1.0",
                                                                                            "-sourcepath",
                                                                                            sourcePath.getCanonicalPath(),
                                                                                            "-subpackages",
                                                                                            "com.surfapi.test",
                                                                                            "-subpackages",
                                                                                            "com.surfapi.coll"
                                                                                          } ) );
        assertEquals(expectedCommand, javadocProcess.buildCommand());
    }

    /**
     * 
     */
    @Test
    public void testJavadoc() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        
        String dbName = "test1";
        String libraryId = "/java/com.surfapi.test/1.0";
        File sourcePath = new File("src/test/java");

        SimpleJavadocProcess javadocProcess = new SimpleJavadocProcess()
                                                    .setMongoDBName( dbName )
                                                    .setLibraryId( libraryId )
                                                    .setSourcePath( sourcePath )
                                                    .setPackages( Arrays.asList( "com.surfapi.test" ) );
        javadocProcess.run();
        
        List<Map> docs = new MongoDBImpl(dbName).find( libraryId, new MapBuilder());
        
        assertFalse( docs.isEmpty() );
        assertEquals( JsonDocletTest.ExpectedTestJavadocSize, docs.size() );
        
        for (Map doc : docs) {
            Log.trace(this,"testJavadoc: " + doc.get("name") + ": " + doc.get("_id") );
        }
    }
    

    /**
     * 
     */
    @Test
    public void testJavadocWithSubpackages() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        
        String dbName = "test1";
        String libraryId = "/java/com.surfapi.test/1.0";
        File sourcePath = new File("src/test/java");

        SimpleJavadocProcess javadocProcess = new SimpleJavadocProcess()
                                                    .setMongoDBName( dbName )
                                                    .setLibraryId( libraryId )
                                                    .setSourcePath( sourcePath )
                                                    .setSubpackages( Arrays.asList( "com.surfapi.db" ) );
        javadocProcess.run();
        
        List<Map> docs = new MongoDBImpl(dbName).find( libraryId, new MapBuilder());
        
        assertFalse( docs.isEmpty() );

        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "qualifiedName", "com.surfapi.db.MongoDBImplTest" ) ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "qualifiedName", "com.surfapi.db.post.AutoCompleteIndexTest" ) ) );
        
        for (Map doc : docs) {
            Log.trace(this,"testJavadocWithSubpackages: " + doc.get("name") + ": " + doc.get("_id") );
        }
    }

    
}

