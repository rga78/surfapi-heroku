package com.surfapi.javadoc;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.RootDoc;
import com.surfapi.app.JavadocMapUtils;
import com.surfapi.db.DB;
import com.surfapi.db.DBLoader;
import com.surfapi.db.MongoDBService;
import com.surfapi.db.post.SetStubIds;
import com.surfapi.log.Log;

/**
 * Javadoc doclet.  Inserts javadoc into mongodb.
 */
public class MongoDoclet extends JsonDoclet {
    
    /**
     * The libraryId (collection name in mongo) associated with the javadoc we're processing.
     */
    private String libraryId;
    
    /**
     * The libraryId mapped into an object.
     */
    private Map mappedLibrary;
    
    /**
     * Set of class names in this library.  Used for distinguishing between
     * classes in this library and classes from other libraries (e.g. the JDK).
     */
    private Set<String> libraryClassNames = new HashSet<String>();
    
    /**
     * Doclet entry point. Javadoc calls this method, passing in the
     * doc metadata.
     */
    public static boolean start(RootDoc root) {

        return new MongoDoclet(root).go();
    }
    
    /**
     * NOTE: Without this method present and returning LanguageVersion.JAVA_1_5,
     *       Javadoc will not process generics because it assumes LanguageVersion.JAVA_1_1
     * @return language version (hard coded to LanguageVersion.JAVA_1_5)
     */
    public static LanguageVersion languageVersion() {
        return LanguageVersion.JAVA_1_5;
    }

    /**
     * CTOR.
     */
    public MongoDoclet(RootDoc rootDoc) {
        super(rootDoc);
    }
    
    /**
     * The meat.
     */
    protected boolean go() {
        
        // Populate the class name set.
        for (ClassDoc classDoc : rootDoc.classes()) {
            libraryClassNames.add( classDoc.qualifiedName() );
        }
        
        // Process classes and add them to the db.
        for (ClassDoc classDoc : rootDoc.classes()) {
            Log.trace( this, "go: processing class: " + classDoc.qualifiedName() );
            safeSave( getLibraryId(), processClass(classDoc));
        }
        
        // Add all packages to the db.
        Log.trace( this, "go: processing packages...");
        safeSave( getLibraryId(), processPackages( getPackageDocs() ));
        
        // Create an "overview" or "summary" document for the library (contains package lists).
        safeSave(DB.LibraryCollectionName, Arrays.asList( createLibraryOverview() ) );

        return true;
    }
        
    /**
     * Wrap DB.save() with try-catch.
     */
    protected void safeSave(String libraryId, Collection<Map> javadocModels) {
        try {
            getDb().save( libraryId, javadocModels);
        } catch (Exception e) {
            Log.error(this, "safeSave: caught exception", e);
        }
    }
    
    /**
     * @return a library overview/summary document (containing package lists).
     */
    protected Map createLibraryOverview() {
        return new DBLoader().inject( getDb() ).createLibraryOverview( getLibraryId() );
    }
    
    /**
     * Process the given classDoc along with all its methods, constructors, fields, enumConstants, etc.
     * 
     * @return a list of javadoc models.
     */
    protected JSONArray processClass(ClassDoc classDoc) {
        JSONArray retMe = super.processClass(classDoc);
        
        // Set the _id fields for all stubs in the documents
        new SetStubIds().setStubIdsForSameLibrary( getLibraryId(), retMe, libraryClassNames);
        
        // Set the _id and _library fields for the documents.
        setIds(retMe);
        
        // Add to indexes for "all known subclasses" and "all known implementations"
       ///  new AllKnownSubclassesQuery().inject(MongoDBService.getDb()).insert( retMe );
        
      //   new AllKnownImplementationsQuery().inject(MongoDBService.getDb()).insert( retMe );
 
        return retMe;
    }
    
    /**
     * Set the _id and _library fields for all the given docs.
     * 
     * @return docs
     */
    protected JSONArray setIds(JSONArray docs) {

        for (Map doc : (List<Map>) docs) {
            doc.put("_library", getMappedLibrary() ); 
            doc.put("_id", JavadocMapUtils.buildId(getLibraryId(), doc));
        }
        
        return docs;
    }
    
    /**
     * Process the given set of packageDocs.  
     * 
     * @return a list of package models.
     */
    protected JSONArray processPackages( Collection<PackageDoc> packageDocs ) {
        
        JSONArray retMe = super.processPackages(packageDocs);

        new SetStubIds().setStubIdsForSameLibrary( getLibraryId(), retMe, libraryClassNames);
        
        // Set the _id and _library fields for the documents.
        setIds(retMe);
        
        return retMe;
    }

    
    /**
     * @return the libraryId, as read from the config
     */
    protected String getLibraryId() {
        
        if (StringUtils.isEmpty(libraryId)) {
            
            libraryId = System.getProperty("com.surfapi.mongo.library.id");
        
            if (StringUtils.isEmpty(libraryId)) {
                throw new IllegalArgumentException("Must specify system property com.surfapi.mongo.library.id");
            }
        }
        
        return libraryId;
    }
    
    /**
     * @return the libraryId, mapped out into an object.
     */
    protected Map getMappedLibrary() {
        if (mappedLibrary == null) {
            mappedLibrary = JavadocMapUtils.mapLibraryId( getLibraryId());
        }
        return mappedLibrary;
    }
    
    /**
     * 
     */
    protected DB getDb() {
        return MongoDBService.getDb();
    }
    


}

