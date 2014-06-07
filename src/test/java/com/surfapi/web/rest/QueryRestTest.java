package com.surfapi.web.rest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeTrue;

import java.io.File;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.db.DBLoader;
import com.surfapi.db.DBService;
import com.surfapi.db.MongoDBImpl;
import com.surfapi.db.post.ReferenceNameQuery;
import com.surfapi.junit.DropMongoDBRule;
import com.surfapi.junit.MongoDBProcessRule;

/**
 * Query REST target.
 */
public class QueryRestTest extends JerseyTest {
    
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
     * Install AutoComplete into the Jersey test container.
     */
    @Override
    protected Application configure() {
        return new ResourceConfig(QueryRest.class);
    }
    
 
    /**
     * 
     */
    @Test
    public void testQueryReferenceName() throws Exception {
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        DBService.setDb( new MongoDBImpl("test1" ) );
        new DBLoader().inject( DBService.getDb() ).loadFile(new File("src/test/resources/com.surfapi_1.0.json") );
        new ReferenceNameQuery().inject(DBService.getDb()).buildIndex();
        
        String responseMsg = target().path("q/java/qn/com.surfapi.test.DemoJavadoc+parse").request().get(String.class);
        
        // Log.trace(this, "testQueryIndex: responseMsg: " + responseMsg);

        // Verify the json response is parseable.
        JSONArray jsonArr = (JSONArray) new JSONParser().parse( responseMsg );
        assertFalse( jsonArr.isEmpty() );
    }
}
