package com.surfapi.db.post;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.DB;
import com.surfapi.db.DBLoader;
import com.surfapi.db.MongoDBImpl;
import com.surfapi.db.MongoDBService;
import com.surfapi.javadoc.JavadocMain;
import com.surfapi.junit.CaptureSystemOutRule;
import com.surfapi.junit.DropMongoDBRule;
import com.surfapi.junit.MongoDBProcessRule;
import com.surfapi.log.Log;

/**
 * 
 */
public class AutoCompleteIndexTest {

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
    public void testBasic() throws Exception {
        
        // TODO: @Rule candidate? (in combination with the ClassRule)
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        Map lib_v10 = JavadocMapUtils.mapLibraryId( "/java/com.surfapi/1.0" );
        
        File testJsonFile = new File("src/test/resources/com.surfapi_1.0.json");
        assertTrue(testJsonFile.exists());
        
        DB db = new MongoDBImpl("test1");
        new DBLoader().inject(db).loadFile( testJsonFile )
                                .loadFile( new File("src/test/resources/com.surfapi_0.9.json") );
        
        
        new AutoCompleteIndex().inject(db).buildIndexForLang( "java" );
        
        //String indexName = AutoCompleteIndex.buildAutoCompleteIndexNameForLang( "java" );
        
        // List<Map> objs = db.find( indexName, new MapBuilder().append( "_searchName", new MapBuilder().append( "$regex", "^Demo.*") ) );
        List<Map> objs = new AutoCompleteIndex().inject(db).query( "java", "Demo", 25 );
        assertFalse( objs.isEmpty() );
        for (Map obj : objs) {
            Log.trace(this, "testBasic: " + obj.get("id"));
            assertTrue( ((String)obj.get("name")).startsWith("Demo") );
            assertEquals( lib_v10, obj.get(JavadocMapUtils.LibraryFieldName) );
        }
        
        // Verify case-insensitive search
        List<Map> objs2 = new AutoCompleteIndex().inject(db).query( "java", "demo", 25 );
        assertEquals(objs, objs2);
        
        // Verify only the latest library version was used
        List<Map> libraries = (List<Map>) Cawls.pluck( objs, JavadocMapUtils.LibraryFieldName);
        assertNotNull( Cawls.findFirst( libraries, new MapBuilder().append( "_id", "/java/com.surfapi/1.0") ) );
        assertNull( Cawls.findFirst( libraries, new MapBuilder().append( "_id", "/java/com.surfapi/0.9") ) );
       
    }
 
    /**
     * 
     */
    @Test
    public void testPackageEntries() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        DB db = new MongoDBImpl("test1");
        new DBLoader().inject(db).loadFile( new File("src/test/resources/com.surfapi_1.0.json") )
                                 .loadFile( new File("src/test/resources/com.surfapi_0.9.json") );
        
        
        new AutoCompleteIndex().inject(db).buildIndexForLang( "java" );
        
        // String indexName = AutoCompleteIndex.buildAutoCompleteIndexNameForLang( "java" );
        
        // Search by segmentName = 'test'
        // List<Map> objs = db.find( indexName, new MapBuilder().append( "_searchName", new MapBuilder().append( "$regex", "^test.*") ) );
        List<Map> objs = new AutoCompleteIndex().inject(db).query( "java", "test", 25 );
        assertFalse( objs.isEmpty() );
        for (Map obj : objs) {
            Log.trace(this, "testPackageEntries: for search 'test': " + obj.get("id"));
        }
        assertEquals( 1, objs.size() );
        assertNotNull( Cawls.findFirst( objs, new MapBuilder().append( "id", "/java/com.surfapi/1.0/com.surfapi.test") ) );

        // Searching by the root segment name 'com'.  Verify that the root nameSegment wasn't inserted in its own entry
        // (since it's already covered by the full name entry).
        // objs = db.find( indexName, new MapBuilder().append( "_searchName", new MapBuilder().append( "$regex", "^com.*") ) );
        objs = new AutoCompleteIndex().inject(db).query( "java", "com", 25 );
        assertFalse( objs.isEmpty() );
        for (Map obj : objs) {
            Log.trace(this, "testPackageEntries: for search 'com': " + obj.get("id") + ", _searchName: " + obj.get("_searchName"));
        }
        assertEquals( 2, objs.size() );
        assertNotNull( Cawls.findFirst( objs, new MapBuilder().append( "id", "/java/com.surfapi/1.0/com.surfapi.test") ) );
        assertNotNull( Cawls.findFirst( objs, new MapBuilder().append( "id", "/java/com.surfapi/1.0/com.surfapi.proc") ) );

        // Searching by the segment name 'surfapi'.  
        // objs = db.find( indexName, new MapBuilder().append( "_searchName", new MapBuilder().append( "$regex", "^surfapi.*") ) );
        objs = new AutoCompleteIndex().inject(db).query( "java", "surfapi", 25 );
        assertFalse( objs.isEmpty() );
        for (Map obj : objs) {
            Log.trace(this, "testPackageEntries: for search 'surfapi': " + obj.get("id"));
        }
        assertEquals( 2, objs.size() );
        assertNotNull( Cawls.findFirst( objs, new MapBuilder().append( "id", "/java/com.surfapi/1.0/com.surfapi.test") ) );
        assertNotNull( Cawls.findFirst( objs, new MapBuilder().append( "id", "/java/com.surfapi/1.0/com.surfapi.proc") ) );
        
