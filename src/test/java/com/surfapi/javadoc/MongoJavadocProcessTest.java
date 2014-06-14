package com.surfapi.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.coll.MapBuilder;
import com.surfapi.db.MongoDBImpl;
import com.surfapi.junit.CaptureSystemOutRule;
import com.surfapi.junit.DropMongoDBRule;
import com.surfapi.junit.MongoDBProcessRule;
import com.surfapi.log.Log;

/**
 * 
 */
public class MongoJavadocProcessTest {

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
        
        File baseDir = new File("src/test/java/com/surfapi/test");
        MongoJavadocProcess javadocProcess = new MongoJavadocProcess(baseDir)
                                                    .setDocletPath( JavadocMain.buildDocletPath() )
                                                    .setDirFilter( TrueFileFilter.INSTANCE )
                                                    .setMongoDBName( "test1" )
                                                    .setLibraryId( "/java/com.surfapi.test/1.0" );
        
        
        List<String> expectedCommand = new ArrayList<String>( Arrays.asList( new String[] { "javadoc", 
                                                                     "-docletpath",
                                                                     javadocProcess.getDocletPath(),
                                                                     "-doclet",
                                                                     MongoDoclet.class.getCanonicalName(),
                                                                     "-J-Dcom.surfapi.mongo.db.name=test1",
                                                                     "-J-Dcom.surfapi.mongo.library.id=/java/com.surfapi.test/1.0",
                                                                     "-quiet" } ) );
        
        List<String> javaFileNames = FileSystemUtils.listJavaFileNames(baseDir);
        expectedCommand.addAll(javaFileNames);
        
        assertEquals(expectedCommand, javadocProcess.buildCommand(javaFileNames));
    }

    /**
     * 
     */
    @Test
    public void testJavadoc() throws Exception {
        
        String dbName = "test1";
        String libraryId = "/java/com.surfapi.test/1.0";
        
        File baseDir = new File("src/test/java/com/surfapi/test");
        MongoJavadocProcess javadocProcess = new MongoJavadocProcess(baseDir)
                                                    .setDocletPath( JavadocMain.buildDocletPath() )
                                                    .setDirFilter( TrueFileFilter.INSTANCE )
                                                    .setMongoDBName( dbName )
                                                    .setLibraryId(libraryId );
        
        
        javadocProcess.run();
        
        List<Map> docs = new MongoDBImpl(dbName).find( libraryId, new MapBuilder());
        
        assertFalse( docs.isEmpty() );
        assertEquals( 34, docs.size() );
        
        for (Map doc : docs) {
            Log.trace(this,"testJavadoc: " + doc.get("name") + ": " + doc.get("_id") );
        }
        
    }
    
    
}
