package com.surfapi.web.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.DB;
import com.surfapi.db.DBImpl;
import com.surfapi.db.DBLoader;
import com.surfapi.db.DBService;
import com.surfapi.junit.CaptureSystemOutRule;

/**
 * Auto-complete REST target.
 */
public class DbRestTest extends JerseyTest {

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
    public static void beforeClass() {
        
        DB db = new DBImpl();
        new DBLoader().inject( db ).loadUnchecked(new File("src/test/resources") );
        DBService.setDb(db);
    }

    /**
     * 
     */
    @Test
    public void testLookup() throws Exception {
        String path = "/java/DBTest.test/1.0.3/com.surfapi.test.DemoJavadoc";
        String responseMsg = target().path( path ).request().get(String.class);

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
