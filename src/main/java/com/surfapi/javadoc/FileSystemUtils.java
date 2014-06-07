package com.surfapi.javadoc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.IOFileFilter;

public class FileSystemUtils {

    /**
     * Find all *.java files in the given baseDir
     *
     * @return The Collection of *.java file names.
     */
    public static List<String> listJavaFileNames(File baseDir) throws IOException {
        List<String> fileNames = new ArrayList<String>();
        for (File file : FileUtils.listFiles(baseDir, new String[] { "java"}, true)) {
            fileNames.add( file.getCanonicalPath() );
        }
        return fileNames;
    }
    
    /**
     * 
     * @return Map of dirName -> Collection(fileNames).
     */
    public static Map<File, List<File>> listJavaFilesByDir(File baseDir, IOFileFilter dirFilter) throws IOException {
        
        Map<File, List<File>> retMe = new HashMap<File, List<File>>();
        
        // If the baseDir doesn't pass the filter, then return an empty list.
        // (Note: FileUtils.listFilesAndDirs doesn't apply the filter to the base dir).
        if (!dirFilter.accept(baseDir)) {
            return retMe;
        }
        
        for (File dir : FileUtils.listFilesAndDirs(baseDir, DirectoryFileFilter.INSTANCE, dirFilter) ) {
            for (File file : FileUtils.listFiles(dir, new String[] { "java"}, false)) {
                
                List<File> files = retMe.get( dir );
                if (files == null) {
                    retMe.put( dir, new ArrayList<File>() );
                    files = retMe.get( dir );
                }
                files.add( file );
            }
        }
        
        return retMe;
    }
    
    /**
     * 
     * @return The names of the given collection of Files.
     */
    public static List<String> mapToFileNames(Collection<File> files) throws IOException {
        List<String> fileNames  = new ArrayList<String>();
        for (File file : files) {
            fileNames.add(file.getCanonicalPath());
        }
        return fileNames;
    }
    
    /**
     * 
     * @return A chunked list of *.java files under the given baseDir, chunked by dir.
     */
    public static List<List<String>> chunkJavaFileNamesByDir(File baseDir, IOFileFilter dirFilter) throws IOException {
        List<List<String>> retMe = new ArrayList<List<String>>();
        for (List<File> chunk : listJavaFilesByDir(baseDir, dirFilter).values()) {
            if (!chunk.isEmpty()) {
                retMe.add( mapToFileNames(chunk) );
            }
        }
        return retMe;
    }
}
