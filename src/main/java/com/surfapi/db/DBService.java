package com.surfapi.db;

import java.io.File;

/**
 *
 */
public class DBService {
    
    /**
     * In need of Dependency injection.
     */
    private static DB staticInstance;
    
    /**
     * The dir containing all the raw data files.
     */
    public static File dataDir = new File( System.getProperty("DATA_DIR", "data") );

    /**
     * @return the DB instance.  first call will load the DB from the HFS.
     */
    public static DB getDb() {
        if (staticInstance == null) {
            DB db = new DBImpl();
            new DBLoader().inject( db ).loadUnchecked(dataDir);  // TODO: not thread safe i don't think (even with synchronized)
            setDb(db);
        }
        return staticInstance;
    }
    
    /**
     * Inject the given DB ref into the staticInstance.
     */
    public static synchronized DB setDb(DB db) {
        DBService.staticInstance = db;
        return DBService.staticInstance;
    }
    
}