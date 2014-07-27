package com.surfapi.main.tasks;

import com.surfapi.db.MongoDBService;
import com.surfapi.log.Log;
import com.surfapi.main.Task;
import com.surfapi.main.TaskArgs;

/**
 * Utility task for dropping a collection or an entire db.
 * 
 */
public class DropTask extends Task<DropTask> {

    @Override
    public String getTaskName() {
        return "drop";
    }

    @Override
    public String getTaskHelp() {
        return getTaskName() + " [ --collection=[collectionName] | --db ]";
    }

    @Override
    public String getTaskDescription() {
        return "Drop a collection or the entire db.";
    }

    @Override
    public int handleTask(String[] args) throws Exception {
        
        TaskArgs taskArgs = new TaskArgs(args);
        
        if (taskArgs.isSpecified("--db")) {

            getDb().drop();
            Log.info( this, "handleTask: Dropped DB. " + getDb().getName());
            
        } else {
            String collection = taskArgs.getRequiredStringValue("--collection");
            
            MongoDBService.getDb().drop( collection );
            Log.info(this, "handleTask: Dropped collection: " + collection + " from DB " + getDb().getName());
        }
        
        return 0;
    }

    
}
