package com.surfapi.javadoc;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.surfapi.log.Log;

/**
 * Main entry point for:
 * 
 * 1. extracting a src jar
 * 2. generating JSON javadoc
 * 
 */
public class ExtractMain {
    
    /**
     * 
     */
    private static void usage(String err) {
        if (!StringUtils.isEmpty(err)) {
            Log.error(err);
        }
        
        Log.info("Usage: ExtractMain <libraryId> <src-jar-file>");
        
        System.exit(-1);
    }
    
    /**
     * @param src-jar-file The jar file containing the src code to parse for javadoc.
     */
    public static void main(String[] args) throws Exception {
        
        if (args.length < 2) {
            usage(null);
        }
        
        File jarFile = new File(args[1]);
        if (! jarFile.exists()) {
            usage("src jar file " + jarFile.getAbsolutePath() + " does not exist");
        }
        
        // returns the directory in which the jar was extracted.
        // JavadocProcess will process all *.java files under that dir.
        File extractDir = new ExtractSrcJar(jarFile).extract();

        // TODO: Read the .surfapi config file for the list of dirs to process, if one exists.
        // If no config file exists, process the baseDir.
        
        JavadocMain.main(new String[] { args[0], extractDir.getCanonicalPath() });
        
        // Delete the directory recursively
        FileUtils.deleteDirectory( extractDir );
    }

}
