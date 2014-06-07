
package com.surfapi.junit;

import java.io.IOException;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.surfapi.mongodb.MongoDBProcess;

/**
 * @Rule class that starts/stops a MongoDBProcess around each test or test class, 
 * depending on whether it's marked with @Rule or @ClassRule.
 */
public class MongoDBProcessRule implements TestRule {

    /**
     * A reference to the actual MongoDBProcess.
     */
    private MongoDBProcess mongodbProcess;

    /**
     * @return true if the mongodb process is running.
     */
    public boolean isStarted() {
        return mongodbProcess != null;
    }

    /**
     * @param base Represents the test to be run. Typically you'd wrap this 
     *             Statement with a new Statement that performs the before/after
     *             operations around a call to base.evaluate(), which executes
     *             the test.
     * @param description This can be used to obtain @annotation data from the
     *             test method.
     *
     * @return A Statement to be evaluated() by the test runner.
     */
    @Override
    public Statement apply( Statement base, Description description ) {
        return new MongoDBProcessStatement( base );
    }

    /**
     * Statement class - performs the before/after operations around a 
     * call to the base Statement's evaulate() method (which runs the test).
     */
    protected class MongoDBProcessStatement extends Statement {

        /**
         * A reference to the Statement that MongoDBProcessStatement wraps around.
         */
        private final Statement base;

        /**
         * CTOR.
         *
         * @param base The Statement that MyStatement wraps around.
         */
        public MongoDBProcessStatement(Statement base) {
            this.base = base;
        }

        /**
         * This method is called by the test runner in order to execute the test.
         *
         * Before/After logic is embedded here around a call to base.evaluate(),
         * which processes the Statement chain (for any other @Rules that have been
         * applied) until at last the text method is executed.
         *
         */
        @Override
        public void evaluate() throws Throwable {

            before();

            try {
                base.evaluate();
            } finally {
                after();
            }
        }

        /**
         * Start up the MongoDBProcess, so long as runMongo=true is set in the System properties.
         *
         * The code detects if the mongodb process is already active and won't start up a new
         * one in that case (it also won't destroy the process after the test completes).
         */
        protected void before() throws IOException {
            if ( Boolean.getBoolean("runMongo") ) {
                mongodbProcess = MongoDBProcess.start();
            }
        }

        /**
         * Stop the mongodb process - but only if we're the ones who started it.  If it was
         * already started then we won't stop it.
         */
        protected void after() throws IOException {
            if ( mongodbProcess != null ) {
                mongodbProcess.stop();
                mongodbProcess = null;
            }
        }
    }

}
 


