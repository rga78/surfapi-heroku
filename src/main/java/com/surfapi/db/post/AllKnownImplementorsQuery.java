package com.surfapi.db.post;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.app.LibraryUtils;
import com.surfapi.coll.Cawls;
import com.surfapi.coll.ListBuilder;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.BulkWriter;
import com.surfapi.db.DB;
import com.surfapi.db.MongoDBImpl;
import com.surfapi.log.Log;

/**
 * Populate the "allKnownImplementors" index from interface-name to the classes that implement them.
 *
 * TODO: This and AllKnownSubclassesQuery are almost identical.  Refactor out the pattern.
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
     * Add the documents from the given library to the index.
     */
    public AllKnownImplementorsQuery addLibraryToIndex( String libraryId ) {

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
    public AllKnownImplementorsQuery removeLibrary( String libraryId ) {

        Log.info( this, "removeLibrary: " + libraryId);
        
        Map<String, String> library =  JavadocMapUtils.mapLibraryId(libraryId);
        
        getDb().remove( getCollectionName(), new MapBuilder()
                                                    .append( "_id", buildIdsForLibraryCriteria( library ) )
                                                    .append( "_libraryVersions", new ListBuilder<String>().append( library.get("version") ) ) );

        return removeLibraryFromExistingEntries(library);   
    }
    
    /**
     * Update the _libraryVersions field in all entries that match the given library (sans version)
     * and remove the given library.
     * 
     */
    protected AllKnownImplementorsQuery removeLibraryFromExistingEntries(Map library) {
        
        getDb().update( getCollectionName(), 
                        new MapBuilder().append( "_id", buildIdsForLibraryCriteria( library ) ),
                        new MapBuilder().append( "$pull", new MapBuilder().append( "_libraryVersions", library.get("version") ) ) );
       
        return this;
    }

    /**
     * @return the criteria for matching the _id field to the given library
     */
    protected Map buildIdsForLibraryCriteria(Map libraryModel) {
        return new MapBuilder().append( "$regex", "^" + LibraryUtils.getIdSansVersion(libraryModel) + ".*");
    }
    
    
    
    /**
     * Ensure the proper columns are indexed.
     */
    protected void ensureIndex() {
        getDb().createIndex( getCollectionName(), new MapBuilder().append( "_interface", 1 )
                                                                  .append( "qualifiedName", 1 ) );
    }
    
    
    private class IndexBuilder implements DB.ForAll {
        
       private BulkWriter bulkWriter;
        
       @Override
       public void before(DB db, String collection) {
           bulkWriter = new BulkWriter( (MongoDBImpl) getDb(), getCollectionName());
                                // .setWriteConcern( WriteConcern.UNACKNOWLEDGED );
       }
        
       @Override
       public void call(DB db, String collection, Map javadocModel) {
           insert(javadocModel);
       }
 
       @Override 
       public void after(DB db, String collection) {
           bulkWriter.flush();
       }

       public void insert(Map javadocModel) {
           if (JavadocMapUtils.isClass(javadocModel)) {
               // getDb().save( getCollectionName(), buildDocuments(javadocModel) );
               bulkWriter.insert( buildDocuments(javadocModel) );
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
               retMe.add( versionedDocument( buildDocument( javadocModel, intf) ) );
           }

           return retMe;
       }
       
       /**
        * Create a "versioned" instance of the given document.  Lookup the indexedDocument
        * in the query first.  If there's an existingDocuemnt (probably due to another version
        * of the library), then pull out its "libraryVersions" field, which is a Set of 
        * versions.  Put the current library version in the set and then put it in the
        * "versioned" indexedDocument.
        * 
        * @return a "versioned" form of the given indexedDocument
        */
       protected Map versionedDocument( Map indexedDocument ) {
           
           Map existingDocument = getDb().read( getCollectionName(), (String) indexedDocument.get("_id"));
           
           List<String> libraryVersions = (existingDocument != null) 
                                           ? (List<String>) existingDocument.get("_libraryVersions")
                                           : new ArrayList<String>();
                                           
           libraryVersions.add( JavadocMapUtils.getLibraryVersion(indexedDocument) );
           
           indexedDocument.put("_libraryVersions", libraryVersions);
           
           return indexedDocument;
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
           retMe.put( "_id", buildDocumentId(javadocModel, intf) );
           
           retMe.put( "_interface", JavadocMapUtils.getQualifiedName(intf) );
           retMe.put( JavadocMapUtils.LibraryFieldName, javadocModel.get( JavadocMapUtils.LibraryFieldName) );

           return retMe;
       }
       
       /**
        * The _id field is constructed without library version such that only 1 version of the entry exists
        *         *
        * @return the _id field for the indexed document.
        */
       protected String buildDocumentId(Map javadocModel, Map intf) {
           return JavadocMapUtils.getIdSansVersion( javadocModel )
                           + JavadocMapUtils.getQualifiedName(intf); 
         
       }
    }
    

}
