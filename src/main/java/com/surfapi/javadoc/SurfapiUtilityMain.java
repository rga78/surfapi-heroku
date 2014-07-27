package com.surfapi.javadoc;

import com.surfapi.db.DB;
import com.surfapi.db.MongoDBService;
import com.surfapi.log.Log;
import com.surfapi.main.MainTaskDispatcher;
import com.surfapi.main.tasks.BuildIndexTask;
import com.surfapi.main.tasks.DropTask;
import com.surfapi.main.tasks.HelpTask;
import com.surfapi.main.tasks.RemoveLibraryTask;

/**
 * Main entry point for utility functions.
 *
 */
public class SurfapiUtilityMain {

    /**
     * 
     * usage: SurfapiUtilityMain [action] [options]
     * 
     */
    public static void main(String[] args) throws Exception{

        Log.info(new SurfapiUtilityMain(), "main: entry");
        
        DB db = MongoDBService.getDb();
        
        MainTaskDispatcher dispatcher = new MainTaskDispatcher();
        
        // Register task handlers
        dispatcher.registerTask(new BuildIndexTask().inject(db));
        dispatcher.registerTask(new RemoveLibraryTask().inject(db));
        dispatcher.registerTask(new DropTask().inject(db));
        dispatcher.registerTask(new HelpTask(dispatcher.getTaskList()));
               
        // Process the command
        // -rx- System.exit( dispatcher.runProgram(args) );
        dispatcher.runProgram(args) ;
        
        Log.info(new SurfapiUtilityMain(), "main: exit");
    }

}
