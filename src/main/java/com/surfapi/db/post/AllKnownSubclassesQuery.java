package com.surfapi.db.post;

import java.util.ArrayList;
import java.util.Arrays;
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
 * Index of superclass-name -> subclasses
 * 
 * Note there are only entries per library in the index, not per library VERSION.
 * So when removing must be careful not to remove entries if the entry has
 * a corresponding document in another version of the library.
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
        // DONE: handle libraries with multiple versions
        //       perhaps by querying for the _id while globbing out the version field?
        //       getDb().find( new regex( "/java/com.surfapi/.*?/")
        //       You'd have to go thru every entry that could potentially be deleted (the query
        //       below only don't delete the entries), and check if the relativeId exists 
        //       in any other library.  That's a matter of calling the above query w/ regex.
        //       So we're going to be doing quite a few queries during removal.  That could
        //       take some time.  The other thing is to keep track of the libraries in the
        //       indexDocument.  When the list drops to zero, remove the entry.  This would
        //       save us a few queries.
        // DONE: also removal might be faster if we put the indexDocuemnt _id in front of
        //       the indexed column value, cuz that would be an easier query than what we're
        //       doing now, but not much easier.
        //       essentially it's like indexing the indexDocuemtns on the library field, sans version,
        //       in addition to the _superclass field.
        //       
        getDb().remove( getCollectionName(), new MapBuilder()
                                                    .append( "_id", buildIdsForLibraryCriteria( library ) )
                                                    // .append( "_libraryVersions", new MapBuilder().append( "$size", 1) ) );
                                                    .append( "_libraryVersions", new ListBuilder<String>().append( library.get("version") ) ) );
        
        return removeLibraryFromExistingEntries(library);
    }
    
    /**
     * Update the _libraryVersions field in all entries that match the given library (sans version)
     * and remove the given library.
     * 
     */
    protected AllKnownSubclassesQuery removeLibraryFromExistingEntries(Map library) {
        
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
        getDb().createIndex( getCollectionName(), new MapBuilder().append( "_superclass", 1 )
                                                                  .append( "qualifiedName", 1 ) );
    }
    
    /**
     * @return the index builder
     */
    public DB.ForAll getBuilder() {
        return new IndexBuilder();
    }
    
    
    private class IndexBuilder implements DB.ForAll {
        
        private BulkWriter bulkWriter;
        
        @Override
        public void before(DB db, String collection) {
            Log.info(this, "before: " + collection);
            bulkWriter = new BulkWriter( (MongoDBImpl) getDb(), getCollectionName());
                                // .setWriteConcern( WriteConcern.UNACKNOWLEDGED );
        }

        @Override 
        public void after(DB db, String collection) {
            bulkWriter.flush();
            ensureIndex();
        }
        
        @Override
        public void call(DB db, String collection, Map javadocModel) {
            insert(javadocModel);
        }

        public void insert(Map javadocModel) {
            if (JavadocMapUtils.isClass(javadocModel)) {
                // getDb().save( getCollectionName(), buildDocuments(javadocModel) );
                bulkWriter.insert( buildDocuments(javadocModel) );
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
                   retMe.add( versionedDocument( buildDocument( javadocModel, superclass ) ) );
               }
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
        * @return a subset of fields from the javadocModel, along with a few fields
        *         needed by the index.
        */
       protected Map buildDocument(Map javadocModel, Map superclass) {

           Map retMe = JavadocMapUtils.buildTypeStub(javadocModel);
          
           retMe.put( "_id", buildDocumentId(javadocModel, superclass) );
           
           retMe.put( "_superclass", JavadocMapUtils.getQualifiedName(superclass) );
           retMe.put( JavadocMapUtils.LibraryFieldName, javadocModel.get( JavadocMapUtils.LibraryFieldName) );

           return retMe;
       }
       
       /**
        * The _id field is constructed without library version such that only 1 version of the entry exists
        *         *
        * @return the _id field for the indexed document.
        */
       protected String buildDocumentId(Map javadocModel, Map superclass) {
           return JavadocMapUtils.getIdSansVersion( javadocModel )
                           + JavadocMapUtils.getQualifiedName(superclass); // in case we ever want to include grandparent classes
         
       }
    }

}
