package com.surfapi.db.post;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.surfapi.db.DB;

public abstract class CustomIndex<T extends CustomIndex> {

    /**
     * @return the list of all custom indexes.
     */
    public static List<CustomIndex<?>> getAllIndexes() {
        List<CustomIndex<?>> retMe = new ArrayList<CustomIndex<?>>();
       
        retMe.add( new AutoCompleteIndex() );
        retMe.add( new ReferenceNameQuery() );
        retMe.add( new AllKnownSubclassesQuery() );
        retMe.add( new AllKnownImplementorsQuery() );
        
        return retMe;
    }
    
    /**
     * @param filter simple class name filter
     * 
     * @return the list of all custom indexes whose class name (simple name) matches the given filter.
     *         If the filter is empty, all indexes are returned.
     */
    public static List<CustomIndex<?>> getIndexes(String filter) {
        
        if (StringUtils.isEmpty(filter)) {
            return getAllIndexes();
        }
        
        List<CustomIndex<?>> retMe = new ArrayList<CustomIndex<?>>();
        
        for (CustomIndex<?> customIndex : getAllIndexes()) {
            if (customIndex.getClass().getSimpleName().equals(filter)) {
                retMe.add(customIndex);
            }
        }
        
        return retMe;
    }
    
    /**
     * @return List of CustomIndex.getBuilder()
     */
    public static List<DB.ForAll> getBuilders(Collection<CustomIndex<?>> customIndexes) {
        List<DB.ForAll> retMe = new ArrayList<DB.ForAll>();
        
        for (CustomIndex customIndex : customIndexes) {
            retMe.add( customIndex.getBuilder() );
        }
        
        return retMe;
    }
    
    
    /**
     * @return the list of all custom indexes whose class name matches the given filter.
     */
    public static List<String> getAllIndexSimpleNames() {
        
        List<String> retMe = new ArrayList<String>();
        
        for (CustomIndex<?> customIndex : getAllIndexes()) {
            retMe.add( customIndex.getClass().getSimpleName() );
        }
        
        return retMe;
    }
            
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
    
    /**
     * @return the injected db
     */
    protected DB getDb() {
        return db;
    }
    
    /**
     * @return the collection name associated with this index
     */
    protected String getCollectionName() {
        return collectionName;
    }

    /**
     * Build the index from the ground up. 
     * 
     * Note: it depends on the implementation, but this method typically deletes 
     * the index first if it already exists
     */ 
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

    /**
     * Default impl is a NO-OP.
     */
    public T removeLibrary(String libraryId) {
       return (T) this;
    }
    
    /**
     * @return the builder, which iterates over the models in a library.
     */
    public abstract DB.ForAll getBuilder();
    
    
}
