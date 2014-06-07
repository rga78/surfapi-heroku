package com.surfapi.javadoc;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.surfapi.db.MongoDBService;
import com.surfapi.log.Log;

/**
 * Main class for generating JSON javadoc.
 * 
 * Usage: Main <output-file> <src-dir> ... 
 */
public class JavadocMain {
    
    /**
     * @param src-dir The dir containing the src code to parse for javadoc.
     */
    public static void main(String[] args) throws Exception {
        new JavadocMain()
            .consumeArgs(args)
            .setDocletPath( JavadocMain.buildDocletPath() )
            .go();
    }

    /**
     * Build a doclet path that contains the classes from this project along
     * with all dependency jars.
     * 
     * @return the classpath (-docletpath) for the custom doclet.
     */
    public static String buildDocletPath() throws IOException {
        
        File dependencyDir = new File("./target/dependency");
        Collection<File> jarFiles = FileUtils.listFiles(dependencyDir, new String[] { "jar"}, false);
        List<String> jarFileNames = FileSystemUtils.mapToFileNames(jarFiles);
        
        String jarFileClassPath = StringUtils.join(jarFileNames, File.pathSeparator);
        
        return "./target/classes" 
                + File.pathSeparator
                + jarFileClassPath;
    }
    
    /**
     * @return The jar file that this class is executing within
     */
    public static File getThisJarFile() {
        return new File(JavadocMain.class.getProtectionDomain()
                                        .getCodeSource()
                                        .getLocation()
                                        .getPath());
    }

    /**
     * The doclet path used by the JavadocProcess.  It's a variable field
     * because the path may be different between the real/test environments.
     */
    private String docletPath;
    
    /**
     * A filter for filtering OUT the subdirs that we DON'T want to process.
     * By default it filters out all subdirs under any directory named "test"
     */
    private IOFileFilter subdirFilter = new FilterOutTest();
    
    /**
     * The library being processed.  This is provided as the first arg.
     */
    private String libraryId;
    
    /**
     * The java source dirs to process.  These are provided by args 2..n.
     */
    private String[] srcDirNames;
    
    /**
     * @throws RuntimeException prints the given err, usage, and then throws
     */
    private void usage(String err) {
        if (!StringUtils.isEmpty(err)) {
            Log.error(err);
        }
        
        Log.info("Usage: JavadocMain [ --all ] <libraryId> <src-dir> ...");
        
        throw new RuntimeException("ERROR: usage. " + err);
    }


    /**
     * @throws RuntimeException if the args are not valid.
     */
    protected JavadocMain validateArgs(String[] args) {
        if (args.length < 2) {
            usage(null);
        } 
        return this;
    }
    
    /**
     * 
     */
    protected JavadocMain consumeArgs(String[] args) {
        validateArgs(args);
        
        if (args[0].equals("--all") ) {
            setDirFilter( TrueFileFilter.INSTANCE );
            args = ArrayUtils.remove(args, 0);
            validateArgs(args);
        }
        
        this.libraryId = args[0];
        this.srcDirNames = Arrays.copyOfRange( args, 1, args.length );
        
        return this;
    }

    /**
     *
     * @param dirs A list of dirs to process
     */
    protected void go() throws Exception {

        // Process each directory.
        for (String srcDirName : srcDirNames) {

            File srcDir = new File(srcDirName);
            if (! srcDir.exists() || !srcDir.isDirectory()) {
                Log.error(this, "go: src directory " + srcDir.getAbsolutePath() + " does not exist");

            } else {
                
                new MongoJavadocProcess(srcDir)
                                 .setDocletPath( JavadocMain.buildDocletPath() )
                                 .setDirFilter( subdirFilter )
                                 .setMongoDBName( MongoDBService.getDbName() )
                                 .setLibraryId(libraryId )
                                 .run();
            }
        }

    }

    /**
     *
     */
    public JavadocMain setDocletPath(String docletPath) {
        this.docletPath = docletPath;
        return this;
    }
    
    /**
     * 
     */
    public String getDocletPath() {
        return docletPath;
    }

    /**
     * @param subdirFilter 
     * @return this
     */
    protected JavadocMain setDirFilter(IOFileFilter subdirFilter) {
        this.subdirFilter = subdirFilter;
        return this;
    }

}
