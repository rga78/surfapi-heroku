package com.surfapi.db.post;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.DB;
import com.surfapi.db.MongoDBImpl;
import com.surfapi.javadoc.SimpleJavadocProcess;
import com.surfapi.junit.CaptureSystemOutRule;
import com.surfapi.junit.MongoDBProcessRule;
import com.surfapi.log.Log;

/**
 * TODO: add some smarter and more comprehensive tests for this.
 */
public class AutoCompleteIndexTest {

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
    public static MongoDBProcessRule mongoDBProcessRule = new MongoDBProcessRule(MongoDbName);
    
    /**
     * Capture and suppress stdout unless the test fails.
     */
    @Rule
    public CaptureSystemOutRule systemOutRule  = new CaptureSystemOutRule( );
    
    /**
     * 
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        assumeTrue(mongoDBProcessRule.isStarted());
        
        // Setup the db.
        String libraryId = "/java/com.surfapi/1.0";
        
        File baseDir = new File("src/test/java");
        
        new SimpleJavadocProcess()
               .setMongoUri( MongoUri )
               .setLibraryId( libraryId )
               .setSourcePath( baseDir )
               .setPackages( Arrays.asList( "com.surfapi.test" ) )
               .run();
        
        // add another version of the library with more classes
        new SimpleJavadocProcess()
                .setMongoUri( MongoUri )
                .setLibraryId( "/java/com.surfapi/0.9" )
                .setSourcePath( baseDir )
                .setPackages( Arrays.asList( "com.surfapi.test", "com.surfapi.coll" ) )
                .run();
        
        // Add another library (w/ similarly named classes)
        String testLibraryId = "/java/com.surfapi.coll/1.0";
        new SimpleJavadocProcess()
                .setMongoUri( MongoUri )
                .setLibraryId( testLibraryId )
                .setSourcePath( new File("src/test/java") )
                .setPackages( Arrays.asList( "com.surfapi.coll" ) )
                .run();
        
        // Build the all known subclasses index
        new AutoCompleteIndex().inject(new MongoDBImpl(MongoDbName)).buildIndexForLang("java");
    }
    
    /**
     * 
     */
    @Before
    public void before() {
        assumeTrue(mongoDBProcessRule.isStarted());
    }
    
    /**
     * 
     */
    @Test
    public void testBasic() throws Exception {
        
        DB db = new MongoDBImpl(MongoDbName);
        
        List<Map> objs = new AutoCompleteIndex().inject(db).query( "java", "Demo", 25 );
        assertFalse( objs.isEmpty() );
        
        for (Map obj : objs) {
            Log.trace(this, "testBasic: " + obj.get("id"));
            assertTrue( ((String)obj.get("name")).startsWith("Demo") );
        }
        
        // Verify case-insensitive search
        List<Map> objs2 = new AutoCompleteIndex().inject(db).query( "java", "demo", 25 );
        assertEquals(objs, objs2);
        
        // Verify only one version was used
        objs = new AutoCompleteIndex().inject(db).query( "java", "DemoJavadocSubClass2", 25 );
        for (Map obj : objs) {
            Log.trace(this, "testBasic: DemoJavadocSubClass2: " + obj.get("id"));
        }
        assertEquals( 1, objs.size() );
        
        // -rx- List<Map> libraries = (List<Map>) Cawls.pluck( objs, JavadocMapUtils.LibraryFieldName);
        // -rx- assertNotNull( Cawls.findFirst( libraries, new MapBuilder().append( "_id", "/java/com.surfapi/1.0") ) );
        // -rx- assertNull( Cawls.findFirst( libraries, new MapBuilder().append( "_id", "/java/com.surfapi/0.9") ) );
       
        // Verify extra classes in older version
        objs = new AutoCompleteIndex().inject(db).query( "java", "Cawl", 25 );
        
        assertFalse( objs.isEmpty() );
        for (Map obj : objs) {
            Log.trace(this, "testBasic: " + obj.get("id"));
            assertTrue( ((String)obj.get("name")).startsWith("Cawl") );
        }
    }
    
