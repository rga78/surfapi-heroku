
package com.surfapi.db.post;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.app.LibraryUtils;
import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.BulkWriter;
import com.surfapi.db.DB;
import com.surfapi.db.MongoDBImpl;
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
public class ReferenceNameQuery extends CustomIndex<ReferenceNameQuery> {

    /**
     *
     */
    public static String CollectionName = "/q/java/qn";
    
    protected String getCollectionName() {
        return CollectionName;
    }

    /**
     * For each doc, replace the "_id" field with the "id" field.
     * (Why? Because a javadoc model may exist in the index multiple times under difference refereceNames.
     *  So the "_id" of that model was cached in the "id" field so as not to collide in the index (_id is primary key)).
     *  
     * @return docs, with all "id" fields set into the "_id" field
     */
    protected List<Map> replaceIds(List<Map> docs) {
        
        for (Map doc : docs) {
            doc.put("_id", doc.get("id"));
        }
        
        return docs;
    }
    
    /**
     * Query the DB for the given reference name and return the results.
     *
     * @return the results.
     */
    public List<Map> query(String referenceName) {
        
        return (!StringUtils.isEmpty(referenceName)) 
                ? replaceIds( getDb().find( ReferenceNameQuery.CollectionName, new MapBuilder().append("_qn", referenceName) ) )
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
     * TODO: I believe this is unused at the moment.
     * 
     * @return the superclass document for the given doc.
     */
    public Map lookupSuperclassDoc(Map doc) {
        
        Map superClassStub = (Map) doc.get("superclass");
        
        Map superclassRef =  queryOne( JavadocMapUtils.getQualifiedName(superClassStub),
                                       JavadocMapUtils.getLibraryId( doc ) );

        return getDb().read( JavadocMapUtils.getId(superclassRef) );
    }
    
    /**
     * Build the index from scratch.
     * 
     * NOTE: This will fully delete the existing index.
     */
    public ReferenceNameQuery buildIndex() {

        Log.info( this, "build: building the referenceName index" );
        
        getDb().drop( getCollectionName() );
        
        getDb().forAll( (Collection<String>) Cawls.pluck( getDb().getLibraryList("java"), "_id"), new IndexBuilder() );
        
        ensureIndexedColumns();
        
        return this;
    }

    /**
     * Create the index on the _qn field, if one doesn't already exist.
     */
    protected void ensureIndexedColumns() {
        getDb().createIndex( getCollectionName() , new MapBuilder().append( "_qn", 1 )
                                                                                 .append("_id", -1) );
    }
    
    /**
     * Add the documents from the given library to the index.
     */
    public ReferenceNameQuery addLibraryToIndex( String libraryId ) {

        getDb().forAll( Arrays.asList(libraryId) , new IndexBuilder() );

        ensureIndexedColumns();
        
        return this;
    }
    
    /**
     * Remove the given library from the index.  
     * 
     * That is, remove all entries associated with this library.
     */
    public ReferenceNameQuery removeLibrary( String libraryId ) {

        Log.info( this, "removeLibrary: " + libraryId);
             
        getDb().remove( getCollectionName(), new MapBuilder<String, String>()
                                                      .append( JavadocMapUtils.LibraryFieldName + "._id", libraryId) );
        
        return this;
    }

    /**
     * Builds the index.
     */
    protected class IndexBuilder implements DB.ForAll {

        private BulkWriter bulkWriter;
        
        @Override
        public void before(DB db, String collection) {
            bulkWriter = new BulkWriter( (MongoDBImpl) getDb(), getCollectionName());
                                    // .setWriteConcern( WriteConcern.UNACKNOWLEDGED );
        }
        
        /**
         *
         */
        @Override
        public void call(DB db, String collection, Map doc) {
            for (Map refDoc : buildReferenceDocs(doc)) {
                // db.save( getCollectionName() , refDoc );
                bulkWriter.insert( refDoc );
            }
        }
        
        /**
         * 
         */
        @Override 
        public void after(DB db, String collection) {
            bulkWriter.flush();
        }
        
        /**
         * 
         */
        protected List<Map> buildReferenceDocs(Map doc) {
            List<Map> retMe = new ArrayList<Map>();
            
            for (String referenceName : getReferenceNames(doc)) {
                retMe.add( buildReferenceDoc(doc, referenceName) );
            }
            
            return retMe;
        }

        /**
         * @return A document for the ReferenceNameQuery collection/index.  It's a subset of
         *         the given document along with the given referenceName 
         */
        protected Map buildReferenceDoc( Map doc, String referenceName) {
            Map retMe = Cawls.pick( doc, Arrays.asList( "name", 
                                                        "qualifiedName", 
                                                        "flatSignature", 
                                                        JavadocMapUtils.LibraryFieldName ) );
            
            retMe.put("_id", buildReferenceDocId(doc, referenceName)) ;
            retMe.put("id", JavadocMapUtils.getId(doc) );
            
            retMe.put( "_qn", referenceName );  // Field still named "_qn" from when it was called "qualifiedName" 

            return retMe;
        }
        
        /**
         * Returns an _id value for the ReferenceNameQuery document.  The _id cannot
         * be doc._id because doc may exist multiple times in the index under different
         * referenceNames.  So instead use concatenate doc._id with the referenceName
         * to ensure a unique entry, and to also avoid inserting dups if the same doc
         * is added to the ReferenceNameQuery more than once.
         * 
         * @return doc._id + "/" + referenceName
         */
        protected String buildReferenceDocId(Map doc, String referenceName) {
            return JavadocMapUtils.getId(doc) + "/" + referenceName;
        }
        
        /**
         * @return the reference name(s) for the given doc.
         *         For packages, it's the package name. 
         *         For classes, it's the qualifiedName
         *         For methods, it's the class qualifiedName "+" the method name (the + is to discern CTORS).
         */
        protected Set<String> getReferenceNames( Map doc ) {
            
            Set<String> retMe = new HashSet<String>();

            switch ( JavadocMapUtils.getMetaType(doc) ) {

                case "field":
                case "enumConstant":
                case "annotationTypeElement":
                    retMe.add( JavadocMapUtils.getQualifiedName((Map)doc.get("containingClass")) + "+" + doc.get("name") );
                    break;
                    
                // Methods can be referenced by name alone, or by name with fully-qualified signature,
                // or by name with non-qualified signature.
                case "method":
                case "constructor":
                    retMe.add( JavadocMapUtils.getQualifiedName((Map)doc.get("containingClass")) + "+" + doc.get("name") );
                    
                    retMe.add( JavadocMapUtils.getQualifiedName((Map)doc.get("containingClass")) 
                               + "+" 
                               + doc.get("name")
                               + JavadocMapUtils.getQualifiedParameterSignature(doc) );
                    
                    retMe.add( JavadocMapUtils.getQualifiedName((Map)doc.get("containingClass")) 
                            + "+" 
                            + doc.get("name")
                            + JavadocMapUtils.getNonQualifiedParameterSignature(doc) );

                    break;
                default:
                    retMe.add( JavadocMapUtils.getQualifiedName( doc ) );
            }
            
            return retMe;
        }
    }
}


