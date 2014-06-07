package com.surfapi.javadoc;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.filefilter.IOFileFilter;

import com.surfapi.log.Log;
import com.surfapi.proc.ProcessException;
import com.surfapi.proc.ProcessHelper;

/**
 * Runs javadoc using our custom MongoDoclet.
 *
 */

public class MongoJavadocProcess {

    /**
     * The base package dir.  This dir plus all subdirs are supplied as packages to
     * the javadoc command.
     */
    private File baseDir;
    
    /**
     * The -docletpath parm for the javadoc process.
     */
    private String docletPath;
    
    /**
     * A filter for filtering OUT the subdirs that we DON'T want to process.
     * By default it filters out all subdirs under any directory named "test"
     */
    private IOFileFilter subdirFilter = new FilterOutTest();
    
    /**
     * The name of the mongo db.
     */
    private String mongoDBName;
    
    /**
     * The libraryId (mongodb collection name).
     */
    private String libraryId;
    
    
    /**
     * 
     * @param baseDir - The base package dir. This points to the directory of your base package.
     *                  E.g if your package is "com.abc.foo", the baseDir is "/path/to/com".
     */
    public MongoJavadocProcess(File baseDir) {
        this.baseDir = baseDir;
    }

    /**
     * Inject the docletPath to use for this process.
     * 
     * @return this
     */
    public MongoJavadocProcess setDocletPath(String docletPath) {
        this.docletPath = docletPath;
        return this;
    }
    
    /**
     * @return the classpath (-docletpath) for the custom doclet.
     */
    protected String getDocletPath() {
        return docletPath;
    }
    
    /**
     * @return this
     */
    public MongoJavadocProcess setMongoDBName(String mongoDBName) {
        this.mongoDBName = mongoDBName;
        return this;
    }
    
    /**
     * @return the mongo db name.
     */
    public String getMongoDBName() {
        return mongoDBName;
    }
    
    /**
     * @return this
     */
    public MongoJavadocProcess setLibraryId(String libraryId) {
        this.libraryId = libraryId;
        return this;
    }
    
    /**
     * @return the library id.
     */
    public String getLibraryId() {
        return libraryId;
    }
    
    /**
     * Spawn off a bunch of javadoc processes, one for each directory (package).
     * 
     * Data for each process is written to mongodb.
     */
    protected void forkJavadocProcesses(File baseDir) throws IOException, InterruptedException {

        List<ProcessHelper> processHelpers = new ArrayList<ProcessHelper>();
        
        // Chunk java file names by package (directory).
        // This is necessary because of the problem where the packageDoc will only contain the
        // classes/exceptions/interfaces/etc that are included in the javadoc invocation.
        for ( List<String> javaFileNames : FileSystemUtils.chunkJavaFileNamesByDir(baseDir, getDirFilter()) ) {
        
            String processDescription = "javadoc against directory: " +  new File(javaFileNames.get(0)).getParentFile().getCanonicalPath();
            Log.info(this, "run: " + processDescription);
            
            processHelpers.add( new ProcessHelper( buildJavadocProcess(javaFileNames) )
                                                .setDescription(processDescription)
                                                .spawnStreamReaders() );
        }
        
        // Wait for all the processHelpers to finish.
        waitForAll(processHelpers);
    }
    
    /**
     * Wait for all the given processHelpers to finish.
     */
    protected void waitForAll(Collection<ProcessHelper> processHelpers) throws InterruptedException {
        for (ProcessHelper processHelper : processHelpers) {
            processHelper.waitFor();
            
            if (processHelper.exitValue() != 0) {
                Log.error(this, "waitForAll: " + new ProcessException( processHelper ));
            }
        }
    }

    /**
     * 
     */
    public void run() throws IOException, InterruptedException, ExecutionException {

        forkJavadocProcesses(baseDir);
    }

    /**
     *
     * @param javaFileNames The list of java files to process
     * 
     * @return the javadoc Process
     */
    protected Process buildJavadocProcess(List<String> javaFileNames) throws IOException {
        return new ProcessBuilder( buildCommand(javaFileNames) ).start();
    }
    
    /**
     * 
     * @param javaFileNames The list of java files to process
     * 
     * @return the javadoc command, with java file names as args  
     *         Assume baseDir points at a directory that contains java files somewhere underneath it.
     */
    protected List<String> buildCommand( List<String> javaFileNames ) throws IOException {

        List<String> command = new ArrayList<String>();

        // TODO: add -D properties for com.surfapi.mongo.db.name, com.surfapi.mongo.library.id
        command.addAll( Arrays.asList( new String[] { "javadoc", 
                                              "-docletpath",
                                              getDocletPath(),
                                              "-doclet",
                                              MongoDoclet.class.getCanonicalName(),
                                              "-J-Dcom.surfapi.mongo.db.name=" + getMongoDBName(),
                                              "-J-Dcom.surfapi.mongo.library.id=" + getLibraryId(),
                                              "-quiet" } ) );

        command.addAll( javaFileNames );
        
        return command;
    }

    /**
     * @param subdirFilter 
     * @return this
     */
    protected MongoJavadocProcess setDirFilter(IOFileFilter subdirFilter) {
        this.subdirFilter = subdirFilter;
        return this;
    }
    
    /**
     * 
     * @return the file filter to use to filter which dirs to scan for *.java files.
     */
    protected IOFileFilter getDirFilter() {
        return subdirFilter;
    }
    

}