    /**
     * 
     */
    @Test
    public void testMatchWholeName() throws Exception {
        
        DB db = new MongoDBImpl(MongoDbName);
        
        List<Map> docs = new AutoCompleteIndex().inject(db).query( "java", "demointerface", 25 );
        
        Log.trace( this, "testMatchWholeName: match start: ", Cawls.pluck(docs, "id"));

        assertEquals(3, docs.size());
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "DemoInterface" ) ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "DemoInterface2" ) ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "DemoInterfaceSubIntf" ) ) );
        
        // run the query again, this time with a ' ' at the end. 
        // Should match only DemoInterface
        docs = new AutoCompleteIndex().inject(db).query( "java", "demointerface ", 25 );
        Log.trace( this, "testMatchWholeName: match whole name: ", Cawls.pluck(docs, "id"));
        
        assertEquals(1, docs.size());
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "DemoInterface" ) ) );
        
    }
 
    /**
     * 
     */
    @Test
    public void testPackageEntries() throws Exception {
 
        DB db = new MongoDBImpl(MongoDbName);
        
        // Search by segmentName = 'test'
        List<Map> objs = new AutoCompleteIndex().inject(db).query( "java", "test", 25 );
        assertFalse( objs.isEmpty() );
        for (Map obj : objs) {
            Log.trace(this, "testPackageEntries: for search 'test': " + obj.get("id"));
        }
        assertEquals( 1, objs.size() );

        // Searching by the root segment name 'com'.  Verify that the root nameSegment wasn't inserted in its own entry
        // (since it's already covered by the full name entry).
        objs = new AutoCompleteIndex().inject(db).query( "java", "com", 25 );
        assertFalse( objs.isEmpty() );
        for (Map obj : objs) {
            Log.trace(this, "testPackageEntries: for search 'com': " + obj.get("id") + ", _searchName: " + obj.get("_searchName"));
        }
        assertEquals( 3, objs.size() );

        // Searching by the segment name 'surfapi'.  
        objs = new AutoCompleteIndex().inject(db).query( "java", "surfapi", 25 );
        assertFalse( objs.isEmpty() );
        for (Map obj : objs) {
            Log.trace(this, "testPackageEntries: for search 'surfapi': " + obj.get("id"));
        }
        assertEquals( 3, objs.size() );
        
        // Searching by the segment name 'surfapi.col'.  
        objs = new AutoCompleteIndex().inject(db).query( "java", "surfapi.col", 25 );
        assertFalse( objs.isEmpty() );
        for (Map obj : objs) {
            Log.trace(this, "testPackageEntries: for search 'surfapi.col': " + obj.get("id"));
        }
        assertEquals( 2, objs.size() );
    }
