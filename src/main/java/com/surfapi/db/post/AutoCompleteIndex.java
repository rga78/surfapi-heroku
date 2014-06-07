package com.surfapi.db.post;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.app.JavadocObject;
import com.surfapi.app.LibraryUtils;
import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.DB;
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
public class AutoCompleteIndex {

    /**
     * Injected REF to DB.
     */
    private DB db;

    /**
     * inject DB.
     */
    public AutoCompleteIndex inject(DB db) {
        this.db = db;
        return this;
    }

    /**
     * Build the auto-complete index for the given lang.
     */
    public void buildIndexForLang( final String lang ) {
        
        String indexName = buildAutoCompleteIndexNameForLang( lang ) ;

        db.drop( indexName );
        
        Log.info(this, "buildIndexForLang: building index for language " + lang);
        
        // Build the index against the *latest versions only* of each library.
        db.forAll( (Collection<String>) Cawls.pluck( LibraryUtils.latestVersionsOnly( db.getLibraryList(lang) ), "_id" ), 
                   new IndexBuilder( indexName ) );

        db.createIndex( indexName, new MapBuilder().append( "_searchName", 1 ) );
        
        buildIndexesForLibraries( db.getLibraryIds(lang) );
    }
    
    /**
     * Build auto-complete indexes for all the given libraries.
     */
    protected void buildIndexesForLibraries(Collection<String> libraryIds) {
        for (String libraryId : libraryIds) {
            buildIndexForLibrary(libraryId);
        }
    }

    /**
     * Build the auto-complete index for the given library.
     */
    protected void buildIndexForLibrary( String libraryId ) {

        String indexName = buildAutoCompleteIndexName( libraryId );
        db.drop( indexName  );
        
        Log.info(this, "buildIndex: building index for library " + libraryId);

        db.forAll( Arrays.asList(libraryId), new IndexBuilder( indexName ));

        db.createIndex( indexName, new MapBuilder().append( "_searchName", 1 ) );
    }
    
    /**
     * Add the given libraries to the auto-complete index.
     */
    public void addLibrariesToIndex(List<String> libraryIds) {
        for (String libraryId : libraryIds) {
            addLibraryToIndex(libraryId);
        }
    }

    /**
     * Add the given library to the auto-complete indexes.
     * 
     * The library will be added to the lang's index so long as it's either the
     * first library of its kind or the newest of its kind.
     * 
     * An index for the library itself is also built.
     */
    public void addLibraryToIndex(String libraryId) {

        Map library =  JavadocMapUtils.mapLibraryId(libraryId);
        
        String indexName = buildAutoCompleteIndexNameForLang( (String) library.get("lang") ) ;
        
        if (shouldAddLibraryToIndex( library ) ) {
            
            Log.info(this, "addLibraryToIndex: adding library " + libraryId + ", " + library.get("name"));
            
            // Remove.
            db.remove( indexName, new MapBuilder<String, String>()
                                        .append( JavadocMapUtils.LibraryFieldName + ".name", (String) library.get("name")) );
            
            db.forAll( Arrays.asList(libraryId), new IndexBuilder( indexName ));
        }
        
        buildIndexForLibrary(libraryId);
        
    }
    
    /**
     * @return true if the given library should be added to the auto-complete index.
     *         A library is added if it's the first of its kind or if its version
     *         is newer (greater) than any other of its kind.
     */
    protected boolean shouldAddLibraryToIndex( Map library ) {
        
        // Note: the library we're adding is already in the library versions list.
        // It was added by the DbLoader. So if the list only has one entry, then
        // it must be for this library. Otherwise check if this library is newer
        // than every other one in the list.
        List<Map> libraryVersions = db.getLibraryVersions("java", (String) library.get("name") );
        
        if (libraryVersions.size() == 1) {
            return true;
        } else {
            // Sort the libraries in reverse order (newest version first).
            // If the library being added is at the head of the list, then
            // it must be the newest.
            Collections.sort( libraryVersions, new Comparator<Map>() {
                public int compare(Map lib1, Map lib2) {
                    return (-1) * LibraryUtils.libraryCompareVersion(lib1, lib2);
                }
            });
            
            if ( library.get("version").equals( libraryVersions.get(0).get("version") ) ) {
                return true;
            }
        }
        
        return false;
    }


    /**
     * @return the auto-complete index collection name for the given collection
     */
    public static String buildAutoCompleteIndexName( String collectionName ) {
        return collectionName + "/autoCompleteIndex";
    }

    /**
     * @return the auto-complete index collection name for the given language
     */
    public static String buildAutoCompleteIndexNameForLang( String lang ) {
        return lang + "/autoCompleteIndex";
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
        return db.find( buildAutoCompleteIndexName( indexName ), 
                        new MapBuilder().append( "_searchName", new MapBuilder().append( "$regex", "^" + text + ".*") ), 
                        limitResults );
    }
    
    
    /**
     * Builds the index.
     */
    protected static class IndexBuilder implements DB.ForAll {
        
        /**
         * The index (collection) name.
         */
        private String indexName;
        
        /**
         * CTOR.
         * 
         * @param indexName
         */
        public IndexBuilder(String indexName) {
            this.indexName = indexName;
        }
        
        /**
         * 
         */
        @Override
        public void call(DB db, String collection, Map obj) {
            for ( Map indexedObj : buildIndexedDocuments( new JavadocObject( obj ) ) ) {
                db.save( indexName, indexedObj );
            }
        }
        
        /**
         * @return true if the given obj should be indexed (a class or package)
         */
        protected boolean isIndexable(Map obj) {
            return JavadocMapUtils.isClass(obj) || JavadocMapUtils.isPackage(obj);
        }

        /**
         * @return a document to use for the auto-complete index
         */
        public List<Map> buildIndexedDocuments( JavadocObject doc ) {

            List<Map> retMe = new ArrayList<Map>();

            if (! isIndexable(doc.getJson()) ) {
                return retMe;
            }

            if ( doc.isPackage() ) {

                List<String> nameSegments = Arrays.asList( StringUtils.split( doc.getString("name"), "." ) );

                // Insert for every segment except the first (the first is covered by the entry using the full name).
                for (String nameSegment : nameSegments.subList( 1, nameSegments.size() ) ) {

                    retMe.add( new MapBuilder<String,Object>().append("_id", UUID.randomUUID().toString()) 
                                               .append( "id", doc.getId() )     
                                               .append( "_searchName", nameSegment )
                                               .append( "name", doc.getString("name") )
                                               .append( "qualifiedName", doc.getQualifiedName() )
                                               .append( JavadocMapUtils.LibraryFieldName, doc.getLibrary() ) );
                }
            }

            // NOTE: deliberately not using "_id", because some documents may get inserted into the index more than once
            //       for different search strings (e.g. packages are inserted for each segment of their name).
            
            // Why not use mongodb's auto-assigned ID? Cuz the auto-assigned _id field in mongoDb is an ObjectId object, 
            // which isn't a valid JSON element so the JSON can't be parsed.
            retMe.add( new MapBuilder<String,Object>().append("_id", UUID.randomUUID().toString())
                                       .append( "id", doc.getId() )     
                                       .append( "_searchName", doc.getString("name") )
                                       .append( "name", doc.getString("name") )
                                       .append( "qualifiedName", doc.getQualifiedName() )
                                       .append( JavadocMapUtils.LibraryFieldName, doc.getLibrary() ) );

            return retMe;
        }

    }


}
