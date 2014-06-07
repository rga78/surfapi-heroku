package com.surfapi.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.post.ReferenceNameQuery;
import com.surfapi.junit.DropMongoDBRule;
import com.surfapi.junit.MongoDBProcessRule;


public class DBLoaderTest {

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
    public void testParseLibraryId() {
        
        assertEquals("/java/com.surfapi/1.0",  DBLoader.parseLibraryId( "com.surfapi_1.0.json") );
        assertEquals("/java/com.surfapi/1.0",  DBLoader.parseLibraryId( "com.surfapi_1.0") );
        assertEquals("/java/com.surfapi/0",  DBLoader.parseLibraryId( "com.surfapi.json") );
        assertEquals("/java/com.surfapi/0",  DBLoader.parseLibraryId( "com.surfapi") );
        assertEquals("/java/mongo-java-driver/2.9.3",  DBLoader.parseLibraryId( "mongo-java-driver_2.9.3.json") );
    }
    
    /**
     * 
     */
    @Test(expected=IllegalArgumentException.class)
    public void testParseLibraryIdBad() {
        
        DBLoader.parseLibraryId("too_many_underscores.json");
    }
    
    /**
     * This test method can be used to load an arbitrary *.json file into an arbitrary db.
     */
    @Test
    public void testTheWholeShebang() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        String dbName = System.getProperty("db.name");
        String jsonFileName = System.getProperty("json.file") ;
        
        assumeTrue( !StringUtils.isEmpty(dbName));
        assumeTrue( !StringUtils.isEmpty(jsonFileName));
        
        File jsonFile = new File(jsonFileName );
        assumeTrue( jsonFile.exists() );
        
        DB db = new MongoDBImpl(dbName);
        new DBLoader().inject(db).doTheWholeShebang( jsonFile );
        
        String libraryId = DBLoader.parseLibraryId( jsonFile.getName() );
        Map library = JavadocMapUtils.mapLibraryId( libraryId );
        
        // Verify it's been added everywhere we expect it to be...
        assertNotNull( Cawls.findFirst( db.getLibraryVersions((String) library.get("lang"), (String) library.get("name")),
                                        new MapBuilder().append( "version", library.get("version") ) ) );
        
        // I could check ReferenceNameQuery and AutoCompleteIndex, but then I'd have 
        // to look at the actual documents in the library... eh.
        
        List<Map> docs = db.find( ReferenceNameQuery.CollectionName, new MapBuilder().append( JavadocMapUtils.LibraryFieldName, library) ) ;
        assertFalse( docs.isEmpty() );
       
    }
    
    /**
     * 
     */
    // @Test
    public void testSetStubIds() throws Exception {
        
        DB db = new DBImpl();
        new DBLoader().inject(db).loadFile( new File("src/test/resources/com.surfapi_1.0.json") );
        
        String libraryId = "/java/com.surfapi/1.0";
        
        Map doc = db.read(libraryId, "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc");
        assertNotNull(doc);
        
        // Verify that all _ids have been set.
        // Note: also verifying that the lists are not empty
        assertTrue( assertAllIdsAreSet( libraryId, (List<Map>) doc.get("methods") ).size() > 0) ;
        assertTrue( assertAllIdsAreSet( libraryId, (List<Map>) doc.get("fields") ).size() > 0) ;
        assertTrue( assertAllIdsAreSet( libraryId, (List<Map>) doc.get("constructors") ).size() > 0) ;
        assertTrue( assertAllIdsAreSet( libraryId, (List<Map>) doc.get("innerClasses") ).size() > 0) ;
        
        Map containingPackage = (Map) doc.get("containingPackage");
        assertNotNull(containingPackage);
        assertEquals( JavadocMapUtils.buildId(libraryId, containingPackage), containingPackage.get("_id") );
        
        // superclass and interfaces are not in the same library as DemoJavadoc, so their _ids should NOT
        // have been set.
        
        assertNull( ((Map)doc.get("superclass")).get("_id") );
        Map callableInterface = ((List<Map>)doc.get("interfaces")).get(0);
        assertNull( callableInterface.get("_id") );
        
    }
    
    private Collection<Map> assertAllIdsAreNull( Collection<Map> stubs) {
        for (Map stub : Cawls.safeIterable(stubs)) {
            assertNull( stub.get("_id") );
        }
        return stubs;
    }
    
    private Collection<Map> assertAllIdsAreSet( String libraryId, Collection<Map> stubs) {
        for (Map stub : stubs) {
            assertEquals( JavadocMapUtils.buildId(libraryId, stub), stub.get("_id") );
        }
        return stubs;
    }
    
}
