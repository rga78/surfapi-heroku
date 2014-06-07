package com.surfapi.javadoc;

import java.io.File;

import org.apache.commons.io.filefilter.IOFileFilter;

/**
 * filter out file names that have the "test" dir anywhere
 * in their path.
 */
class FilterOutTest implements IOFileFilter {

    @Override
    public boolean accept(File file) {
        for (File parentFile = file; parentFile != null; parentFile = parentFile.getParentFile()) {
            if (parentFile.getName().equals("test")) {
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