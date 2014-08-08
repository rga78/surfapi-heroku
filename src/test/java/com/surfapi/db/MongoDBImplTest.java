package com.surfapi.db;

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

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;
import com.surfapi.app.JavadocMapUtils;
import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.json.JSONTrace;
import com.surfapi.junit.CaptureSystemOutRule;
import com.surfapi.junit.DropMongoDBRule;
import com.surfapi.junit.MongoDBProcessRule;
import com.surfapi.log.Log;

/**
 * 
 */
public class MongoDBImplTest {
    
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
    public void testDBLoader() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        File testJsonFile = new File("src/test/resources/DBTest.test_1.0.3.json");
        assertTrue(testJsonFile.exists());
        
        String libraryId = "/java/DBTest.test/1.0.3";
        assertEquals(libraryId, DBLoader.parseLibraryId( testJsonFile.getName() )) ;
        
        // Reset the db.
        new MongoDBImpl("test1").drop();
        
        DB db = new MongoDBImpl("test1") ;
        new DBLoader().inject(db ).loadUnchecked( new File("src/test/resources") );
        
        String key = DBLoader.parseLibraryId( testJsonFile.getName() ) + "/com.surfapi.test.DemoJavadoc" ;
        
        Map obj = db.read( libraryId, key );
        
        assertNotNull(obj);
        assertEquals( "class", JavadocMapUtils.getMetaType(obj) );
        assertEquals( "com.surfapi.test.DemoJavadoc", JavadocMapUtils.getQualifiedName(obj));
        
        obj = db.read( libraryId, key + ".parse(java.net.URL,java.util.List)");
        
        assertNotNull(obj);
        assertEquals( "method", JavadocMapUtils.getMetaType(obj) );
        assertEquals( "com.surfapi.test.DemoJavadoc.parse", JavadocMapUtils.getQualifiedName(obj));
        
        // Verify that the library overview/package summary was built and added:
        obj = db.read(DB.LibraryCollectionName, libraryId);
        assertNotNull(obj);
        List<Map> pkgs = (List<Map>) obj.get("packages");
        assertNotNull( pkgs );
        assertEquals( "com.surfapi.test", pkgs.get(0).get("name") );
        assertEquals( libraryId + "/com.surfapi.test", pkgs.get(0).get("_id") );
        
        // Verify that the library was added to the libraries collection
        List<Map> javaLibs = db.getLibraryList("java");
        Map libObj = Cawls.findFirst(javaLibs, new MapBuilder<String, String>().append("_id", libraryId)) ;
        assertNotNull(libObj);
        
