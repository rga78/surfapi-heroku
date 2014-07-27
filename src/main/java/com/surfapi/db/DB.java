package com.surfapi.db;


import java.util.Collection;
import java.util.List;
import java.util.Map;


/**
 * Until we have mongodb....
 */
public interface DB  {
    
    public static final String LibraryCollectionName = "libraries";
    
    /**
     * Save the given document to the given collection in the DB.
     */
    public void save(String collection, Map obj) ;
    
    /**
     * Save the given set of documents to the given collection in the DB.
     */
    public void save(String collection, Collection<Map> docs);
    
    /**
     * forAll callback.
     */
    public static interface ForAll {
        public void call(DB db, String collection, Map obj) ;
    }
    
    /**
     * Run given forAll callback against every document in the given set of collections.
     */
    public void forAll( Collection<String> collections, ForAll callback );
    
    /**
     * Run given forAll callback against every document in the given collection.
     */
    public void forAll( String collection, ForAll callback );

    /**
     * @return the obj in the given collection at the given key.
     */
    public Map read(String collection, String key) ;

    /**
     * @return db stats, as a map.
     */
    public Map getStats();

    /**
     * @return a list of libraries for the given programming language.
     */
    public List<Map> getLibraryList(String lang);

    /**
     * @return a summary object for the given library
     */
    public Map getLibrary(String libraryId);

    /**
     * @return a list of documents that match the given filter.
     */
    public List<Map> find(String collection, Map filter);
    
    /**
     * @return a list of documents that match the given filter. At most limit documents are returned.
     */
    public List<Map> find(String collection, Map filter, int limit);


    /**
     * @return list of versions for the given library.
     */
    public List<Map> getLibraryVersions(String lang, String libraryName);

    /**
     * @return list of library IDs, which correspond to collection names.
     */
    public List<String> getLibraryIds(String lang);

    /**
     * @return the document with thei given ID.  Note the id includes the collection name
     *         in it.
     */
    public Map read(String _id);

    /**
     * Build an index for the given collection on the given keys.
     */
    public void createIndex(String collection, Map keys);

    /**
     * Delete the given collection from the DB.
     */
    public void drop(String collection);

    /**
     * Delete the entire DB.
     */
    public void drop();

    /**
     * Remove documents from the given collection that match the given filter.
     */
    public Object remove(String collection, Map filter);

    /**
     * @return the name/id of this db.
     */
    public String getName();
    
}

