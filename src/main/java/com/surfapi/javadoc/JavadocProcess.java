package com.surfapi.javadoc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.surfapi.log.Log;
import com.surfapi.proc.ProcessException;
import com.surfapi.proc.ProcessHelper;

/**
 * Runs javadoc against all *.java files in the given baseDir, using our custom MyDoclet.
 *
 * Note: this class writes multiple JSON objects to the stream, separated by commas, similar
 * to array notation except WITHOUT the encapsulating array braces '[' and ']'.  It's up
 * to the caller to write those to the given OutputStream before and after calling this guy.
 *
 * This allows the caller to run JavadocProcess against multiple baseDirs and write all the
 * output to a single file.
 */
public class JavadocProcess {

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
     * 
     * @param baseDir - The base package dir. This points to the directory of your base package.
     *                  E.g if your package is "com.abc.foo", the baseDir is "/path/to/com".
     */
	public JavadocProcess(File baseDir) {
		this.baseDir = baseDir;
	}

    /**
     * Inject the docletPath to use for this process.
     * 
     * @return this
     */
    public JavadocProcess setDocletPath(String docletPath) {
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
     * Spawn off a bunch of javadoc processes, one for each directory (package).
     * 
     * Data for each process is written to the corresponding file
     *
     * @return a list of Pair<ProcessHelper, File>.
     */
    protected List<Pair<ProcessHelper, File>> forkJavadocProcesses(File baseDir) throws IOException {

        List<Pair<ProcessHelper, File>> retMe = new ArrayList<Pair<ProcessHelper, File>>();
	    
        // Chunk java file names by package (directory).
        // This is necessary because of the problem where the packageDoc will only contain the
        // classes/exceptions/interfaces/etc that are included in the javadoc invocation.
	    for ( List<String> javaFileNames : FileSystemUtils.chunkJavaFileNamesByDir(baseDir, getDirFilter()) ) {
	    
            String processDescription = "javadoc against directory: " +  new File(javaFileNames.get(0)).getParentFile().getCanonicalPath();
	        Log.info(this, "run: " + processDescription);
	        
	        File tempOutputFile = File.createTempFile( "javadoc." + UUID.randomUUID().toString(), ".json");
	        tempOutputFile.deleteOnExit();
	        
	        retMe.add( new ImmutablePair<ProcessHelper, File>( 
                                        new ProcessHelper( buildJavadocProcess(javaFileNames) )
                                                .setDescription(processDescription)
	                                            .pipeTo(ProcessHelper.Stream.STDOUT, new FileOutputStream(tempOutputFile))
                                                .spawnStreamReaders(),
                                        tempOutputFile ) );
	    }

        return retMe;
    }

	/**
	 * 
	 * @return a Pair containing stdout and stderr from the javadoc process.
	 */
	public Pair<List<String>,List<String>> run() throws IOException, InterruptedException, ExecutionException {

        List<Pair<ProcessHelper, File>> processHelpers = forkJavadocProcesses(baseDir);
	    
        return collectOutput(processHelpers);
	}
	
	/**
	 * TODO: collect stderr.
	 * 
	 * @return a Pair containing stdout and stderr from the given javadoc process.
	 */
	protected Pair<List<String>,List<String>> collectOutput(List<Pair<ProcessHelper, File>> processHelpers) 
	        throws InterruptedException, IOException {
	    
        // Collect all the output from the forked javadoc processes
        List<String> stdout = new ArrayList<String>();

        // Now wait for all processes to end and collect their output.
        for (Pair<ProcessHelper,File> pair : processHelpers) {

            ProcessHelper processHelper = pair.getLeft();
	        processHelper.waitFor();

	        if (processHelper.exitValue() != 0) {
                Log.error(this, "run: " + new ProcessException( processHelper ));
	        } else {
                stdout.addAll( removeWarnings( IOUtils.readLines( new FileReader(pair.getRight()) ) ) );
            }
        }

        return new ImmutablePair<List<String>, List<String>>( stdout, null);
	}
	
	
	/**
     * Pipe all the output from the forked javadoc processes to the given output stream.
     *
     */
    protected void pipeOutput(List<Pair<ProcessHelper, File>> processHelpers, 
                              OutputStream outputStream) throws InterruptedException, IOException {

        // Now wait for all processes to end and pipe their output.
        for (Pair<ProcessHelper,File> pair : processHelpers) {

            ProcessHelper processHelper = pair.getLeft();
            processHelper.waitFor();

            if (processHelper.exitValue() != 0) {
                Log.error(this, "run: " + new ProcessException( processHelper ));
            } else {
                IOUtils.writeLines( removeWarnings( IOUtils.readLines( new FileReader(pair.getRight()) ) ),
                                    "\n",
                                    outputStream,
                                    "ISO-8859-1");
            }
            
        }

    }
    
	
	/**
     * 
     * @param outputStream Output is written to this stream.
     * 
     * @return a Pair containing stdout and stderr from the javadoc process.
     */
    public void run(OutputStream outputStream) throws IOException, InterruptedException, ExecutionException {

        pipeOutput( forkJavadocProcesses(baseDir), outputStream);
    }
    
    /**
     * 
     */
	protected List<String> removeWarnings(List<String> list) {
	    
	    Pattern patter = Pattern.compile("^\\d+\\s+warnings?$");
	    
	    List<String> retMe = new ArrayList<String>();
	    for (String s : list) {
	        if (!patter.matcher(s).matches()) {
	            retMe.add(s);
	        }
	    }
       
        return retMe;
    }

    /**
     * Why not use com.sun.tools.javadoc.Main?  
     * 
     * Per http://docs.oracle.com/javase/7/docs/technotes/guides/javadoc/standard-doclet.html:
     * 
     * "The disadvantages of calling main are: (1) It can only be called once per run -- for 1.2.x or 1.3.x, 
     * use java.lang.Runtime.exec("javadoc ...") if more than one call is needed, (2) it exits using System.exit(), 
     * which exits the entire program, and (3) an exit code is not returned."
     * 
     * TODO: maybe it's still useful for getting the classpath right?  I could package a separate jar for
     *       it, with all its dependencies baked in, and invoke using java -jar instead of javadoc.
     *
     * @param javaFileNames The list of java files to process
     * 
     * @return the javadoc Process
     */
    protected Process buildJavadocProcess(List<String> javaFileNames) throws IOException {
        return new ProcessBuilder( buildCommand2(javaFileNames) ).start();
    }
    
    /**
     * 
     * @param javaFileNames The list of java files to process
     * 
     * @return the javadoc command, with java file names as args  
     *         Assume baseDir points at a directory that contains java files somewhere underneath it.
     */
    protected List<String> buildCommand2( List<String> javaFileNames ) throws IOException {

        List<String> command = new ArrayList<String>();

        command.addAll( Arrays.asList( new String[] { "javadoc", 
                                              "-docletpath",
                                              getDocletPath(),
                                              "-doclet",
                                              JsonDoclet.class.getCanonicalName(),
                                              "-quiet" } ) );

        command.addAll( javaFileNames );
        
        return command;
    }

    /**
     * @param subdirFilter 
     * @return this
     */
    protected JavadocProcess setDirFilter(IOFileFilter subdirFilter) {
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
