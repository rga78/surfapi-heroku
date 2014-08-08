package com.surfapi.db;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.log.Log;

/**
 * 
 */
public class MongoDBImpl implements DB {
    
    private com.mongodb.DB mongoDB;
    
    private String dbUri;
    
    
    /**
     * CTOR.
     */
    public MongoDBImpl(String dbName) {
        try {
            mongoDB = connect(dbName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * @return the mongodb uri
     */
    protected String getMongoUri(String dbName) {
        return Cawls.firstNotEmpty(System.getenv("MONGOLAB_URI"), 
                                   System.getProperty("MONGOLAB_URI"),
                                   "mongodb://localhost/" + dbName);
    }
    
    /**
     * Connect either via the MONGOLAB_URI envvar or, if that's not defined,
     * to the localhost.
     */
    protected com.mongodb.DB connect(String dbName) throws UnknownHostException {
        
        dbUri = getMongoUri(dbName);
        Log.info(this, "connect: URI: " + dbUri);
            
        MongoClientURI mongoUri  = new MongoClientURI(dbUri); 
        return new MongoClient(mongoUri).getDB(mongoUri.getDatabase());
    }
    
    /**
     * @return the mongoDB client
     */
    @Override
    public com.mongodb.DB getMongoDB() {
        return mongoDB;
    }
    
    /**
     * @return the uri of the mongo db.
     */
    @Override
    public String getName() {
        return dbUri;
    }
    
    /**
     * @throws a RuntimeException if the collection is null, the obj is null, or the obj
     *         doesn't contain an "_id" or "id" field (the latter is used by autoCompleteIndices).
     */
    protected void validateSave(String collection, Map obj) {
        if (StringUtils.isEmpty(collection) 
            || obj == null 
            ||  ( StringUtils.isEmpty( ObjectUtils.toString( obj.get("_id") ) )  && StringUtils.isEmpty( (String) obj.get("id") ) ) ) {
            throw new IllegalArgumentException("Cannot save object - invalid parms: collection: " + collection + "; obj: " + obj);
        }
    }

    /**
     * 
     */
    @Override
    public void save(String collection, Map obj) {
        // Log.log(this, "adding object with id: " + obj.get("_id"));
        validateSave(collection, obj);
        try {
            mongoDB.getCollection(collection).save( new BasicDBObject(obj) );
        } catch (Exception e) {
            throw new RuntimeException("Exception saving object " + obj.get("_id") + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * 
     */
    @Override
    public void save(String collection, Collection<Map> docs) {
       for (Map doc : docs) {
           save(collection, doc);
       }
    }
    
    
    @Override
    public void update(String collection, Map query, Map fields) {
        mongoDB.getCollection(collection).update( new BasicDBObject(query), 
                                                  new BasicDBObject(fields), 
                                                  false, 
                                                  true );
    }

    /**
     * Run callback against all documents in the given set of collections.
     */
    @Override
    public void forAll(Collection<String> collections, ForAll callback) {
        for (String collectionName : collections ) {
            forAll(collectionName, callback);
        }
    }
    
    /**
     * Run callback against all documents in the given collection.
     */
    @Override
    public void forAll(String collectionName, ForAll callback) {
        forAll(collectionName, Arrays.asList(callback));
    }
    
    /**
     * Run callbacks against all documents in the given collection.
     */
    @Override
    public void forAll(String collectionName, Collection<ForAll> callbacks) {
        
        Log.info(this, "forAll: collection: " + collectionName);
        
        for (ForAll callback : callbacks) {
            callback.before(this, collectionName);
        }
        
        DBCursor dbCursor = mongoDB.getCollection(collectionName).find();
        
        for (DBObject dbobj : dbCursor) {
            for (ForAll callback : callbacks) {
                callback.call( this, collectionName, dbobj.toMap() );
            }
        }
        
        dbCursor.close();
        
        for (ForAll callback : callbacks) {
            callback.after(this, collectionName);
        }
    }

    /**
     * @return the obj with the given _id (note the _id contains the collection name).
     */
    @Override
    public Map read(String _id) {
        return (_id != null) ? read( parseCollectionName(_id), _id ) : null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Map read(String collection, String key, Map<String, Integer> fields) {
        DBObject dbobj = mongoDB.getCollection(collection).findOne( key, new BasicDBObject(fields) );
        return (dbobj != null) ? dbobj.toMap() : null;
    }

    
    /**
     * 
     */
    protected String parseCollectionName(String _id) {
        return _id.substring(0, NumberUtils.max( _id.lastIndexOf('/'), 0, 0) );
    }

    
    /**
     * @return the document at the given key in the given collection
     */
    @Override
    public Map read(String collection, String key) {
        DBObject dbobj = mongoDB.getCollection(collection).findOne( key );
        return (dbobj != null) ? dbobj.toMap() : null;
    }

    /**
     * Drop all collections in the db.
     */
    @Override
    public void drop() {
        mongoDB.dropDatabase();
    }
    
    /**
     * 
     */
    @Override
    public Map getStats() {
        return new MapBuilder<String, Object>()
                        .append( "stats", mongoDB.getStats().toMap() )
                        .append( "collections", getCollectionStats() );
    }
    
    /**
     * 
     */
    protected List<Map> getCollectionStats() {
        List<Map> retMe = new ArrayList<Map>();
        
        for (String collName : mongoDB.getCollectionNames() ) {
            // -rx- retMe.add( toJSONObject( mongoDB.getCollection( collName ) ) );
            Map collStats =  mongoDB.getCollection( collName ).getStats().toMap();
            collStats.put("indexes", getIndexStats( mongoDB.getCollection( collName ) ) );
            retMe.add( collStats );
        }
        
        return retMe;
    }
    
    /**
     * 
     */
    protected List<Map> getIndexStats( DBCollection dbCollection ) {
        List<Map> retMe = new ArrayList<Map>();
        
        for (DBObject dbobj : dbCollection.getIndexInfo() ) {
            retMe.add( dbobj.toMap());
        }
        
        return retMe;
    }

    /**
     * @return a list of libraries for the given language.
     */
    @Override
    public List<Map> getLibraryList(String lang) {
        List<Map> retMe = new ArrayList<Map>();
        
        DBCursor dbCursor = mongoDB.getCollection(DB.LibraryCollectionName).find( new BasicDBObject().append( "lang", lang) );
        
        for (DBObject dbobj : dbCursor ) {
            retMe.add( Cawls.pick( dbobj.toMap(), Arrays.asList("_id", "name", "version", "lang", "metaType") ) );
        }
        
        dbCursor.close();
        
        return retMe;
    }

    /**
     * 
     */
    @Override
    public Map getLibrary(String libraryId) {
        DBObject dbobj = mongoDB.getCollection(DB.LibraryCollectionName).findOne( libraryId );
        return (dbobj != null) ? dbobj.toMap() : null;
    }

    /**
     * 
     */
    @Override
    public List<Map> find(String collection, Map filter) {
        List<Map> retMe = new ArrayList<Map>();

        DBCursor dbCursor = mongoDB.getCollection(collection).find( new BasicDBObject(filter) );
        for (DBObject dbobj : dbCursor) {
            retMe.add( dbobj.toMap());
        }
        dbCursor.close();

        return retMe;
    }
    
    /**
     * 
     */
    @Override
    public List<Map> find(String collection, Map filter, int limit) {
        List<Map> retMe = new ArrayList<Map>(limit);

        DBCursor dbCursor = mongoDB.getCollection(collection).find( new BasicDBObject(filter) ).limit(limit);
        for (DBObject dbobj : dbCursor) {
            retMe.add( dbobj.toMap());
        }
        dbCursor.close();

        return retMe;
    }


    /**
     * @return a list of library documents that match the given lang and libraryName 
     *         (i.e. returns all versions of the given library).
     */
    @Override
    public List<Map> getLibraryVersions(String lang, String libraryName) {
        List<Map> retMe = new ArrayList<Map>();
        
        DBObject filter = new BasicDBObject().append( "lang", lang).append( "name", libraryName);
        
        DBCursor dbCursor = mongoDB.getCollection(DB.LibraryCollectionName).find(filter);
        for (DBObject dbobj : dbCursor ) {
            retMe.add( Cawls.pick( dbobj.toMap(), Arrays.asList("_id", "name", "version", "lang", "metaType") ) );
        }
        dbCursor.close();
        
        return retMe;
    }

    /**
     * @return a list of libraryIds - which correspond to collection names.
     */
    @Override
    public List<String> getLibraryIds(String lang) {
        List<String> retMe = new ArrayList<String>();
        
        DBCursor dbCursor = mongoDB.getCollection(DB.LibraryCollectionName).find( new BasicDBObject().append( "lang", lang) );
        
        for (DBObject dbobj : dbCursor ) {
            retMe.add( (String) dbobj.get("_id") );
        }
        
        dbCursor.close();
        
        return retMe;
    }

    /**
     * 
     */
    @Override
    public void createIndex(String collection, Map keys) {
        mongoDB.getCollection(collection).createIndex( new BasicDBObject(keys) );
    }

    /**
     * Delete the given collection from the DB.
     */
    @Override
    public void drop(String collection) {
        mongoDB.getCollection(collection).drop();
    }

    /**
     * 
     */
    @Override
    public Object remove(String collection, Map filter) {
        return mongoDB.getCollection(collection).remove( new BasicDBObject(filter) );
    }



}
