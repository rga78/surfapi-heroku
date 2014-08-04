package com.surfapi.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;

/**
 * A simple DB implementation for unit testing purposes.
 * 
 * DBImpl itself is a Map of collection names to collections.  Each collection
 * is itself a map of item _ids to items.  So DBIml is a Map of Maps of Maps.
 */
public class DBImpl extends ConcurrentHashMap<String, Map<String, Map>> implements DB {
    
    /**
     * @return the given collection
     */
    @Override
    public Map get(Object collection) {
        Map retMe = super.get(collection);
        if (retMe == null) {
            super.putIfAbsent((String)collection, new HashMap<String, Map>());
            retMe = super.get(collection);
        }
        return retMe;
    }

    /**
     * @throws a RuntimeException if the collection is null, the obj is null, or the obj
     *         doesn't contain an "_id" field.
     */
    public void validateSave(String collection, Map obj) {
        if (StringUtils.isEmpty(collection) || obj == null || StringUtils.isEmpty( (String) obj.get("_id") ) ) {
            throw new IllegalArgumentException("Cannot save object - invalid parms: collection: " + collection + "; obj: " + obj);
        }
    }
    
    /**
     * Add the given doc element from the given library to the DB.
     */
    @Override
    public void save(String collection, Map obj) {
        validateSave(collection,obj);
        get(collection).put( (String) obj.get("_id"), obj );
    }

    /**
     * 
     */
    @Override
    public void forAll(Collection<String> collections, ForAll callback) {
        for ( String collectionName : collections) {
            forAll(collectionName, callback);
        }
    }
    

    @Override
    public void forAll(String collectionName, ForAll callback) {
        Map<String, Map> collection = get(collectionName);
        callback.before(this, collectionName);
        for (Map obj : collection.values()) {
            callback.call(this, collectionName, obj);
        }
        callback.after(this,collectionName);
    }


    /**
     * 
     */
    @Override
    public Map read(String collection, String key) {
        return (StringUtils.isEmpty(collection)) ? null : (Map) get(collection).get(key);
    }
    
    /**
     * @return the obj with the given _id (note the _id contains the collection name).
     */
    @Override
    public Map read(String _id) {
        return (_id != null) ? read( parseCollectionName(_id), _id ) : null;
    }
    
    /**
     * 
     */
    protected String parseCollectionName(String _id) {
        return _id.substring(0, NumberUtils.max( _id.lastIndexOf('/'), 0, 0) );
    }
    
    /**
     * 
     */
    @Override
    public Map getStats() {
        return new HashMap();
    }

    /**
     * @return a list of libraries for the given language.
     */
    @Override
    public List<Map> getLibraryList(String lang) {
        List<Map> retMe = new ArrayList<Map>();
        
        for (Map dbobj : find(DB.LibraryCollectionName, new MapBuilder().append( "lang", lang) ) ) {
            retMe.add( Cawls.pick( dbobj, Arrays.asList("_id", "name", "version", "lang", "metaType") ) );
        }
        
        return retMe;
    }

    /**
     * 
     */
    @Override
    public Map getLibrary(String libraryId) {
        return read(DB.LibraryCollectionName, libraryId);
    }

    /**
     * 
     */
    @Override
    public List<Map> find(String collection, Map filter) {
        return Cawls.findAll( get(collection).values(), filter);
    }
    

    @Override
    public List<Map> find(String collection, Map filter, int limit) {
        List<Map> results = find(collection, filter);
        return results.subList(0, Math.min( results.size(), limit ) );
    }


    /**
     * 
     */
    @Override
    public List<Map> getLibraryVersions(String lang, String libraryName) {
        List<Map> retMe = new ArrayList<Map>();
        for (Map dbobj : find(DB.LibraryCollectionName, new MapBuilder().append("lang", lang).append( "name", libraryName) ) ) {
            retMe.add( Cawls.pick( dbobj, Arrays.asList("_id", "name", "version", "lang", "metaType") ) );
        }
        return retMe;
    }

    /**
     * @return list of libraryIds (which correspond to collection names).
     */
    @Override
    public List<String> getLibraryIds(String lang) {
        return (List<String>) Cawls.pluck( find(DB.LibraryCollectionName, new MapBuilder().append( "lang", lang)), "_id" );
    }

    /**
     * 
     */
    @Override
    public void createIndex(String collection, Map keys) {
        // NO-OP. throw new IllegalArgumentException("DBImpl.createIndex is not implemented");
    }

    /**
     * Delete the given collection from the DB.
     */
    @Override
    public void drop(String collection) {
        super.remove(collection);
    }

    /**
     * Delete the entire DB.
     */
    @Override
    public void drop() {
        super.clear();
    }

    /**
     * 
     */
    @Override
    public Object remove(String collection, Map filter) {
        for (Map doc : find(collection, filter)) {
            // TODO
        }
        return null;
    }

    @Override
    public void save(String collection, Collection<Map> docs) {
       for (Map doc : docs) {
           save(collection, doc);
       }
        
    }

    @Override
    public String getName() {
        return "Map-based-db-for-testing";
    }

    @Override
    public void update(String collectionName, Map query, Map fields) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public com.mongodb.DB getMongoDB() {
        throw new RuntimeException("not implemented");
    }


}
