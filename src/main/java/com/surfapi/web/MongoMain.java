package com.surfapi.web;

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
        
        DBService.setDb( MongoDBService.getDb() );
        
        ServerMain.main(args);
    }

}
