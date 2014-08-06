package com.surfapi.db.post;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.app.JavadocObject;
import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.BulkWriter;
import com.surfapi.db.DB;
import com.surfapi.db.MongoDBImpl;
import com.surfapi.log.Log;

/**
 * Builds auto-complete indexes, which are used by the auto-complete text box
 * that suggests results to users as they type in a class/package/method name.
 *
 * starting point: db filled with collections, 1 collection per library
 *
 * ending point: auto-complete indexes - 1 per collection, 1 per language
 *
 */
public class AutoCompleteIndex extends CustomIndex<AutoCompleteIndex> {

    /**
     * 
     */
    @Override
    public AutoCompleteIndex buildIndex() {
        return buildIndexForLang("java");
    }

    /**
     * Build the auto-complete index for the given lang.
     */
    public AutoCompleteIndex buildIndexForLang( final String lang ) {
        
        String indexName = getCollectionNameForLang( lang ) ;

        getDb().drop( indexName );
        
        Log.info(this, "buildIndexForLang: building index for language " + lang);
        
        // Note: we must processes all libraries -- even different versions of the same library --
        // since different versions may contain different classes.
        getDb().forAll( (Collection<String>) Cawls.pluck( getDb().getLibraryList("java"), "_id"), new IndexBuilder() );
        
        return this;
    }
    
    /**
     * Create the index on the _searchName field for the given auto-complete index collection.
     */
    protected void ensureIndex(String collectionName) {
        getDb().createIndex( collectionName, new MapBuilder().append( "_searchName", 1 ) );
    }

    /**
     * Add the given library to the auto-complete indexes.
     * 
     * The library will be added to the lang's index so long as it's either the
     * first library of its kind or the newest of its kind.
     * 
     * An index for the library itself is also built.
     */
    public AutoCompleteIndex addLibraryToIndex(String libraryId) {
            
        Log.info(this, "addLibraryToIndex: " + libraryId);

        getDb().forAll( libraryId, new IndexBuilder());
        
        return this;
        
    }
    
    /**
     * Remove (1) the library's index and
     *        (2) all entries for the given library from the lang's index.
     */
    public AutoCompleteIndex removeLibrary(String libraryId) {
        
        Map<String, String> library =  JavadocMapUtils.mapLibraryId(libraryId);
        
        Log.info(this, "removeLibrary: removing library " + libraryId + ", " + library.get("name"));
        
        // Remove from lang's index.
        // Note: If this library has multiple versions then we must make sure not to 
        // delete the entries associated with other versions.  If there's only
        // 1 version, then we're ok.
        // TODO: handle libraries with multiple versions
        if (getDb().getLibraryVersions( library.get("lang"), library.get("name") ).size() == 1) {
            
            String indexName = getCollectionNameForLang( (String) library.get("lang") ) ;
        
            getDb().remove( indexName, new MapBuilder<String, String>()
                                              .append( JavadocMapUtils.LibraryFieldName + ".name", library.get("name")) );
        }
        
        // Remove library's index
        getDb().drop( getCollectionName(libraryId) );
        
        return this;
     }

    /**
     * @return the auto-complete index collection name for the given collection
     */
    public static String getCollectionName( String libraryId ) {
        return libraryId + "/autoCompleteIndex";
    }

    /**
     * @return the auto-complete index collection name for the given language
     */
    public static String getCollectionNameForLang( String lang ) {
        return getCollectionName(lang);
    }
    

    /**
     * Query the given auto-complete index on the given text.
     * 
     * @param indexName the index name ("java" or a libraryId)
     * @param text the text to query on
     *
     * @return the results.
     */
    public List<Map> query(String indexName, String text, int limitResults) {
        return getDb().find( getCollectionName( indexName ), 
                             new MapBuilder().append( "_searchName", buildSearchNameCriteria(text) ),
                             limitResults );
    }
    
    /**
     * @return the criteria by which to match against the _searchName field in the query
     */
    protected Object buildSearchNameCriteria(String text) {
        return (!text.endsWith(" ")) 
                ? new MapBuilder().append( "$regex", "^" + normalizeSearchName(text) + ".*")
                : normalizeSearchName(text.trim());
    }
    
    /**
     * @return the searchName, lowercase.
     */
    protected static String normalizeSearchName(String searchName) {
        return searchName.toLowerCase();
    }
    
    /**
     * @return the index builder
     */
    public DB.ForAll getBuilder() {
        return new IndexBuilder();
    }
    
    
    /**
     * Builds the index.
     */
    protected class IndexBuilder implements DB.ForAll {
        
