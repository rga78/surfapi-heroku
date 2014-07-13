package com.surfapi.db.post;

import java.util.List;

import com.surfapi.db.DB;

public abstract class CustomIndex<T extends CustomIndex> {

    /**
     * Injected REF to DB.
     */
    private DB db;
    
    /**
     * The custom index collection name.
     */
    private String collectionName;
    
    /**
     * Default CTOR.
     */
    public CustomIndex() {}
    
    /**
     * CTOR, takes collection name.
     */
    public CustomIndex(String collectionName) {
        this.collectionName = collectionName;
    }

    /**
     * inject DB.
     */
    public T inject(DB db) {
        this.db = db;
        return (T) this;
    }
    
    protected DB getDb() {
        return db;
    }
    
    protected String getCollectionName() {
        return collectionName;
    }

    public abstract T buildIndex() ;

    /**
     * Default impls calls addLibraryToIndex on all libraryIds.
     */
    public T addLibrariesToIndex(List<String> libraryIds) {
        for (String libraryId : libraryIds) {
            addLibraryToIndex(libraryId);
        }
        
        return (T) this;
    }

    /**
     * Default impl is NO-OP.
     */
    public T addLibraryToIndex(String libraryId) {
        return (T) this;
    }
    
    
}
