package com.surfapi.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.junit.CaptureSystemOutRule;
import com.surfapi.log.Log;

/**
 * 
 */
public class FileSystemUtilsTest {

    /**
     * Capture and suppress stdout unless the test fails.
     */
    @Rule
    public CaptureSystemOutRule systemOutRule  = new CaptureSystemOutRule( );
    
    /**
     * 
     */
    @Test
    public void testListJavaFilesByDir() throws Exception {
        
        File baseDir = new File("src/test/java/");
        
        Map<File, List<File>> fileMap = FileSystemUtils.listJavaFilesByDir(baseDir, TrueFileFilter.INSTANCE);
        
        assertFalse(fileMap.isEmpty());
        
        for (Map.Entry<File, List<File>> entry : fileMap.entrySet()) {
            for (File file : entry.getValue()) {
                // Log.log(this, "testListFiles: " + entry.getKey().getName() + ": " + file.getName());
                assertEquals( entry.getKey(), file.getParentFile());
            }
        }
    }
    
    /**
     * 
     */
    @Test
    public void testListJavaFilesByDirFilterOutTest() throws Exception {
        
        File baseDir = new File("src/test/java/");
        
        Map<File, List<File>> fileMap = FileSystemUtils.listJavaFilesByDir(baseDir, new FilterOutDirs());
        
        assertTrue(fileMap.isEmpty());
        
        baseDir = new File("src/test/java/com/surfapi/test");
        
        fileMap = FileSystemUtils.listJavaFilesByDir(baseDir, new FilterOutDirs());
        
        for (Map.Entry<File, List<File>> entry : fileMap.entrySet()) {
            for (File file : entry.getValue()) {
                Log.trace(this, "testListJavaFilesByDirFilterOutTest: " + entry.getKey().getName() + ": " + file.getName());
                // assertEquals( entry.getKey(), file.getParentFile());
            }
        }
        
        assertTrue(fileMap.isEmpty());
    }
    
    /**
     * 
     */
    @Test
    public void testChunkFileNamesByDir() throws Exception {
        
        File baseDir = new File("src/test/java/");
        List<List<String>> chunks = FileSystemUtils.chunkJavaFileNamesByDir(baseDir, TrueFileFilter.INSTANCE);
        
        assertFalse( chunks.isEmpty() );
        
        for (List<String> fileNames : chunks) {
            assertFalse( fileNames.isEmpty() );
            File parentDir = new File(fileNames.get(0)).getParentFile();
            for (String fileName : fileNames) {
                // Log.log(this, "testListFiles: " + entry.getKey().getName() + ": " + file.getName());
                assertEquals( parentDir, new File(fileName).getParentFile());
            }
        }
    }

}
