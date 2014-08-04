package com.surfapi.db;

import java.util.List;
import java.util.Map;

import com.mongodb.BasicDBObject;
import com.mongodb.BulkWriteOperation;
import com.mongodb.WriteConcern;
import com.surfapi.app.JavadocMapUtils;
import com.surfapi.coll.MapBuilder;
import com.surfapi.log.Log;

/**
 * Manages bulk-writes to the db.
 */
public class BulkWriter {

    private MongoDBImpl db;
    
    private String collection;
    
    private BulkWriteOperation bulkWriteOperation;
    
    /**
     * The number of inserts made since the last flush.
     */
    private int operationCount  = 0;
    
    /**
     * The number of inserts at which a flush should occur.
     */
    private int flushCount = 1000;
    
    /**
     * 
     */
    private WriteConcern writeConcern = WriteConcern.ACKNOWLEDGED;
    
    /**
     * CTOR.
     */
    public BulkWriter(MongoDBImpl db, String collection) {
        this.db = db;
        this.collection = collection;
        
        this.bulkWriteOperation = db.getMongoDB().getCollection( getCollectionName() ).initializeUnorderedBulkOperation();
    }
    
    public MongoDBImpl getDb() {
        return db;
    }
    
    public String getCollectionName() {
        return collection;
    }
    
    public BulkWriteOperation getBulkWriteOperation() {
        if (bulkWriteOperation == null) {
            bulkWriteOperation = getDb().getMongoDB().getCollection( getCollectionName() ).initializeUnorderedBulkOperation();
        }
        return bulkWriteOperation;
    }
    
    public BulkWriter insert( Map doc ) {
        getDb().validateSave( getCollectionName(), doc );
        
        // getBulkWriteOperation().insert( new BasicDBObject( doc ) );
        // The following 'upsert' should work whether or not the _id already exists
        getBulkWriteOperation().find( new BasicDBObject( new MapBuilder().append("_id", doc.get("_id") ) ) )
                               .upsert()
                               .update( new BasicDBObject( new MapBuilder().append( "$set", JavadocMapUtils.removeId(doc) ) ) );
                               
        return increment();
    }
    
    public BulkWriter increment() {
        return (++operationCount >= flushCount) ? flush() : this;
    }
    
    public BulkWriter insert( List<Map> docs ) {
        for (Map doc : docs) {
            insert(doc);
        }
        return this;
    }
    
    public BulkWriter safeInsert( Map doc ) {
        try {
            return insert( doc );
        } catch (Exception e) {
            Log.error(this, "safeInsert: caught exception", e);
        }
        return this;
    }
    
    public BulkWriter safeInsert( List<Map> docs ) {
        for (Map doc : docs) {
            safeInsert(doc);
        }
        return this;
    }

    public BulkWriter flush() {
        if (operationCount > 0) {
            getBulkWriteOperation().execute( getWriteConcern() );
        }
        operationCount = 0;
        bulkWriteOperation = null;
        return this;
    }

    public BulkWriter setFlushCount(int flushCount) {
        this.flushCount = flushCount;
        return this;
    }
    
    public BulkWriter setWriteConcern(WriteConcern writeConcern) {
        this.writeConcern = writeConcern;
        return this;
    }
    
    public WriteConcern getWriteConcern() {
        return writeConcern;
    }
}
