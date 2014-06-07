
package com.surfapi.junit;

import java.io.IOException;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import com.surfapi.db.MongoDBImpl;

/**
 * @Rule class that drops the specified mongo database before and after the test is run. 
 * 
 */
public class DropMongoDBRule implements TestRule {

    /**
     * A reference to the MongoDBProcessRule that manages the mongodb process.
     */
    private MongoDBProcessRule mongoDBProcessRule;
    
    /**
     * 
     */
    private String dbName;
    
    /**
     * CTOR.
     */
    public DropMongoDBRule(MongoDBProcessRule mongoDBProcessRule, String dbName ) {
        this.mongoDBProcessRule = mongoDBProcessRule;
        this.dbName = dbName;
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
        return new DropMongoStatement( base );
    }

    /**
     * Statement class - performs the before/after operations around a 
     * call to the base Statement's evaulate() method (which runs the test).
     */
    protected class DropMongoStatement extends Statement {

        /**
         * A reference to the Statement that MongoDBProcessStatement wraps around.
         */
        private final Statement base;

        /**
         * CTOR.
         *
         * @param base The Statement that MyStatement wraps around.
         */
        public DropMongoStatement(Statement base) {
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
         * Reset the mongoDB db before the test.
         */
        protected void before() throws IOException {
            if ( mongoDBProcessRule.isStarted() ) {
                new MongoDBImpl(dbName).drop();
            }
        }

        /**
         * Reset the mongoDB db after the test.
         */
        protected void after() throws IOException {
            if ( mongoDBProcessRule.isStarted() ) {
                new MongoDBImpl(dbName).drop();
            }
        }
    }

}
 


