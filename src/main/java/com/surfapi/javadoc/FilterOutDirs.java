package com.surfapi.javadoc;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.io.filefilter.IOFileFilter;

/**
 * Filter out files that are nested under any of the specified dirs.
 * 
 */
class FilterOutDirs implements IOFileFilter {
    
    /**
     * The list of dir names to filter out.  Any files nested at any depth
     * under a dir whose name is in this list are filtered out.
     */
    Collection<String> dirs;
    
    /**
     * 
     */
    public FilterOutDirs(String... dirs) {
        this.dirs = (dirs == null || dirs.length == 0) 
                        ? Arrays.asList("test", "internal", "example", "examples")
                        : Arrays.asList(dirs);
    }

    @Override
    public boolean accept(File file) {
        for (File parentFile = file; parentFile != null; parentFile = parentFile.getParentFile()) {
            if (dirs.contains( parentFile.getName() ) ) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean accept(File dir, String fileName) {
        return accept(dir);
    }
}