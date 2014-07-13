package com.surfapi.web.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.Arrays;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.db.DBService;
import com.surfapi.db.MongoDBImpl;
import com.surfapi.db.post.AllKnownImplementorsQuery;
import com.surfapi.db.post.AllKnownSubclassesQuery;
import com.surfapi.db.post.ReferenceNameQuery;
import com.surfapi.javadoc.SimpleJavadocProcess;
import com.surfapi.junit.CaptureSystemOutRule;
import com.surfapi.junit.DropMongoDBRule;
import com.surfapi.junit.MongoDBProcessRule;
import com.surfapi.log.Log;

/**
 * Query REST target.
 */
public class QueryRestTest extends JerseyTest {
    
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
     * Drops the given db before/after each test.
     */
    @ClassRule
    public static DropMongoDBRule dropMongoDBRule = new DropMongoDBRule( mongoDBProcessRule, MongoDbName );
    
    /**
     * Capture and suppress stdout unless the test fails.
     */
    @Rule
    public CaptureSystemOutRule systemOutRule  = new CaptureSystemOutRule( );

    /**
     * Install AutoComplete into the Jersey test container.
     */
    @Override
    protected Application configure() {
        return new ResourceConfig(QueryRest.class);
    }
    
    /**
     * 
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        // Setup the db.
        String libraryId = "/java/com.surfapi/1.0";
        
        File baseDir = new File("src/test/java");
        
        SimpleJavadocProcess javadocProcess = new SimpleJavadocProcess()
                                                    .setMongoUri( MongoUri )
                                                    .setLibraryId( libraryId )
                                                    .setSourcePath( baseDir )
                                                    .setPackages( Arrays.asList( "com.surfapi.test" ) );
        javadocProcess.run();
        
        // Build the backend indexes
        new ReferenceNameQuery().inject(new MongoDBImpl(MongoDbName)).buildIndex();
        new AllKnownSubclassesQuery().inject(new MongoDBImpl(MongoDbName)).buildIndex();
        new AllKnownImplementorsQuery().inject(new MongoDBImpl(MongoDbName)).buildIndex();
    }
    
 
    /**
     * 
     */
    @Test
    public void testQueryReferenceName() throws Exception {
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        // Need to set this because the rest handler uses DBService.getDb()
        DBService.setDb( new MongoDBImpl( MongoDbName ) );
        
        String responseMsg = target().path("q/java/qn/com.surfapi.test.DemoJavadoc+parse").request().get(String.class);
        
        Log.trace(this, "testQueryReferenceName: responseMsg: " + responseMsg);

        // Verify the json response is parseable.
        JSONArray jsonArr = (JSONArray) new JSONParser().parse( responseMsg );
        assertFalse( jsonArr.isEmpty() );
    }
    
    /**
     * 
     */
    @Test
    public void testAllKnownSubclasses() throws Exception {
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        // Need to set this because the rest handler uses DBService.getDb()
        DBService.setDb( new MongoDBImpl( MongoDbName ) );
        
        String responseMsg = target().path("q/java/allKnownSubclasses/com.surfapi.test.DemoJavadoc").request().get(String.class);
        
        Log.trace(this, "testAllKnownSubclasses: responseMsg: " + responseMsg);

        // Verify the json response is parseable.
        JSONArray jsonArr = (JSONArray) new JSONParser().parse( responseMsg );
        assertFalse( jsonArr.isEmpty() );
    }
    
    /**
     * 
     */
    @Test
    public void testAllKnownImplementors() throws Exception {
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        // Need to set this because the rest handler uses DBService.getDb()
        DBService.setDb( new MongoDBImpl( MongoDbName ) );
        
        String responseMsg = target().path("q/java/allKnownImplementors/com.surfapi.test.DemoInterface").request().get(String.class);
        
        Log.trace(this, "testAllKnownImplementors: responseMsg: " + responseMsg);

        // Verify the json response is parseable.
        JSONArray jsonArr = (JSONArray) new JSONParser().parse( responseMsg );
        assertFalse( jsonArr.isEmpty() );
    }
}
