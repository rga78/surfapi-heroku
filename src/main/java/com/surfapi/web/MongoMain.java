package com.surfapi.web;

import org.apache.commons.lang3.StringUtils;

import com.surfapi.db.DBService;
import com.surfapi.db.MongoDBService;

/**
 * Sets Mongo as the DBService provider, then calls ServerMain to start the server.
 * 
 * Note: -Dcom.surfapi.mongo.db.name must be set.
 * 
 */
public class MongoMain {

    /**
     * Wrap ServerMain().  Sets mongo as the db provider.
     * 
     */
    public static void main(String[] args) throws Exception {
        
        verifyEnv();
        
        DBService.setDb( MongoDBService.getDb() );
        
        ServerMain.main(args);
    }
    
    /**
     * Verify the mongo db name has been configured in the env.
     */
    protected static void verifyEnv() {
        if (StringUtils.isEmpty( System.getProperty("com.surfapi.mongo.db.name") ) ) {
            throw new RuntimeException("System property 'com.surfapi.mongo.db.name' must be set");
        }
    }
    
   
}
