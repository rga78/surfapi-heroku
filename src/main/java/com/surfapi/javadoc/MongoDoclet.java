package com.surfapi.javadoc;

import org.apache.commons.lang3.StringUtils;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.LanguageVersion;
import com.sun.javadoc.RootDoc;
import com.surfapi.db.DBLoader;
import com.surfapi.db.MongoDBService;

/**
 * Javadoc doclet.  Inserts javadoc into mongodb.
 */
public class MongoDoclet extends JsonDoclet {
    
    /**
     * For loading data into mongo.
     */
    private DBLoader dbLoader;
    
    /**
     * The libraryId (collection name in mongo) associated with the javadoc we're processing.
     */
    private String libraryId;
    
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
        
        dbLoader = new DBLoader().inject( MongoDBService.getDb() );
    }
    
    /**
     * 
     */
    protected boolean go() {
        
        for (ClassDoc classDoc : rootDoc.classes()) {
            dbLoader.popDB( getLibraryId(), processClass(classDoc));
        }
        
        dbLoader.popDB( getLibraryId(), processPackages( getPackageDocs() ));

        return true;
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


}

