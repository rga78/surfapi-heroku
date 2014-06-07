package com.surfapi.mongodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.json.simple.JSONObject;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MongoClient;

public class MongoDBProcessTest {

    
    @Test
    public void test() throws Exception {
        
        assumeTrue( Boolean.getBoolean("runMongo") );
        
        MongoDBProcess mongodbProcess = MongoDBProcess.start();
        
        MongoClient mongoClient = new MongoClient();
        DBCollection collection = mongoClient.getDB( "test1" ).getCollection("MongodbProcessTest.test.collection");

        JSONObject obj = new JSONObject();
        obj.put("_id", "1");
        obj.put("hello", "world");
        
        collection.save( new BasicDBObject( obj ) );
        
        DBCursor cursor =  collection.find();
        assertTrue( cursor.count() > 0 );
        
        assertEquals("world", collection.findOne("1").get("hello"));

        collection.drop();
        mongoClient.getDB( "test1" ).dropDatabase();
        
        mongodbProcess.stop();
    }
    
    @Test
    public void testStartAndStop() throws Exception {
        
        assumeTrue( Boolean.getBoolean("runMongo") );
        
        MongoDBProcess mongodbProcess = MongoDBProcess.start();
        mongodbProcess.stop();
        mongodbProcess = MongoDBProcess.start();
        mongodbProcess.stop();
        // TODO: verify it stopped?
    }
    
    /**
     * 
     */
    @Test
    public void testIsStarted() throws Exception {
        
        assumeTrue( Boolean.getBoolean("runMongo") );
        
        MongoDBProcess mongodbProcess = MongoDBProcess.start();
        assertTrue(MongoDBProcess.isStarted());
        mongodbProcess.stop();
    
    }
    
}
