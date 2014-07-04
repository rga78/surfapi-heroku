package com.surfapi.db.post;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.DB;
import com.surfapi.log.Log;

/**
 * Note: subclasses are registered only for their IMMEDIATE superclass (not
 *       parents of the superclass).
 */
public class AllKnownSubclassesQuery {
    
    private DB db;
    
    private static final String CollectionName = "/q/java/allKnownSubclasses";

    public AllKnownSubclassesQuery inject(DB db) {
        this.db = db;
        return this;
    }

    protected DB getDb() {
        return db;
    }
    
    protected String getCollectionName() {
        return CollectionName;
    }
    
    /**
     * Build the index from scratch.
     * 
     * NOTE: This will fully delete the existing index.
     */
    public AllKnownSubclassesQuery buildIndex() {

        Log.info( this, "build: building the allKnownSubclasses index" );
        
        getDb().drop( getCollectionName() );
        
        getDb().forAll( (Collection<String>) Cawls.pluck( db.getLibraryList("java"), "_id"), new IndexBuilder() );
        
        ensureIndex();
        
        return this;
    }
    
    /**
     * Add the documents from the given libraries to the index.
     */
    public AllKnownSubclassesQuery addLibrariesToIndex( List<String> libraryIds ) {

        Log.info( this, "addLibrariesToIndex: " + libraryIds);
        
        getDb().forAll( libraryIds , new IndexBuilder() );

        ensureIndex();
        
        return this;
    }
    
    /**
     * Ensure the proper columns are indexed.
     */
    protected void ensureIndex() {
        getDb().createIndex( getCollectionName(), new MapBuilder().append( "_superclass", 1 )
                                                                  .append( "qualifiedName", 1 ) );
    }
    
    
    private class IndexBuilder implements DB.ForAll {
        
       @Override
       public void call(DB db, String collection, Map javadocModel) {
           insert(javadocModel);
       }
 

       public void insert(Map javadocModel) {
           if (JavadocMapUtils.isClass(javadocModel)) {
               getDb().save( getCollectionName(), buildDocuments(javadocModel) );
           }
       }

       /**
        * @return the "interfaces" field if it's an interface; otherwise the "superclass" field.
        */
       protected List<Map> getSuperclasses(Map javadocModel) {
           if ( JavadocMapUtils.isInterface(javadocModel) ) {
               return (List<Map>) javadocModel.get("interfaces") ;
           } else {
               Map superclass = (Map) javadocModel.get("superclass");
               return (superclass != null) ? Arrays.asList(superclass) : Collections.EMPTY_LIST;
           }
       }

       /**
        * 
        */
       protected List<Map> buildDocuments(Map javadocModel) {
           List<Map> retMe = new ArrayList<Map>();

           for (Map superclass : getSuperclasses(javadocModel)) {
               retMe.add( buildDocument( javadocModel, superclass ) );
           }

           return retMe;
       }

       /**
        * @return a subset of fields from the javadocModel, along with a few fields
        *         needed by the index.
        */
       protected Map buildDocument(Map javadocModel, Map superclass) {
           Map retMe = JavadocMapUtils.buildTypeStub(javadocModel);

           retMe.put( "_id", JavadocMapUtils.getQualifiedName(superclass) + "/" + javadocModel.get("_id") );
           retMe.put( "_superclass", JavadocMapUtils.getQualifiedName(superclass) );
           retMe.put( "id", javadocModel.get("_id") );

           return retMe;
       }
    }

}
