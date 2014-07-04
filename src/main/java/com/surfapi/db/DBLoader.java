package com.surfapi.db;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.post.AutoCompleteIndex;
import com.surfapi.db.post.JavadocPostProcessor;
import com.surfapi.db.post.ReferenceNameQuery;
import com.surfapi.log.Log;

/**
 * Load data from *.json files.
 *
 * Files can be loaded individually or from a directory.
 * 
 * The *.json files are assumed to have a name in the format: {@code <libraryName>_<libraryVersion>.json}.
 * 
 * Each library is loaded into its own collection.  The name of the collection is the libraryId.
 * The libraryId is of the form {@code "/java/<libraryName>/<libraryVersion"}.
 *
 * Each document within the library is fitted with a few extra fields if they're not already present,
 * e.g. _id and _library.
 * 
 * The DBLoader instance is injected with the DB reference via {@link #inject(DB)};
 */
public class DBLoader {
    
    /**
     * Ref to the DB.
     */
    private DB db;
    
    /**
     * Inject DB.
     * 
     * @return this
     */
    public DBLoader inject(DB db) {
        this.db = db;
        return this;
    }
    
    /**
     * Load from the data dir.
     * 
     * @return this
     */
    public DBLoader loadUnchecked(File file ) {
        try {
            if (file.isDirectory()) {
                return loadDir( file );
            } else {
                return loadFile( file );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load DB", e);
        }
    }
    
    /**
     * @return this
     */
    public DBLoader loadDir( File dataDir ) throws IOException, ParseException {
        
        Collection<File> jsonFileList = FileUtils.listFiles(dataDir, new String[] { "json"}, true);
        
        for (File jsonFile : jsonFileList) {
            loadFile( jsonFile );
        }
        
        return this;
    }
    
    /**
     * 
     */
    public DBLoader loadFile( File jsonFile ) throws IOException, ParseException {
        Log.info(this, "loadFile: " + jsonFile.getAbsolutePath() );
        
        String libraryId = parseLibraryId( jsonFile.getName() );
        
        popDB( libraryId, loadJsonFile( jsonFile ) );

        
        return this;
    }
    
    /**
     * The given fileName must be in the format {@literal "<libraryName>_<libraryVersion>.json"}.
     *
     * If the {@literal "_<libraryVersion>"} part is missing, version 0 is applied.
     *
     * @param fileName
     *
     * @return a libraryId for the given file name. with _ replaced with /
     *
     * @throws IllegalArgumentException if the filename is not in the proper format.
     */
    protected static String parseLibraryId( String fileName ) {

        if (!fileName.contains("_")) {
            return "/java/" + StringUtils.stripEnd( fileName, ".json" ) + "/0";

        } else if (fileName.indexOf("_") == fileName.lastIndexOf("_")) {
            // Only 1 "_" allowed!
            return "/java/" + StringUtils.stripEnd( fileName, ".json" ).replace("_","/") ;

        } else {
            throw new IllegalArgumentException("Improperly named file: " + fileName 
                                               + ". Must be in the form: '<libraryName>_<libraryVersion>.json'");
        }
    }
    
    /**
     * 
     */
    protected JSONArray loadJsonFile( File jsonFile ) throws ParseException, IOException {
        return(JSONArray) new JSONParser().parse( new FileReader( jsonFile ) );
    }
    
    /**
     * Add the given list of documents for the given libraryID to the DB.
     *
     * This method adds a few fields to each document, e.g _id and _library,
     * if they're not already present.
     *
     * Note: this method also addes a library overview document to the 
     * {@link DB#LibraryCollectionName} collection.
     */
    public DBLoader popDB( String libraryId, List<Map> docs ) {
        
        // Add all the json doc data
        for (Map doc : docs ) {
            db.save( libraryId, buildDocument( libraryId, doc ));
        }
        
        // Create an overview for the library (e.g. a package list for a java library).
        db.save(DB.LibraryCollectionName, createLibraryOverview(libraryId));
        
        return this;
    }
    
    /**
     * Add required internal fields to the given doc (before inserting it into the db).
     * 
     * _id
     * _library
     * 
     * TODO: stub ids? yes can do this if we are *sure* the stub ref is in the same library.
     *       This is true for methods, fields, enumConstants, and many other fields.
     *       See SetStubIds.
     *       
     * @return 
     */
    protected Map buildDocument( String libraryId, Map doc ) {
        
        doc.put("_library", JavadocMapUtils.mapLibraryId(libraryId)); 
        doc.put("_id", JavadocMapUtils.buildId(libraryId, doc));
        
        return doc; 
    }

    /**
     * @return an overview for the given library (e.g. a package list for a java library).
     */
    public Map createLibraryOverview(String libraryId) {
        
        List<Map> packageStubs = new ArrayList<Map>();
 
        for (Map pkg : db.find(libraryId, new MapBuilder<String, String>().append("metaType", "package") )) {
            packageStubs.add( Cawls.pick(pkg, Arrays.asList("_id", "name", "metaType", "firstSentenceTags") ));
        }
        
        // Add a library record to the libraries collection
        Map retMe =  JavadocMapUtils.mapLibraryId(libraryId);
        retMe.put("packages", packageStubs);
        retMe.put("metaType", "library");
        return retMe;
    }
    
  

    /**
     * TODO: not used.
     * 
     * Starting with a json file (produced by javadoc-json-doclet):
     *  1. create a new library (collection) for the json file 
     *  2. populate the new library with all documents in the json file
     *  3. run the JavadocPostProcessor against the new library
     *  4. run the IndexPostProcessor against the new library
     */
    public DBLoader doTheWholeShebang( File jsonFile ) throws IOException, ParseException {

        String libraryId = parseLibraryId( jsonFile.getName() );

        loadFile( jsonFile );
        
        new AutoCompleteIndex().inject(db).addLibraryToIndex( libraryId );
        
        new ReferenceNameQuery().inject(db).addLibraryToIndex( libraryId );

        new JavadocPostProcessor().inject(db).postProcess(Arrays.asList( libraryId ));
        
        return this;
    }
    
}
