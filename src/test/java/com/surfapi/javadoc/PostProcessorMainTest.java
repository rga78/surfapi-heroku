
package com.surfapi.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.util.List;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.DB;
import com.surfapi.db.MongoDBService;
import com.surfapi.db.post.AutoCompleteIndex;
import com.surfapi.db.post.ReferenceNameQuery;
import com.surfapi.junit.CaptureSystemOutRule;
import com.surfapi.junit.DropMongoDBRule;
import com.surfapi.junit.MongoDBProcessRule;


public class PostProcessorMainTest {

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

    @Test
    public void test() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        // Setup mongodb service
        MongoDBService.setDbName( "test1" );
        
        String libraryId = "/java/com.surfapi/1.0";
        
        JavadocMain.main( new String[] { libraryId, "src/main/java/com/surfapi/coll", "src/test/java/com/surfapi/test" } );

        DB db = MongoDBService.getDb();

        List<Map> docs = db.find( libraryId, new MapBuilder() );
        
        assertFalse( docs.isEmpty() );
        
        // Verify a few expected docs
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "com.surfapi.coll" ) ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "Cawls" ) ) );
        
        // Run the post-processor
        PostProcessorMain.main( new String[] { libraryId } );
        

        // Verify reference query ---------------------------
        docs = new ReferenceNameQuery().inject(db).query( "com.surfapi.coll.Cawls" );
        assertFalse( docs.isEmpty() );
        assertEquals( 1, docs.size() );
        
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/1.0/com.surfapi.coll.Cawls") ) );
        
        // "test" classes should have been skipped.
        docs = new ReferenceNameQuery().inject(db).query( "com.surfapi.test.DemoJavadoc" );
        assertTrue( docs.isEmpty());


        // Verify auto-complete index ------------------------
        docs = new AutoCompleteIndex().inject(db).query( "java", "Cawl", 25 );
        assertFalse( docs.isEmpty() );
        for (Map doc : docs) {
            assertTrue( ((String)doc.get("name")).startsWith("Cawl") );
        }
    

        // Verify SetStubIds ------------------------
        Map doc = db.read( "/java/com.surfapi/1.0/com.surfapi.coll.Cawls" );
        assertNotNull( doc );
        docs = (List<Map>) doc.get("methods");
        assertFalse( docs.isEmpty() );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/1.0/com.surfapi.coll.Cawls.pick(java.util.Map,java.util.Collection)" ) ) );
        for (Map doc1 : docs) {
            assertNotNull( doc1.get("_id" ) );
        }
    }

}

