package com.surfapi.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

import java.util.List;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.mongodb.WriteConcern;
import com.surfapi.coll.MapBuilder;
import com.surfapi.json.JSONTrace;
import com.surfapi.junit.CaptureSystemOutRule;
import com.surfapi.junit.DropMongoDBRule;
import com.surfapi.junit.MongoDBProcessRule;
import com.surfapi.log.Log;

/**
 * 
 */
public class BulkWriterTest {
    
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
    @Rule
    public DropMongoDBRule dropMongoDBRule = new DropMongoDBRule( mongoDBProcessRule, MongoDbName );
    
    
    /**
     * Capture and suppress stdout unless the test fails.
     */
    @Rule
    public CaptureSystemOutRule systemOutRule  = new CaptureSystemOutRule( );
    
    /**
     * 
     */
    @Test
    public void testBulkWrite() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        MongoDBImpl db = new MongoDBImpl(MongoDbName);
        
        BulkWriter bulkWriter = new BulkWriter(db, "test.collection").setFlushCount(3);
        
        bulkWriter.insert( new MapBuilder().append( "_id", "1") );
        bulkWriter.insert( new MapBuilder().append( "_id", "2") );
        
        // hasn't been flushed yet.
        assertNull( db.read("test.collection", "1") );
        
        bulkWriter.insert( new MapBuilder().append( "_id", "3") );
        
        assertNotNull( db.read("test.collection", "1") );
        assertNotNull( db.read("test.collection", "2") );
        assertNotNull( db.read("test.collection", "3") );
        
        bulkWriter.insert( new MapBuilder().append( "_id", "4") );
        bulkWriter.insert( new MapBuilder().append( "_id", "5") );
        
        // hasn't been flushed yet.
        assertNull( db.read("test.collection", "4") );
        
        bulkWriter.insert( new MapBuilder().append( "_id", "6") );
        
        assertNotNull( db.read("test.collection", "4") );
        assertNotNull( db.read("test.collection", "5") );
        assertNotNull( db.read("test.collection", "6") );

    }
    
    /**
     * 
     */
    @Test
    public void testEmptyFlushUnacknowledged() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        MongoDBImpl db = new MongoDBImpl(MongoDbName);
        
        assertNull( db.read("test.collection", "1") );
        
        BulkWriter bulkWriter = new BulkWriter(db, "test.collection").setFlushCount(3)
                                                                     .setWriteConcern( WriteConcern.UNACKNOWLEDGED );
        
        bulkWriter.insert( new MapBuilder().append( "_id", "1") );
        bulkWriter.insert( new MapBuilder().append( "_id", "2") );
        bulkWriter.flush();
        
        assertNotNull( db.read("test.collection", "1") );
        assertNotNull( db.read("test.collection", "2") );
        
        bulkWriter.flush();
        bulkWriter.flush();
    }
    
    /**
     * 
     */
    @Test
    public void testEmptyFlush() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        MongoDBImpl db = new MongoDBImpl(MongoDbName);
        
        assertNull( db.read("test.collection", "1") );
        
        BulkWriter bulkWriter = new BulkWriter(db, "test.collection").setFlushCount(3);
        
        bulkWriter.insert( new MapBuilder().append( "_id", "1") );
        bulkWriter.insert( new MapBuilder().append( "_id", "2") );
        bulkWriter.flush();
        
        assertNotNull( db.read("test.collection", "1") );
        assertNotNull( db.read("test.collection", "2") );
        
        bulkWriter.flush();
        bulkWriter.flush();
    }
    
    /**
     * 
     */
    @Test
    public void testUpsert() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        MongoDBImpl db = new MongoDBImpl(MongoDbName);
        
        assertNull( db.read("test.collection", "1") );
        
        BulkWriter bulkWriter = new BulkWriter(db, "test.collection").setFlushCount(3);
        
        bulkWriter.insert( new MapBuilder().append( "_id", "1") );
        bulkWriter.insert( new MapBuilder().append( "_id", "2") );
        bulkWriter.flush();
        
        assertNotNull( db.read("test.collection", "1") );
        assertNotNull( db.read("test.collection", "2") );
        
        bulkWriter.insert( new MapBuilder().append( "_id", "2").append("foo", "bar") );
        bulkWriter.insert( new MapBuilder().append( "_id", "3").append("foo", "bar") );
        bulkWriter.flush();
        
        Map doc = db.read("test.collection", "2");
        assertEquals("bar", doc.get("foo") );
        
        doc = db.read("test.collection", "3");
        assertEquals("bar", doc.get("foo") );
    }
  

    private void log(String msg, List<Map> docs) {
        for (Map doc : docs) {
            Log.trace(this, msg + JSONTrace.prettyPrint(doc) );
        }
    }
}
      
