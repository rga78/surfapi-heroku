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
 * Index of superclass-name -> subclasses
 * 
 * Note: subclasses are registered only for their IMMEDIATE superclass (not
 *       parents of the superclass).
 */
public class AllKnownSubclassesQuery extends CustomIndex<AllKnownSubclassesQuery> {
    
    
    private static final String CollectionName = "/q/java/allKnownSubclasses";
    
    protected String getCollectionName() {
        return CollectionName;
    }
    
    /**
     * Query for the subclasses of the given class name.
     * 
     * @param superclassName
     *
     * @return the results.
     */
    public List<Map> query(String superclassName) {
        return getDb().find( getCollectionName(), 
                             new MapBuilder().append( "_superclass", superclassName ) );
    }
    
    /**
     * Build the index from scratch.
     * 
     * NOTE: This will fully delete the existing index.
     */
    public AllKnownSubclassesQuery buildIndex() {

        Log.info( this, "build: building the allKnownSubclasses index" );
        
        getDb().drop( getCollectionName() );
        
        getDb().forAll( (Collection<String>) Cawls.pluck( getDb().getLibraryList("java"), "_id"), new IndexBuilder() );
        
        // -rx- getDb().forAll( (Collection<String>) Cawls.pluck( LibraryUtils.latestVersionsOnly( db.getLibraryList("java") ), "_id" ), 
        //                 new IndexBuilder() );
        
        ensureIndex();
        
        return this;
    }
    
    /**
     * Add the documents from the given library to the index.
     */
    public AllKnownSubclassesQuery addLibraryToIndex( String libraryId ) {

        Log.info( this, "addLibraryToIndex: " + libraryId);
        
        getDb().forAll( libraryId , new IndexBuilder() );

        ensureIndex();
        
        return this;
    }
    
    /**
     * Remove the given library from the index.  
     * 
     * That is, remove all entries associated with this library.
     */
    public AllKnownSubclassesQuery removeLibrary( String libraryId ) {

        Log.info( this, "removeLibrary: " + libraryId);
        
        Map<String, String> library =  JavadocMapUtils.mapLibraryId(libraryId);
        
        // Note: If this library has multiple versions then we must make sure not to 
        // delete the entries associated with other versions.  If there's only
        // 1 version, then we're ok.
        // TODO: handle libraries with multiple versions
        if (getDb().getLibraryVersions( library.get("lang"), library.get("name") ).size() == 1) {
            
            // TODO: probably should be filtering on lang too.
            getDb().remove( getCollectionName(), new MapBuilder<String, String>()
                                                      .append( JavadocMapUtils.LibraryFieldName + ".name", library.get("name")) );
        }
        
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
               // Don't add all the subclasses of java.lang.Object - there's just too many..
               if ( !JavadocMapUtils.getQualifiedName(superclass).equals("java.lang.Object")) {
                   retMe.add( buildDocument( javadocModel, superclass ) );
               }
           }

           return retMe;
       }

       /**
        * @return a subset of fields from the javadocModel, along with a few fields
        *         needed by the index.
        */
       protected Map buildDocument(Map javadocModel, Map superclass) {
           Map retMe = JavadocMapUtils.buildTypeStub(javadocModel);

           // The _id field is constructed without library version such that only 1 version of the entry exists
           retMe.put( "_id", JavadocMapUtils.getQualifiedName(superclass) 
                             + JavadocMapUtils.getIdSansVersion( javadocModel ) );
           
           retMe.put( "_superclass", JavadocMapUtils.getQualifiedName(superclass) );
           retMe.put( JavadocMapUtils.LibraryFieldName, javadocModel.get( JavadocMapUtils.LibraryFieldName) );

           return retMe;
       }
    }

}
