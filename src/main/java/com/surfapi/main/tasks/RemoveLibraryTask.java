package com.surfapi.main.tasks;

import com.surfapi.coll.MapBuilder;
import com.surfapi.db.DB;
import com.surfapi.db.post.CustomIndex;
import com.surfapi.main.Task;
import com.surfapi.main.TaskArgs;

/**
 * Utility task.  Removes a library from the db.
 * 
 * Removing includes:
 * 1. removing the library's collection
 * 2. removing the library from the 'libraries' collection
 * 3. removing the library from all indexes
 * 
 */
public class RemoveLibraryTask extends Task<RemoveLibraryTask> {

    /**
     *
     */
    @Override
    public String getTaskName() {
        return "removeLibrary";
    }

    /**
     *
     */
    @Override
    public String getTaskHelp() {
        return getTaskName() + " [ --libraryId=[libraryId] ]";
    }

    /**
     *
     */
    @Override
    public String getTaskDescription() {
        return "Remove a library from the database";
    }

    /**
     * 
     */
    @Override
    public int handleTask(String[] args) throws Exception {
        
        TaskArgs taskArgs = new TaskArgs(args);

        String libraryId = taskArgs.getRequiredStringValue("--libraryId");

        // Remove from all indexes
        for (CustomIndex customIndex : CustomIndex.getAllIndexes() ) {
            customIndex.inject(getDb()).removeLibrary(libraryId);
        }
        
        // Remove from libraries list
        getDb().remove( DB.LibraryCollectionName, new MapBuilder().append( "_id", libraryId) );
        
        // Remove the library itself
        getDb().drop( libraryId );

        return 0;
    }


}
