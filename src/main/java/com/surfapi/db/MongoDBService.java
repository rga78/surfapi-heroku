package com.surfapi.db;

/**
 * Wrapper around static singleton MongoDBImpl instance.
 */
public class MongoDBService {

    /**
     * In need of Dependency injection.
     */
    private static MongoDBImpl staticInstance;

    /**
     * @return the DB instance. 
     */
    public static DB getDb() {
        if (staticInstance == null) {
            setDb( new MongoDBImpl( getDbName() ) );
        }
        return staticInstance;
    }
    
    /**
     * Inject the given DB ref into the staticInstance.
     */
    public static synchronized MongoDBImpl setDb(MongoDBImpl db) {
        staticInstance = db;

        DBService.setDb( staticInstance );

        return staticInstance;
    }
    
    /**
     * @return the mongo db name (pulled from the config)
     */
    public static String getDbName() {
        return System.getProperty("com.surfapi.mongo.db.name", "test");
    }
    
    /**
     * Set the given db name into the system property.
     */
    public static void setDbName(String dbName) {
        System.setProperty("com.surfapi.mongo.db.name", dbName);
    }
}
