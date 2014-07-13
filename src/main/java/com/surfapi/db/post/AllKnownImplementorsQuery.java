package com.surfapi.db.post;

import java.util.ArrayList;
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
 * Populate the "allKnownImplementors" index from interface-name to the classes that implement them.
 *
 */
public class AllKnownImplementorsQuery extends CustomIndex<AllKnownImplementorsQuery> {

    private static final String CollectionName = "/q/java/allKnownImplementors";
    
    protected String getCollectionName() {
        return CollectionName;
    }
    
    /**
     * Query for the impmlementors of the given interface name.
     * 
     * @param interfaceName
     *
     * @return the results.
     */
    public List<Map> query(String interfaceName) {
        return getDb().find( getCollectionName(), 
                             new MapBuilder().append( "_interface", interfaceName ) );
    }

    /**
     * Build the index from scratch.
     * 
     * NOTE: This will fully delete the existing index.
     */
    public AllKnownImplementorsQuery buildIndex() {

        Log.info( this, "build: building the allKnownImplementors index" );
        
        getDb().drop( getCollectionName() );
        
        getDb().forAll( (Collection<String>) Cawls.pluck( getDb().getLibraryList("java"), "_id"), new IndexBuilder() );
        
        ensureIndex();
        
        return this;
    }

    /**
     * Add the documents from the given libraries to the index.
     */
    public AllKnownImplementorsQuery addLibrariesToIndex( List<String> libraryIds ) {

        Log.info( this, "addLibrariesToIndex: " + libraryIds);
        
        getDb().forAll( libraryIds , new IndexBuilder() );

        ensureIndex();
        
        return this;
    }
    
    
    /**
     * Ensure the proper columns are indexed.
     */
    protected void ensureIndex() {
        getDb().createIndex( getCollectionName(), new MapBuilder().append( "_interface", 1 )
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
        * Note: this method and buildDocument() below are the only two methods
        *       that are different between this class and AllKnownSubclassesQuery.
        *       There's a pattern here that can be factored out - but it's not
        *       worth doing that now.  The boilerplate code isn't all that much
        *       to begin with.
        *
        * @return the "interfaces" field if it's a class; otherwise nothing.
        */
       protected List<Map> getInterfaces(Map javadocModel) {
           if ( JavadocMapUtils.isInterface(javadocModel) ) {
               return Collections.EMPTY_LIST; 
           } else {
               // return (List<Map>) javadocModel.get("interfaces");
               return (List<Map>) javadocModel.get("allInterfaceTypes");
           }
       }

       /**
        * 
        */
       protected List<Map> buildDocuments(Map javadocModel) {
           List<Map> retMe = new ArrayList<Map>();

           for (Map intf : getInterfaces(javadocModel)) {
               retMe.add( buildDocument( javadocModel, intf) );
           }

           return retMe;
       }

       /**
        * The given javadocModel is an implementor of the given interface.  Create
        * an index item that maps interface -> implementor.
        *
        * @return a subset of fields from the javadocModel, along with a few fields
        *         needed by the index.
        */
       protected Map buildDocument(Map javadocModel, Map intf) {
           Map retMe = JavadocMapUtils.buildTypeStub(javadocModel);

           // The _id field is constructed without library version such that only 1 version of the entry exists
           retMe.put( "_id", JavadocMapUtils.getQualifiedName(intf) 
                             + JavadocMapUtils.getIdSansVersion( javadocModel ) );
           
           retMe.put( "_interface", JavadocMapUtils.getQualifiedName(intf) );
           retMe.put( JavadocMapUtils.LibraryFieldName, javadocModel.get( JavadocMapUtils.LibraryFieldName) );

           return retMe;
       }
    }
    

}
