package com.surfapi.mongodb;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.surfapi.log.Log;
import com.surfapi.proc.ProcessHelper;
import com.surfapi.proc.StreamPiper;

/**
 * Handles starting/stopping a mongodb process for testing purposes.
 *
 * NOTE:  This code was originally written for windows (not mac).
 */
public class MongoDBProcess extends ProcessHelper<MongoDBProcess> implements Observer {

    /**
     * Running locally on a mac.
     * This code was originally written for windows.
     */
    private static boolean IsMac = System.getProperty("os.name").startsWith("Mac");

    /**
     * Singleton reference to the actual active mongod process.
     */
    private static MongoDBProcess singleton;
    
    /**
     * Semaphore used to communicate between the outputObserver and the main thread
     * that the mongodb server has completed initialization.
     * 
     * Note: the 0 means a release must occur first before any acquires will work.
     */
    private Semaphore mongoDbStarted = new Semaphore(0);
    
    /**
     * CTOR
     * 
     * @param process The already-started mongod process.
     */
    public MongoDBProcess(Process process) {
        super(process);
        addObserver(this);
        addObserver(new StreamPiper(System.out));
    }

    /**
     * Static method starts the mongodb process (if it hasn't already been started) 
     * and returns a MongodbProcess instance.
     * 
     * The first caller will actually start up the mongodb process and get back a
     * MongodbProcess handle that can stop it.
     * 
     * Subsequent callers will use the already-started mongodb process and get back
     * a dummy MongodbProcess handle that can NOT stop the process.
     * 
     * @return a MongodbProcess wrapped around the real process.
     */
    public static MongoDBProcess start() throws IOException {
        
        if (singleton == null && !MongoDBProcess.isStarted()) {
            // TODO: check mongod.lock file, try to delete.  if we can, means the process wasn't running (hopefully).
            //       if we can't it means the process is running and has the file locked.  Then what?  How to stop?
            //       don't bother stopping?  
            Log.info("MongodbProcess: start: starting mongodb process...");
            
            singleton = buildProcess().waitForStart();
            
            Log.info("MongodbProcess: start: mongodb started.");
            
            return singleton;
            
        } else {
            // Subsequent callers get empty mongodb process, so that stop() doesn't stop the real one.
            Log.info("MongodbProcess: start: mongodb process already started.");
            
            return new MongoDBProcess(null);
        }
    }

    /**
     * Wait for the mongodb process to start up.
     * 
     * @return this
     */
    private MongoDBProcess waitForStart() throws IOException {
        try {
            // TODO: check for timeouts
            boolean acquired = mongoDbStarted.tryAcquire(60, TimeUnit.SECONDS);
            return this;
        } catch (InterruptedException ie) {
            throw new IOException(ie);
        }
    }
    
    /**
     * Wait for the mongodb process to stop.
     * 
     * @return this
     */
    private MongoDBProcess waitForStop() throws IOException {
        try {
            Thread.sleep(5 * 1000);    // TODO: better way?
            deleteLockFile();
            return this;
        } catch (InterruptedException ie) {
            throw new IOException(ie);
        }
    }
    
    /**
     * NOTE: only works on windows.
     *
     * @return the mongodb process lock file.
     */
    private static File getLockFile() {
        return new File("C:\\data\\db\\mongod.lock");
    }
    
    /**
     * For some reason the lock file doesn't always get deleted.
     */
    private static void deleteLockFile() throws IOException {
        File lockFile = getLockFile();
        if (lockFile.exists()) {
            lockFile.delete();
        }
    }
    
    /**
     * Defers to isStartedWin() or isStartedMac()
     *
     * @return true if the mongodb process is already running.  
     */
    protected static boolean isStarted() {
        return (MongoDBProcess.IsMac) ? isStartedMac() : isStartedWin();
    }

    /**
     * Assumes mongodb is running.
     * TODO: detect mongodb is running.
     * @return true 
     */
    protected static boolean isStartedMac() {
        return true;
    }

    /**
     * @return true if the mongodb process is already running.  This is determined
     *         by looking for and trying to delete the mongod lock file.
     */
    protected static boolean isStartedWin() {
        File lockFile = getLockFile();
        return (lockFile.exists()) ?  !lockFile.delete() : false;
    }
    
    /**
     * Stop the mongodb process and dump its output.
     */
    public void stop() throws IOException {
        
        if (getProcess() != null) {
            
            try {
                Log.info(this, "stop: stopping mongodb process...");
                
                List<String> output = destroyAndWaitFor().getOutput();
                waitForStop();
                
                // Reset the singleton. I'm assuming here that this object is the singleton.
                // Setting it to null means the next guy to call MongodbProcess.start() will
                // start up a new one.
                singleton = null;
                
                // We're already logging the output via the TailObserver.
                // Log.log(this, "stop: mongod output:\n" + StringUtils.join( output, "\n" ) );
                
            } catch (Exception e) {
                throw new IOException(e);
            }
        } else {
            Log.info(this, "stop: using pre-existing mongodb process.  Will not stop");
        }
    }
    
    /**
     * NOTE: only works on windows.
     */
    protected static MongoDBProcess buildProcess() throws IOException {
        Process process = new ProcessBuilder( Arrays.asList( "C:\\mongodb\\bin\\mongod.exe" ) ).start() ;
        return (MongoDBProcess) new MongoDBProcess( process ).spawnStreamReaders();
    }

    /**
     * Observer interface.
     * 
     * The mongodbprocess observes itself...
     * 
     * Looks for the message: "waiting for connections on port 27017" to indicate 
     * the DB is up and running.
     */
    @Override
    public void update(Observable o, Object line) {
        if ( ((String)line).contains("waiting for connections on port 27017") ) {
            Log.info(this, "update: mongodb init complete detected");
            mongoDbStarted.release();
        }
    }
}
