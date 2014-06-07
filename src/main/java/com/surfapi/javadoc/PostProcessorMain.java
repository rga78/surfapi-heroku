package com.surfapi.javadoc;

import java.util.Arrays;

import com.surfapi.db.DB;
import com.surfapi.db.MongoDBService;
import com.surfapi.db.post.AutoCompleteIndex;
import com.surfapi.db.post.JavadocPostProcessor;
import com.surfapi.db.post.ReferenceNameQuery;
import com.surfapi.log.Log;

/**
 * Main entry point for building indexes and doing other "post-insertion" operations
 * against a newly-added library (or against all libraries, which effectively refreshes
 * all indexes)
 *
 */
public class PostProcessorMain {

    /**
     * 
     * Build indexes and run "post-insertion" processes against the javadoc.
     *
     * Note: requires MongoDBService to be configured (-Dcom.surfapi.mongo.db.name)
     */
    public static void main(String[] args) throws Exception{

        DB db = MongoDBService.getDb();

        Log.info(new PostProcessorMain(), "main: entry");

        if (args.length == 0) {
        
            new AutoCompleteIndex().inject(db).buildIndexForLang( "java" );
        
            new ReferenceNameQuery().inject(db).buildIndex();

            new JavadocPostProcessor().inject(db).postProcess();

        } else {

            new AutoCompleteIndex().inject(db).addLibrariesToIndex( Arrays.asList(args) );
        
            new ReferenceNameQuery().inject(db).addLibrariesToIndex( Arrays.asList( args) );

            new JavadocPostProcessor().inject(db).postProcess(Arrays.asList( args ));
        }

        Log.info(new PostProcessorMain(), "main: exit");
    }
}
