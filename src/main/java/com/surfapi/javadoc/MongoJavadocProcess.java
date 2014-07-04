package com.surfapi.javadoc;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
    private IOFileFilter subdirFilter = new FilterOutDirs();
    
    /**
     * The name of the mongo db.
     */
    private String mongoDBName;
    
    /**
     * The libraryId (mongodb collection name).
     */
    private String libraryId;
    
    /**
     * The executor service is used to throttle the number of active javadoc processes
     * spawned by this class.  Some java libraries are huge (e.g the sdk) and will end
     * up spawning hundreds of processes that may overwhelm the system if not throttled.
     * The executor will effectively throttle the number of active javadoc processes
     * to the number of threads the executor contains.  Each thread spawns a javadoc
     * process and waits for that process to end before returning to the pool.
     */
    private ExecutorService executorService ;
    
    
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
        
        executorService = Executors.newFixedThreadPool(10);

        // Chunk java file names by package (directory).
        // This is necessary because of the problem where the packageDoc will only contain the
        // classes/exceptions/interfaces/etc that are included in the javadoc invocation.
        for ( List<String> javaFileNames : FileSystemUtils.chunkJavaFileNamesByDir(baseDir, getDirFilter()) ) {
            
            executorService.submit( buildJavadocProcessRunnable(javaFileNames) );
        }
        
        Log.info(this, "forkJavadocProcesses: all javadoc work submitted. Shutting down executor and awaiting termination...");
        executorService.shutdown();
        executorService.awaitTermination(1,TimeUnit.HOURS);
        Log.info(this, "forkJavadocProcesses: all javadoc processes completed");
       
    }
    
    /**
     * Build Runnable work for spawning and waiting for the javadoc process.
     * 
     * This work will be submitted to the executor.  The executor has a fixed-size 
     * thread pool which effectively throttles the number of active javadoc processes 
     * so as not to overwhelm the system.
     * 
     * @return A Runnable that will spawn and wait for the javadoc process.
     */
    protected Runnable buildJavadocProcessRunnable( final List<String> javaFileNames ) {
        
        return new Runnable() {
            public void run() {
                
                try {
                    
                    String processDescription = "javadoc against directory: " +  new File(javaFileNames.get(0)).getParentFile().getCanonicalPath();
                    Log.info(this, "run: " + processDescription);
                    
                    ProcessHelper processHelper = new ProcessHelper( buildJavadocProcess(javaFileNames) )
                                                            .setDescription(processDescription)
                                                            .spawnStreamReaders()
                                                            .waitFor();

                    if (processHelper.exitValue() != 0) {
                        Log.error(this, "run: " + new ProcessException( processHelper ));
                    }
                    
                } catch (Exception e) {
                    Log.error(this, "run: " + e);
                }
            }
        };
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
    public MongoJavadocProcess setDirFilter(IOFileFilter subdirFilter) {
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