        // Searching by the segment name 'surfapi.test'.  
        objs = new AutoCompleteIndex().inject(db).query( "java", "surfapi.tes", 25 );
        assertFalse( objs.isEmpty() );
        for (Map obj : objs) {
            Log.trace(this, "testPackageEntries: for search 'surfapi.tes': " + obj.get("id"));
        }
        assertEquals( 1, objs.size() );
        assertNotNull( Cawls.findFirst( objs, new MapBuilder().append( "id", "/java/com.surfapi/1.0/com.surfapi.test") ) );

    }

    /**
     * 
     */
    @Test
    public void testAddLibraryToIndexNewestLibraryOnly() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        Map lib_v10 = JavadocMapUtils.mapLibraryId( "/java/com.surfapi/1.0" );
        Map lib_v09 = JavadocMapUtils.mapLibraryId( "/java/com.surfapi/0.9" );
        
        // Add the older version to the db first.
        DB db = new MongoDBImpl("test1");
        new DBLoader().inject(db).loadFile( new File("src/test/resources/com.surfapi_0.9.json") );
        
        // The older version is the only one of its kind, so yeah, add it.
        assertTrue( new AutoCompleteIndex().inject(db).shouldAddLibraryToIndex( lib_v09 ) );
        
        // Add it.
        new AutoCompleteIndex().inject(db).addLibraryToIndex( (String) lib_v09.get("_id") ) ;
        
        // Query, make sure entries were added.
        List<Map> docs = new AutoCompleteIndex().inject(db).query( "java", "Demo", 25 );
        
        assertFalse( docs.isEmpty() );
        for (Map doc : docs) {
            Log.trace(this, "testAddLibraryToIndex: " + doc.get("id"));
            assertTrue( ((String)doc.get("name")).startsWith("Demo") );
            assertEquals( lib_v09, doc.get(JavadocMapUtils.LibraryFieldName) );
        }
        
        // Add the newer version to the db.
        new DBLoader().inject(db).loadFile( new File("src/test/resources/com.surfapi_1.0.json") );
        
        // The newer versions should be add-able to the index, the older version should not.
        assertTrue( new AutoCompleteIndex().inject(db).shouldAddLibraryToIndex( lib_v10 ) );
        assertFalse( new AutoCompleteIndex().inject(db).shouldAddLibraryToIndex( lib_v09 ) );
        
        // Add the new library.
        new AutoCompleteIndex().inject(db).addLibraryToIndex( (String) lib_v10.get("_id") ) ;
        
        // Query, make sure the old entries were removed and new ones were added.
        docs = new AutoCompleteIndex().inject(db).query( "java", "Demo", 25 );
        
        assertFalse( docs.isEmpty() );
        for (Map doc : docs) {
            Log.trace(this, "testAddLibraryToIndex: " + doc.get("id"));
            assertTrue( ((String)doc.get("name")).startsWith("Demo") );
            assertEquals( lib_v10, doc.get(JavadocMapUtils.LibraryFieldName) );
        }
    }

    /**
     * 
     */
    @Test
    public void testAddLibraryToIndexLibrarySpecificIndex() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        MongoDBService.setDbName( "test1" );
        DB db = MongoDBService.getDb();
        
        String testLibraryId = "/java/com.surfapi.test/1.0";
        String libraryId = "/java/com.surfapi/1.0";
        
        // Insert both libraries.
        JavadocMain.main( new String[] { "--all", testLibraryId, "src/test/java/com/surfapi/test" } );
        JavadocMain.main( new String[] { "--all", libraryId, "src/main/java/com/surfapi/proc" } );
        
        // Add both to the auto-complete index
        new AutoCompleteIndex().inject(db).addLibraryToIndex( testLibraryId ) ;
        new AutoCompleteIndex().inject(db).addLibraryToIndex( libraryId ) ;
       
        // Verify that both are in the lang index
        List<Map> docs = new AutoCompleteIndex().inject(db).query( "java", "Demo", 25 );
        assertFalse( docs.isEmpty() );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "DemoJavadoc" ) ) );
        
        docs = new AutoCompleteIndex().inject(db).query( "java", "Proc", 25 );
        assertFalse( docs.isEmpty() );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "ProcessHelper" ) ) );
        
        // Verify the testLibraryId index contains only classes from that library
        docs = new AutoCompleteIndex().inject(db).query( testLibraryId, "Demo", 25 );
        assertFalse( docs.isEmpty() );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "DemoJavadoc" ) ) );
        
        docs = new AutoCompleteIndex().inject(db).query( testLibraryId, "Proc", 25 );
        assertTrue( docs.isEmpty() );
        
        // Verify the libraryId index contains only classes from that library
        docs = new AutoCompleteIndex().inject(db).query( libraryId, "Proc", 25 );
        assertFalse( docs.isEmpty() );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "ProcessHelper" ) ) );

        docs = new AutoCompleteIndex().inject(db).query( libraryId, "Demo", 25 );
        assertTrue( docs.isEmpty() );
    }
 

    
}

