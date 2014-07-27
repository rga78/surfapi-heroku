package com.surfapi.main.tasks;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

import com.surfapi.db.post.CustomIndex;
import com.surfapi.main.Task;
import com.surfapi.main.TaskArgs;

/**
 * Utility task.  Builds indexes from javadoc model data.
 * 
 * One or all indexes can be built, using the data from 1 or all javadoc libraries.
 */
public class BuildIndexTask extends Task<BuildIndexTask> {

    /**
     *
     */
    @Override
    public String getTaskName() {
        return "buildIndex";
    }

    /**
     * TODO: this should probably be getTaskUsage or something..
     */
    @Override
    public String getTaskHelp() {
        return getTaskName() + " [ --index=[indexName] --libraryId=[libraryId] ]";
    }

    /**
     *
     */
    @Override
    public String getTaskDescription() {
        return "Build one or all indexes for one or all libraries";
    }

    /**
     * 
     */
    @Override
    public int handleTask(String[] args) throws Exception {
        
        TaskArgs taskArgs = new TaskArgs(args);

        String libraryId = taskArgs.getStringValue("--libraryId");

        for (CustomIndex customIndex : CustomIndex.getIndexes(taskArgs.getStringValue("--index")) ) {

            customIndex.inject(getDb());

            if ( StringUtils.isEmpty(libraryId) ) {
                customIndex.buildIndex();
            } else {
                customIndex.addLibrariesToIndex(Arrays.asList(libraryId) );
            }
        }

        return 0;
    }


}