        /**
         * bulk writes for the library-specific index.
         */
        private BulkWriter bulkWriter;
        
        /**
         * bulk writes to the lang index.
         */
        private BulkWriter bulkWriterLang;
        
        @Override
        public void before(DB db, String collection) {
            Log.info(this, "before: " + collection);
            bulkWriter = new BulkWriter( (MongoDBImpl) getDb(), getCollectionName(collection));
            bulkWriterLang = new BulkWriter( (MongoDBImpl) getDb(), 
                                             getCollectionNameForLang( JavadocMapUtils.mapLibraryId(collection).get("lang") ));
                                                // .setWriteConcern( WriteConcern.UNACKNOWLEDGED );
        }

        @Override 
        public void after(DB db, String collection) {
            bulkWriter.flush();
            bulkWriterLang.flush();
            
            ensureIndex( getCollectionName(collection) );
            ensureIndex( getCollectionNameForLang( JavadocMapUtils.mapLibraryId(collection).get("lang") ) );
        }
        
        /**
         * 
         */
        @Override
        public void call(DB db, String collection, Map obj) {
            for ( Map indexedObj : buildIndexedDocuments( new JavadocObject( obj ) ) ) {
                bulkWriterLang.insert( new HashMap(indexedObj) );
                bulkWriter.insert( indexedObj );
            }
        }
        
        /**
         * @return true if the given obj should be indexed (a class or package)
         */
        protected boolean isIndexable(Map obj) {
            return JavadocMapUtils.isClass(obj) || JavadocMapUtils.isPackage(obj);
        }
        
        /**
         * @return an _id for the autoCompleteIndex collection
         */
        protected String buildId(String searchName, Map javadocModel) {
            return "/" + searchName + JavadocMapUtils.getIdSansVersion(javadocModel);
        }

        /**
         *       
         * @return a document to use for the auto-complete index
         */
        public List<Map> buildIndexedDocuments( JavadocObject doc ) {

            List<Map> retMe = new ArrayList<Map>();

            if (! isIndexable(doc.getJson()) ) {
                return retMe;
            }

            if ( doc.isPackage() ) {

                // For packages, add entries not only for the fully qualified package name but also
                // for each sub-segment of the package name.  E.g., for java.util.concurrent.atomic,
                // also add entries for atomic, concurrent.atomic, util.concurrent.atomic.
                for (String nameSegment : getQualifiedNameSegments( doc.getString("name") ) ) {

                    retMe.add( new MapBuilder<String,Object>()
                                               .append("_id", buildId( normalizeSearchName(nameSegment), doc.getJson()) ) // -rx- , UUID.randomUUID().toString()) 
                                               .append( "id", doc.getId() )     
                                               .append( "_searchName", normalizeSearchName(nameSegment) )
                                               .append( "name", doc.getString("name") )
                                               .append( "qualifiedName", doc.getQualifiedName() )
                                               .append( JavadocMapUtils.LibraryFieldName, doc.getLibrary() ) );
                }
            } else {

                // NOTE: deliberately not using "_id", because some documents may get inserted into the index more than once
                //       for different search strings (e.g. packages are inserted for each segment of their name).

                // Why not use mongodb's auto-assigned ID? Cuz the auto-assigned _id field in mongoDb is an ObjectId object, 
                // which isn't a valid JSON element so the JSON can't be parsed.
                retMe.add( new MapBuilder<String,Object>().append("_id", buildId( normalizeSearchName(doc.getString("name")), doc.getJson()) ) // -rx- .append("_id", UUID.randomUUID().toString())
                                                          .append( "id", doc.getId() )     
                                                          .append( "_searchName", normalizeSearchName(doc.getString("name")) )
                                                          .append( "name", doc.getString("name") )
                                                          .append( "qualifiedName", doc.getQualifiedName() )
                                                          .append( JavadocMapUtils.LibraryFieldName, doc.getLibrary() ) );
            }

            return retMe;
        }
        
        /**
         * 
         * segmentName("java.nio.concurrent") => java.nio.concurrent, nio.concurrent, concurrent
         * 
         * @return the list of segmented names
         */
        protected List<String> getQualifiedNameSegments(String name) {

            List<String> retMe = new ArrayList<String>();
            
            String[] segments = StringUtils.split(name, ".");
            
            for (int i=0; i < segments.length; ++i) {
                retMe.add( StringUtils.join(segments, ".", i, segments.length) );
            }
            
            return retMe;
        }

    }


}
