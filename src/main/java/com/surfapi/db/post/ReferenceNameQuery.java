
package com.surfapi.db.post;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.app.LibraryUtils;
import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.DB;
import com.surfapi.log.Log;

/**
 * The java "reference" query.
 *
 * For querying the entire DB for documents that match a given reference name,
 * e.g com.surfapi.test.DemoJavadoc#parse, except the '#' is replaced with '+',
 * so it can be used in URIs.
 *
 * Reference names match the format of javadoc @see/@link tag links.
 *
 * This query helps resolve inter-document and inter-library links.
 *
 */
public class ReferenceNameQuery {

    /**
     *
     */
    public static String CollectionName = "/q/java/qn";

    /**
     * The DB ref.
     */
    private DB db;

    /**
     * Inject the DB ref.
     * @return this
     */
    public ReferenceNameQuery inject(DB db) {
        this.db = db;
        return this;
    }

    /**
     * Query the DB for the given reference name and return the results.
     *
     * @return the results.
     */
    public List<Map> query(String referenceName) {
        
        return (!StringUtils.isEmpty(referenceName)) 
                ? db.find( ReferenceNameQuery.CollectionName, new MapBuilder().append("_qn", referenceName) )
                : new ArrayList<Map>();
    }
    
    /**
     * Query the DB for the given reference name and return only one of the results.
     * 
     * If any of the results match the input libraryId, then the first that matches is returned.
     * 
     * Otherwise the first result is returned.
     *
     * @return the results.
     */
    public Map queryOne(String referenceName, String libraryId) {
        
        List<Map> results = query(referenceName);
        
        List<Map> sameLibrary = LibraryUtils.filterOnLibrary( results, libraryId );
        
        if (sameLibrary.size() > 0) {
            return sameLibrary.get(0);
        } else if (results.size() > 0) {
            return results.get(0);
        } else {
            return null;
        }
    }
    
    /**
     * Lookup the superclass for the given doc in the db.  This is done by
     * first using ReferenceNameQuery to lookup the superclass _id from its type name,
     * then looking up the _id in the db to get the full superclass document.
     * 
     * @return the superclass document for the given doc.
     */
    public Map lookupSuperclassDoc(Map doc) {
        
        Map superClassStub = (Map) doc.get("superclass");
        
        Map superclassRef =  queryOne( JavadocMapUtils.getQualifiedName(superClassStub),
                                       JavadocMapUtils.getLibraryId( doc ) );

        return db.read( JavadocMapUtils.getId(superclassRef) );
    }
    
    /**
     * Build the index from scratch.
     * 
     * NOTE: This will fully delete the existing index.
     */
    public ReferenceNameQuery buildIndex() {

        Log.info( this, "build: building the referenceName index" );
        
        db.drop( ReferenceNameQuery.CollectionName );
        
        db.forAll( (Collection<String>) Cawls.pluck( db.getLibraryList("java"), "_id"), new IndexBuilder() );
        
        ensureIndexedColumns();
        
        return this;
    }

    /**
     * Create the index on the _qn field, if one doesn't already exist.
     */
    protected void ensureIndexedColumns() {
        db.createIndex( ReferenceNameQuery.CollectionName , new MapBuilder().append( "_qn", 1 )
                                                                            .append("_id", -1) );
    }
    
    /**
     * Add the documents from the given libraries to the index.
     */
    public ReferenceNameQuery addLibrariesToIndex( List<String> libraryIds ) {

        db.forAll( libraryIds , new IndexBuilder() );

        ensureIndexedColumns();
        
        return this;
    }

    /**
     * Add the documents from the given library to the index.
     */
    public ReferenceNameQuery addLibraryToIndex( String libraryId ) {

        db.forAll( Arrays.asList(libraryId) , new IndexBuilder() );

        ensureIndexedColumns();
        
        return this;
    }

    /**
     * Builds the index.
     */
    protected static class IndexBuilder implements DB.ForAll {

        /**
         *
         */
        @Override
        public void call(DB db, String collection, Map doc) {
            db.save( ReferenceNameQuery.CollectionName , buildDoc( doc ) );
        }

        /**
         *
         */
        protected Map buildDoc( Map doc) {
            Map retMe = Cawls.pick( doc, Arrays.asList( "_id", 
                                                        "name", 
                                                        "qualifiedName", 
                                                        "flatSignature", 
                                                        JavadocMapUtils.LibraryFieldName ) );

            retMe.put( "_qn", getReferenceName(doc) );

            return retMe;
        }

        /**
         *
         */
        protected String getReferenceName( Map doc ) {

            switch ( JavadocMapUtils.getMetaType(doc) ) {
                case "package":
                    return (String) doc.get("name");
                case "method":
                case "constructor":
                case "field":
                case "enumConstant":
                case "annotationTypeElement":
                    return (String)((Map)doc.get("containingClass")).get("qualifiedName") + "+" + doc.get("name");
                default:
                    return (String) doc.get("qualifiedName");
            }
        }
    }
}


