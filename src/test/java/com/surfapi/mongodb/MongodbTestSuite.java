package com.surfapi.mongodb;

import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Suite of tests that use mongodb.
 */
@RunWith(Suite.class)
@SuiteClasses({
                MongoDBProcessTest.class 
              })
public class MongodbTestSuite {

    private static MongoDBProcess mongodbProcess;
    
    /**
     * Start up mongodb.
     */
    @BeforeClass
    public static void setUp() throws IOException {
        if ( Boolean.getBoolean("runMongo") ) {
            mongodbProcess = MongoDBProcess.start();
        }
    }

    /**
     * shut down mongodb.
     */
    @AfterClass
    public static void tearDown() throws IOException {
        if ( mongodbProcess != null ) {
            mongodbProcess.stop();
        }
    }

}
