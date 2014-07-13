package com.surfapi.javadoc;

import com.surfapi.log.Log;
import com.surfapi.main.MainTaskDispatcher;
import com.surfapi.main.tasks.BuildIndexTask;
import com.surfapi.main.tasks.HelpTask;

/**
 * Main entry point for building indexes and doing other "post-insertion" operations
 * against a newly-added library (or against all libraries, which effectively refreshes
 * all indexes)
 *
 */
public class PostProcessorMain {

    /**
     * TODO:
     * usage: PostProcessorMain --buildIndex=all | --buildIndex=[indexName]
     * 
     * Build indexes and run "post-insertion" processes against the javadoc.
     *
     * Note: requires MongoDBService to be configured (-Dcom.surfapi.mongo.db.name)
     */
    public static void main(String[] args) throws Exception{

        Log.info(new PostProcessorMain(), "main: entry");
        
        MainTaskDispatcher dispatcher = new MainTaskDispatcher();
        
        // Register task handlers
        dispatcher.registerTask(new BuildIndexTask());
        dispatcher.registerTask(new HelpTask(dispatcher.getTaskList()));
               
        // Process the command
        System.exit( dispatcher.runProgram(args) );
    }

}
