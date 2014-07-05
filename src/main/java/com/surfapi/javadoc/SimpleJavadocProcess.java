package com.surfapi.javadoc;


import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import com.surfapi.log.Log;
import com.surfapi.proc.ProcessException;
import com.surfapi.proc.ProcessHelper;

/**
 * Runs javadoc using our custom MongoDoclet.
 *
 * javadoc \
 *      -doclet com.surfapi.javadoc.MongoDoclet \
 *      -docletpath "target/classes;$dp" \
 *      -J-Xms1024m \
 *      -J-Xmx4096m \
 *      -J-DMONGOLAB_URI=$MONGOLAB_URI \
 *      -J-Dcom.surfapi.mongo.library.id=$MONGO_LIBRARYID \
 *      -sourcepath /fox/tmp/javadoc/src-jdk7   \
 *      [ -subpackages javax  | <package-list> ]
 *
 */
public class SimpleJavadocProcess {

    /**
     * The base package dir.  This dir plus all subdirs are supplied as packages to
     * the javadoc command.
     */
    private File sourcePath;
    
    /**
     * The mongo URI
     */
    private String mongoUri;

    /**
     * The libraryId (mongodb collection name).
     */
    private String libraryId;
    
    /**
     * List of -subpackages args (all subpackages beneath the arg are processed).
     */
    private List<String> subpackages = new ArrayList<String>();

    /**
     * List of packages to process.
     */
    private List<String> packages = new ArrayList<String>();

    /**
     * @return the classpath (-docletpath) for the custom doclet.
     */
    protected String getDocletPath() throws IOException {
        return buildMavenDocletPath();
    }
    
    /**
     * Build a doclet path that contains the classes from this project along
     * with all dependency jars.
     * 
     * @return the classpath (-docletpath) for the custom doclet.
     */
    public static String buildMavenDocletPath() throws IOException {
        
        File dependencyDir = new File("./target/dependency");
        Collection<File> jarFiles = FileUtils.listFiles(dependencyDir, new String[] { "jar"}, false);
        List<String> jarFileNames = FileSystemUtils.mapToFileNames(jarFiles);
        
        String jarFileClassPath = StringUtils.join(jarFileNames, File.pathSeparator);
        
        return "./target/classes" 
                + File.pathSeparator
                + jarFileClassPath;
    }
    
    /**
     * @return this
     */
    public SimpleJavadocProcess setMongoUri(String mongoUri) {
        this.mongoUri = mongoUri;
        return this;
    }
    
    /**
     * @return the mongo uri
     */
    public String getMongoUri() {
        return mongoUri;
    }
    
    /**
     * @return this
     */
    public SimpleJavadocProcess setLibraryId(String libraryId) {
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
     * @return this
     */
    public SimpleJavadocProcess setSourcePath(File sourcePath) {
        this.sourcePath = sourcePath;
        return this;
    }
    
    /**
     * @return the sourcepath
     */
    public File getSourcePath() {
        return sourcePath;
    }

    /**
     * @return this
     */
    public SimpleJavadocProcess setSubpackages(List<String> subpackages) {
        this.subpackages.addAll( subpackages );
        return this;
    }

    /**
     * @return the list of -subpackages
     */
    public List<String> getSubpackages() {
        return subpackages;
    }

    /**
     * @return this
     */
    public SimpleJavadocProcess setPackages(List<String> packages) {
        this.packages.addAll( packages );
        return this;
    }

    /**
     * @return the list of packages
     */
    public List<String> getPackages() {
        return packages;
    }

    
    /**
     * Build Runnable work for spawning and waiting for the javadoc process.
     * 
     * @return A Runnable that will spawn and wait for the javadoc process.
     */
    protected Runnable buildJavadocProcessRunnable( ) {
        
        return new Runnable() {
            public void run() {
                
                try {
                    
                    String processDescription = "javadoc against sourcepath: " +  getSourcePath().getCanonicalPath();
                    Log.info(this, "run: " + processDescription);
                    
                    ProcessHelper processHelper = new ProcessHelper( buildJavadocProcess() )
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
     * Run the javadoc command.
     */
    public void run() throws IOException, InterruptedException, ExecutionException {
        buildJavadocProcessRunnable().run();
    }

    /**
     *
     * @param javaFileNames The list of java files to process
     * 
     * @return the javadoc Process
     */
    protected Process buildJavadocProcess() throws IOException {
        return new ProcessBuilder( buildCommand() ).start();
    }
    
    /**
     * @return the javadoc command
     */
    protected List<String> buildCommand() throws IOException {

        List<String> command = new ArrayList<String>();

        command.addAll( Arrays.asList( new String[] { "javadoc", 
                                                      "-docletpath",
                                                      getDocletPath(),
                                                      "-doclet",
                                                      MongoDoclet.class.getCanonicalName(),
                                                      "-J-Xms1024m",
                                                      "-J-Xmx4096m",
                                                      "-J-DMONGOLAB_URI=" + getMongoUri(),
                                                      "-J-Dcom.surfapi.mongo.library.id=" + getLibraryId(),
                                                      "-sourcepath",
                                                      getSourcePath().getCanonicalPath()
                                                    } ) );

        command.addAll( buildSubpackagesCommandArgs() );
        command.addAll( getPackages() );
        
        return command;
    }
    

    /**
     * @return -subpackages <subpkg1> -subpackages <subpkg2> ...
     */
    protected List<String> buildSubpackagesCommandArgs() {
        List<String> retMe = new ArrayList<String>();

        for (String subpackage : getSubpackages() ) {
            retMe.add( "-subpackages" );
            retMe.add( subpackage );
        }

        return retMe;
    }

}


