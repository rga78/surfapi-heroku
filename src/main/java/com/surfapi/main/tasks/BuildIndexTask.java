package com.surfapi.main.tasks;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;

import com.surfapi.db.DB;
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
        Collection<CustomIndex<?>> customIndexes = injectAll( getDb(),
                                                              CustomIndex.getIndexes(taskArgs.getStringValue("--index")) );
        
        if (StringUtils.isEmpty(libraryId)) {
            buildFromScratch( customIndexes ) ;
        } else {
            addLibrary(libraryId, customIndexes );
        }

        return 0;
    }

    /**
     * 
     */
    protected void buildFromScratch( Collection<CustomIndex<?>> customIndexes ) {
        for (CustomIndex customIndex : customIndexes) {
            customIndex.buildIndex();
        }
    }
    
    /**
     * 
     */
    protected void addLibrary(String libraryId, Collection<CustomIndex<?>> customIndexes) {
        getDb().forAll( libraryId, CustomIndex.getBuilders(customIndexes)) ;
    }
    
    /**
     * 
     */
    protected Collection<CustomIndex<?>> injectAll(DB db, Collection<CustomIndex<?>> customIndexes) {
        for (CustomIndex customIndex : customIndexes) {
            customIndex.inject(getDb());
        }
        return customIndexes;
    }


}
