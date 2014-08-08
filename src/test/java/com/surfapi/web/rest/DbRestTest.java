package com.surfapi.web.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.DBLoader;
import com.surfapi.db.DBService;
import com.surfapi.db.MongoDBImpl;
import com.surfapi.junit.CaptureSystemOutRule;
import com.surfapi.junit.MongoDBProcessRule;
import com.surfapi.log.Log;

/**
 * Auto-complete REST target.
 */
public class DbRestTest extends JerseyTest {
    
    /**
     * Executed before and after the entire collection of tests (like @BeforeClass/@AfterClass).
     * 
     * Ensures a mongodb process is started.
     */
    @ClassRule
    public static MongoDBProcessRule mongoDBProcessRule = new MongoDBProcessRule("test1");
    
    /**
     * Capture and suppress stdout unless the test fails.
     */
    @Rule
    public CaptureSystemOutRule systemOutRule  = new CaptureSystemOutRule( );
    
    /**
     * Install DbRest into the Jersey test container.
     */
    @Override
    protected Application configure() {
        return new ResourceConfig(DbRest.class);
    }
    
    /**
     * Setup  DB dataDir = src/test/resources for test data
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        assumeTrue( mongoDBProcessRule.isStarted() );
        DBService.setDb( new MongoDBImpl("test1" ) );
        new DBLoader().inject( DBService.getDb() ).loadUnchecked(new File("src/test/resources") );
    }

    /**
     * 
     */
    @Test
    public void testLookup() throws Exception {
        String path = "/java/DBTest.test/1.0.3/com.surfapi.test.DemoJavadoc";
        String responseMsg = target().path( path ).request().get(String.class);

        Log.trace(this, "testLookup: responseMsg: " + responseMsg);
        
        // Verify the json response is parseable.
        JSONObject jsonObj = (JSONObject) new JSONParser().parse( responseMsg );
        // System.out.println("JSON: " + JSONTrace.prettyPrint(jsonArr));
        
        assertEquals( "class", jsonObj.get("metaType"));
        assertEquals( "DemoJavadoc", jsonObj.get("name"));
      
    }
    
    /**
     * 
     */
    @Test
    public void testLookupExcludeFields() throws Exception {
        String path = "/java/DBTest.test/1.0.3/com.surfapi.test.DemoJavadoc";
        String responseMsg = target().path( path ).request().get(String.class);
        Log.trace(this, "responseMsg: " + responseMsg);
        
        JSONObject jsonObj = (JSONObject) new JSONParser().parse( responseMsg );
        
        assertEquals( "class", jsonObj.get("metaType"));
        assertEquals( "DemoJavadoc", jsonObj.get("name"));
        assertTrue( jsonObj.containsKey("methods") );
        
        responseMsg = target().path( path ).queryParam("methods", "0").request().get(String.class);
        Log.trace(this, "responseMsg: (no methods) " + responseMsg);
        
        jsonObj = (JSONObject) new JSONParser().parse( responseMsg );
        
        assertEquals( "class", jsonObj.get("metaType"));
        assertEquals( "DemoJavadoc", jsonObj.get("name"));
        assertFalse( jsonObj.containsKey("methods") );
        
        responseMsg = target().path( path ).queryParam("methods", "0").queryParam("metaType", "0").request().get(String.class);
        Log.trace(this, "responseMsg: (no methods, metaType) " + responseMsg);
        
        jsonObj = (JSONObject) new JSONParser().parse( responseMsg );
        
        assertEquals( "DemoJavadoc", jsonObj.get("name"));
        assertFalse( jsonObj.containsKey("methods") );
        assertFalse( jsonObj.containsKey("metaType") );
    }
    
    /**
     * 
     */
    @Test
    public void testLookupIncludeFields() throws Exception {
        String path = "/java/DBTest.test/1.0.3/com.surfapi.test.DemoJavadoc";
        String responseMsg = target().path( path ).request().get(String.class);
        Log.trace(this, "responseMsg: " + responseMsg);
        
        JSONObject jsonObj = (JSONObject) new JSONParser().parse( responseMsg );
        
        assertEquals( "class", jsonObj.get("metaType"));
        assertEquals( "DemoJavadoc", jsonObj.get("name"));
        
        responseMsg = target().path( path ).queryParam("name", "1").request().get(String.class);
        Log.trace(this, "responseMsg: (only name) " + responseMsg);
        
        jsonObj = (JSONObject) new JSONParser().parse( responseMsg );
        
        assertEquals( "DemoJavadoc", jsonObj.get("name"));
        assertEquals( 2, jsonObj.size() ); // _id is always included.
    }
    
    
    /**
     * 
     */
    @Test
    public void testGetLangLibraries() throws Exception {
        String responseMsg = target().path( "/java" ).request().get(String.class);

        // Verify the json response is parseable.
        JSONObject jsonObj = (JSONObject) new JSONParser().parse( responseMsg );
               
        assertEquals( "java", jsonObj.get("name"));
        assertNotNull( Cawls.findFirst( (List) jsonObj.get("libraries"), new MapBuilder().append("name","DBTest.test")));
    }
    
    /**
     * 
     */
    @Test
    public void testGetLibrary() throws Exception {
        String responseMsg = target().path( "/java/DBTest.test/1.0.3" ).request().get(String.class);

        // Verify the json response is parseable.
        JSONObject jsonObj = (JSONObject) new JSONParser().parse( responseMsg );
               
        assertNotNull( jsonObj );
        List<Map> pkgs = (List<Map>) jsonObj.get("packages");
        assertNotNull( pkgs );
        assertEquals( "com.surfapi.test", pkgs.get(0).get("name") );
        assertEquals( "/java/DBTest.test/1.0.3/com.surfapi.test", pkgs.get(0).get("_id") );
        
    }
    
    /**
     * 
     */
    @Test
    public void testGetLibraryVersions() throws Exception {
        String responseMsg = target().path( "/java/DBTest.test" ).request().get(String.class);

        // Verify the json response is parseable.
        JSONObject jsonObj = (JSONObject) new JSONParser().parse( responseMsg );
               
        assertEquals( "DBTest.test", jsonObj.get("name"));
        assertEquals( 2, ((List)jsonObj.get("versions")).size() );
        assertNotNull( Cawls.findFirst( (List) jsonObj.get("versions"), new MapBuilder().append("name","DBTest.test")
                                                                                        .append("version", "1.0.3") ));
        assertNotNull( Cawls.findFirst( (List) jsonObj.get("versions"), new MapBuilder().append("name","DBTest.test")
                                                                                        .append("version", "1.0.2") ));
    }
    
}