        assertEquals(libraryId, libObj.get("_id"));
        assertEquals("java", libObj.get("lang"));
        assertEquals("DBTest.test", libObj.get("name"));
        assertEquals("1.0.3", libObj.get("version"));
        assertEquals("library", libObj.get("metaType"));
        assertNull(libObj.get("packages")); // The 'packages' field should have been excluded.
    }
    
    /**
     * 
     */
    @Test
    public void testLotsOfData() throws Exception {
        
        // -rx- assumeTrue( mongodbProcess != null );
        assumeTrue( mongoDBProcessRule.isStarted() );
        assumeTrue( Boolean.getBoolean("runFat") );
        
        File testJsonFile = new File("data/java-sdk_1.6.json");
        assertTrue(testJsonFile.exists());
        
        String libraryId = "/java/java-sdk/1.6";
        assertEquals(libraryId, DBLoader.parseLibraryId( testJsonFile.getName() )) ;
        
        DB db = new MongoDBImpl("test1") ;
        
        new DBLoader().inject( db ).loadUnchecked( testJsonFile );
        
        String key = DBLoader.parseLibraryId( testJsonFile.getName() ) + "/java.lang.String";
        
        Map obj = db.read( libraryId, key );
        
        assertNotNull(obj);
        assertEquals( "class", JavadocMapUtils.getMetaType(obj) );
        assertEquals( String.class.getCanonicalName(), JavadocMapUtils.getQualifiedName(obj));
        Log.info(this, "testLotsOfData: found String: " + JSONTrace.prettyPrint( obj ));
        
        obj = db.read( libraryId, key + ".replace(char, char)");
        
        assertNotNull(obj);
        assertEquals( "method", JavadocMapUtils.getMetaType(obj) );
        assertEquals( String.class.getCanonicalName() + ".replace", JavadocMapUtils.getQualifiedName(obj));
        Log.info(this, "testLotsOfData: found String.replace: " + JSONTrace.prettyPrint( obj ));
    }
    
    @Test
    public void testGetCollectionNames() throws Exception {
        // -rx- assumeTrue( mongodbProcess != null );
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        for (String collName : new MongoDBImpl( "test1" ).getMongoDB().getCollectionNames()) {
            Log.info(this, "testGetCollectionNames: " + collName);
        }
    }

    /**
     * 
     */        
    @Test
    public void testGetLibraryList() throws Exception {
        // -rx- assumeTrue( mongodbProcess != null );
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        MongoDBImpl db = new MongoDBImpl( "test1" );

        // Add 
        DBCollection libs = db.getMongoDB().getCollection( DB.LibraryCollectionName );
        libs.save( new BasicDBObject().append("lang", "java")
                                      .append("_id", "/java/java-sdk/1.6")
                                      .append("name", "java-sdk")
                                      .append("version", "1.6") );
        libs.save( new BasicDBObject().append("lang", "scala")
                                      .append("_id", "/scala/scala-collection/1.6")
                                      .append("name", "scala-collection")
                                      .append("version", "1.6") );
        libs.save( new BasicDBObject().append("lang", "java")
                                      .append("_id", "/java/org.junit/4.11")
                                      .append("name", "org.junit")
                                      .append("version", "4.11") );
        
        List<Map> javaLibs = db.getLibraryList("java");
        
        assertNotNull( Cawls.findFirst(javaLibs, new MapBuilder<String, String>().append("_id", "/java/java-sdk/1.6")) );
        assertNotNull( Cawls.findFirst(javaLibs, new MapBuilder<String, String>().append("_id", "/java/org.junit/4.11")) );
        assertNull( Cawls.findFirst(javaLibs, new MapBuilder<String, String>().append("_id", "/scala/scala-collection/1.6")) );
        
    }
    
    /**
     * 
     */        
    @Test
    public void testGetLibraryIds() throws Exception {
        // -rx- assumeTrue( mongodbProcess != null );
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        MongoDBImpl db = new MongoDBImpl( "test1" );

        // Add 
        DBCollection libs = db.getMongoDB().getCollection( DB.LibraryCollectionName );
        libs.save( new BasicDBObject().append("lang", "java")
                                      .append("_id", "/java/java-sdk/1.6")
                                      .append("name", "java-sdk")
                                      .append("version", "1.6") );
        libs.save( new BasicDBObject().append("lang", "scala")
                                      .append("_id", "/scala/scala-collection/1.6")
                                      .append("name", "scala-collection")
                                      .append("version", "1.6") );
        libs.save( new BasicDBObject().append("lang", "java")
                                      .append("_id", "/java/org.junit/4.11")
                                      .append("name", "org.junit")
                                      .append("version", "4.11") );
        
        List<String> javaLibs = db.getLibraryIds("java");
        
        assertTrue( javaLibs.contains( "/java/java-sdk/1.6" ) );
        assertFalse( javaLibs.contains( "/scala/scala-collection/1.6" ) );
        assertTrue( javaLibs.contains( "/java/org.junit/4.11" ) );
    }

    /**
     * 
     */
    @Test(expected=RuntimeException.class)
    public void testValidateSaveEmptyCollection() {
        assumeTrue( mongoDBProcessRule.isStarted() );
        new MongoDBImpl("test1").validateSave("", new MapBuilder().append("_id", "non-null-id"));
    }

    /**
     * 
     */
    @Test(expected=RuntimeException.class)
    public void testValidateSaveNullObject() {
        assumeTrue( mongoDBProcessRule.isStarted() );
        new MongoDBImpl("test1").validateSave("non-null-collection", null);
    }
    
    /**
     * 
     */
    @Test(expected=RuntimeException.class)
    public void testValidateSaveMissingId() {
        assumeTrue( mongoDBProcessRule.isStarted() );
        new MongoDBImpl("test1").validateSave("non-null-collection", new MapBuilder().append("somekey", "somevalue"));
    }

    /**
     * 
     */
    @Test
    public void testValidateSaveWithAutoCompleteIndexId() {
        assumeTrue( mongoDBProcessRule.isStarted() );
        new MongoDBImpl("test1").validateSave("non-null-collection", new MapBuilder().append("id", "somevalue"));
    }

    /**
     *
     */
    @Test
    public void testRemove() {
        assumeTrue( mongoDBProcessRule.isStarted() );

        DB db = new MongoDBImpl("test1");

        Map doc1 = new MapBuilder().append( "_id", "1")
                                   .append( "_library", new MapBuilder().append( "name", "lib1" ) );

        Map doc2 = new MapBuilder().append( "_id", "2")
                                   .append( "_library", new MapBuilder().append( "name", "lib2" ) );

        Map doc3 = new MapBuilder().append( "_id", "1")
                                   .append( "_library", new MapBuilder().append( "name", "lib1" ) );
        assertEquals( doc1, doc3 );

        db.save( "test.collection", doc1 );
        db.save( "test.collection", doc2 );

        List<Map> docs = db.find( "test.collection", new MapBuilder() );
        assertEquals( 2, docs.size() );
        log( "testRemove: ", docs );

        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append("_id", "1") ) );

        Map findDoc1 = Cawls.findFirst( docs, new MapBuilder().append("_id", "1") );
        assertEquals( doc1, Cawls.findFirst( docs, new MapBuilder().append("_id", "1") ) );

        Log.trace("testRemove: doc1: " + doc1 + ", findDoc1: " + findDoc1);
        Log.trace("testRemove: doc1._library: " + doc1.get("_library").getClass().getName() + ", findDoc1._library: " + findDoc1.get("_library").getClass().getName());
        
        assertEquals( doc1.get("_library"), findDoc1.get("_library") );
        Map doc1Lib = (Map) doc1.get("_library");
        Map findDoc1Lib = (Map) findDoc1.get("_library");

        assertNotNull( Cawls.findFirst( docs, doc1) );
        assertNotNull( Cawls.findFirst( docs, doc2) );
        
        WriteResult writeResult = (WriteResult) db.remove( "test.collection", new MapBuilder().append("_library.name", "lib1") );

        assertEquals(1, writeResult.getN() );
        
        docs = db.find( "test.collection", new MapBuilder() );
        assertEquals( 1, docs.size() );
        assertNull( Cawls.findFirst( docs, doc1) );
        assertNotNull( Cawls.findFirst( docs, doc2) );
    }

    /**
    *
    */
   @Test
   public void testBulkWrite() throws Exception {
       assumeTrue( mongoDBProcessRule.isStarted() );

       MongoDBImpl db = new MongoDBImpl("test1");
       
       BulkWriteOperation bwo = db.getMongoDB().getCollection("test.collection").initializeUnorderedBulkOperation();

       bwo.insert( new BasicDBObject(new MapBuilder().append( "_id", "1")) );
       bwo.insert( new BasicDBObject(new MapBuilder().append( "_id", "2")) );
       bwo.insert( new BasicDBObject(new MapBuilder().append( "_id", "3")) );
       bwo.insert( new BasicDBObject(new MapBuilder().append( "_id", "4")) );
       bwo.insert( new BasicDBObject(new MapBuilder().append( "_id", "5")) );
       bwo.insert( new BasicDBObject(new MapBuilder().append( "_id", "6")) );
       
       bwo.execute( WriteConcern.UNACKNOWLEDGED );

       Thread.sleep(500); // sleep half a second for all unacknowledged updates to complete
       
       assertNotNull( db.read( "test.collection", "1" ) );
       assertNotNull( db.read( "test.collection", "2" ) );
       assertNotNull( db.read( "test.collection", "3" ) );
       assertNotNull( db.read( "test.collection", "4" ) );
       assertNotNull( db.read( "test.collection", "5" ) );
       assertNotNull( db.read( "test.collection", "6" ) );

   }
   
   

   /**
    *
    */
   @Test
   public void testLimitFields() {
       assumeTrue( mongoDBProcessRule.isStarted() );

       DB db = new MongoDBImpl("test1");

       Map doc1 = new MapBuilder().append( "_id", "1")
                                  .append( "name", "myname")
                                  .append( "type", "mytype")
                                  .append( "methods", "my methods");
       
       db.save( "test.collection", doc1 );
       
       Map doc2 = db.read("test.collection", "1", new MapBuilder().append("name", 1));
       assertFalse( doc2.containsKey("methods") );
       assertFalse( doc2.containsKey("type") );
       assertEquals( "myname", doc2.get("name") );
       
       doc2 = db.read("test.collection", "1", new MapBuilder().append("methods", 0));
       assertFalse( doc2.containsKey("methods") );
       assertEquals( "myname", doc2.get("name") );
       assertEquals( "mytype", doc2.get("type") );
       
       doc2 = db.read("test.collection", "1", new MapBuilder().append("methods", 0)
                                                                  .append("name", 0));
       assertFalse( doc2.containsKey("methods") );
       assertFalse( doc2.containsKey("name") );
       assertEquals( "mytype", doc2.get("type") );
       
       // Empty fields filter should return all fields.
       doc2 = db.read("test.collection", "1", new MapBuilder());
       assertEquals( "myname", doc2.get("name") );
       assertEquals( "mytype", doc2.get("type") );
       assertEquals( "my methods", doc2.get("methods") );

   }
   

    private void log(String msg, List<Map> docs) {
        for (Map doc : docs) {
            Log.trace(this, msg + JSONTrace.prettyPrint(doc) );
        }
    }
}
      
