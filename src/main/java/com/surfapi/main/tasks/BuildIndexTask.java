package com.surfapi.main.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.surfapi.db.DB;
import com.surfapi.db.MongoDBService;
import com.surfapi.db.post.AllKnownImplementorsQuery;
import com.surfapi.db.post.AllKnownSubclassesQuery;
import com.surfapi.db.post.AutoCompleteIndex;
import com.surfapi.db.post.CustomIndex;
import com.surfapi.db.post.ReferenceNameQuery;
import com.surfapi.main.ArgMap;
import com.surfapi.main.Task;

/**
 *
 */
public class BuildIndexTask implements Task {

    /**
     *
     */
    @Override
    public String getTaskName() {
        return "buildIndex";
    }

    /**
     *
     */
    @Override
    public String getTaskHelp() {
        return "buildIndex [ --index=[indexName] --libraryId=[libraryId] ]";
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
        
        ArgMap argMap = new ArgMap(args);
        
        DB db = MongoDBService.getDb();

        String libraryId = argMap.getStringValue("--libraryId");

        for (CustomIndex customIndex : getIndexes(argMap) ) {

            customIndex.inject(db);

            if ( StringUtils.isEmpty(libraryId) ) {
                customIndex.buildIndex();
            } else {
                customIndex.addLibrariesToIndex(Arrays.asList(libraryId) );
            }
        }

        return 0;
    }

    /**
     * @return the list of CustomIndexes to build.
     */
    protected List<CustomIndex> getIndexes(ArgMap argMap) {

        List<CustomIndex> retMe = new ArrayList<CustomIndex>();

        String index = argMap.getStringValue("--index");

        if ( StringUtils.isEmpty(index) ) {

            retMe.add( new AutoCompleteIndex() );
            retMe.add( new ReferenceNameQuery() );
            retMe.add( new AllKnownSubclassesQuery() );
            retMe.add( new AllKnownImplementorsQuery() );

        } else if (index.equals("AutoCompleteIndex")) {
            retMe.add( new AutoCompleteIndex() );

        } else if (index.equals("ReferenceNameQuery")) {
            retMe.add( new ReferenceNameQuery() );

        } else if (index.equals("AllKnownSubclassesQuery")) {
            retMe.add( new AllKnownSubclassesQuery() );

        } else if (index.equals("AllKnownImplementorsQuery")) {
            retMe.add( new AllKnownImplementorsQuery() );
        }
        
        return retMe;
    }

}