//
//    /**
//     * 
//     */
//    // -rx- @Test
//    public void testAddLibraryToIndexNewestLibraryOnly() throws Exception {
//        
//        assumeTrue( mongoDBProcessRule.isStarted() );
//        
//        Map lib_v10 = JavadocMapUtils.mapLibraryId( "/java/com.surfapi/1.0" );
//        Map lib_v09 = JavadocMapUtils.mapLibraryId( "/java/com.surfapi/0.9" );
//        
//        // Add the older version to the db first.
//        DB db = new MongoDBImpl(MongoDbName);
//        new DBLoader().inject(db).loadFile( new File("src/test/resources/com.surfapi_0.9.json") );
//        
//        // -rx- // The older version is the only one of its kind, so yeah, add it.
//        // -rx- assertTrue( new AutoCompleteIndex().inject(db).shouldAddLibraryToIndex( lib_v09 ) );
//        
//        // Add it.
//        new AutoCompleteIndex().inject(db).addLibraryToIndex( (String) lib_v09.get("_id") ) ;
//        
//        // Query, make sure entries were added.
//        List<Map> docs = new AutoCompleteIndex().inject(db).query( "java", "Demo", 25 );
//        
//        assertFalse( docs.isEmpty() );
//        for (Map doc : docs) {
//            Log.trace(this, "testAddLibraryToIndex: " + doc.get("id"));
//            assertTrue( ((String)doc.get("name")).startsWith("Demo") );
//            assertEquals( lib_v09, doc.get(JavadocMapUtils.LibraryFieldName) );
//        }
//        
//        // Add the newer version to the db.
//        new DBLoader().inject(db).loadFile( new File("src/test/resources/com.surfapi_1.0.json") );
//        
//        // The newer versions should be add-able to the index, the older version should not.
//        // -rx- assertTrue( new AutoCompleteIndex().inject(db).shouldAddLibraryToIndex( lib_v10 ) );
//        // -rx- assertFalse( new AutoCompleteIndex().inject(db).shouldAddLibraryToIndex( lib_v09 ) );
//        
//        // Add the new library.
//        new AutoCompleteIndex().inject(db).addLibraryToIndex( (String) lib_v10.get("_id") ) ;
//        
//        // Query, make sure the old entries were removed and new ones were added.
//        // TODO: remove this test i think..
//        docs = new AutoCompleteIndex().inject(db).query( "java", "Demo", 25 );
//        
//        assertFalse( docs.isEmpty() );
//        for (Map doc : docs) {
//            Log.trace(this, "testAddLibraryToIndex: " + doc.get("id"));
//            assertTrue( ((String)doc.get("name")).startsWith("Demo") );
//            assertEquals( lib_v10, doc.get(JavadocMapUtils.LibraryFieldName) );
//        }
//    }

    /**
     * 
     */
    @Test
    public void testAddLibraryToIndexLibrarySpecificIndex() throws Exception {
        
        String testLibraryId = "/java/com.surfapi.coll/1.0";
        String libraryId = "/java/com.surfapi/1.0";
        
        
        DB db =new MongoDBImpl(MongoDbName);
       
        // Verify that both are in the lang index
        List<Map> docs = new AutoCompleteIndex().inject(db).query( "java", "Demo", 25 );
        assertFalse( docs.isEmpty() );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "DemoJavadoc" ) ) );
        
        docs = new AutoCompleteIndex().inject(db).query( "java", "Cawl", 25 );
        assertFalse( docs.isEmpty() );
        Log.trace(this, "Search for Cawl: ", docs);
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "CawlsTest" ) ) );
        
        // Verify the testLibraryId index contains only classes from that library
        docs = new AutoCompleteIndex().inject(db).query( testLibraryId, "Cawls", 25 );
        assertFalse( docs.isEmpty() );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "CawlsTest" ) ) );
        
        docs = new AutoCompleteIndex().inject(db).query( testLibraryId, "Demo", 25 );
        assertTrue( docs.isEmpty() );
        
        // Verify the libraryId index contains only classes from that library
        docs = new AutoCompleteIndex().inject(db).query( libraryId, "Demo", 25 );
        assertFalse( docs.isEmpty() );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "DemoJavadoc" ) ) );

        docs = new AutoCompleteIndex().inject(db).query( libraryId, "Cawls", 25 );
        assertTrue( docs.isEmpty() );
    }
    
    /**
     * 
     */
    @Test
    public void testAddAndRemoveLibrary() throws Exception {
        
        assumeTrue(mongoDBProcessRule.isStarted());
        
        // add the library first
        String libraryId = "/java/com.surfapi.junit/1.0";
        
        File baseDir = new File("src/test/java");
        
        new SimpleJavadocProcess()
               .setMongoUri( MongoUri )
               .setLibraryId( libraryId )
               .setSourcePath( baseDir )
               .setPackages( Arrays.asList( "com.surfapi.junit" ) )
               .run();
        
        MongoDBImpl db = new MongoDBImpl(MongoDbName);
        
        // Add library to the index
        new AutoCompleteIndex().inject(db).addLibraryToIndex(libraryId);
        
        // Verify it's there in the lang index
        List<Map> docs = new AutoCompleteIndex().inject(db).query( "java", "CaptureSystemOut", 25 );
        assertFalse( docs.isEmpty() );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "CaptureSystemOutRule" ) ) );
        
        // Verify the library-specific index 
        docs = new AutoCompleteIndex().inject(db).query( libraryId, "CaptureSystemOut", 25 );
        assertFalse( docs.isEmpty() );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "name", "CaptureSystemOutRule" ) ) );
        
        // Now remove
        new AutoCompleteIndex().inject(db).removeLibrary(libraryId);
        
        // Verify it's gone
        docs = new AutoCompleteIndex().inject(db).query( "java", "CaptureSystemOut", 25 );
        assertTrue( docs.isEmpty() );
        
        docs = new AutoCompleteIndex().inject(db).query( libraryId, "CaptureSystemOut", 25 );
        assertTrue( docs.isEmpty() );
        
        assertFalse( db.getMongoDB().collectionExists( AutoCompleteIndex.getCollectionName(libraryId)) );
    }

    
}

