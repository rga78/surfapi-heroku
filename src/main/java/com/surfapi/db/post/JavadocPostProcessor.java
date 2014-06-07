package com.surfapi.db.post;

import java.util.Collection;

import com.surfapi.db.DB;
import com.surfapi.log.Log;


/**
 * This class runs against javadoc produced by the javadoc-json doclet in order
 * to do some post-processing on the data, such as:
 *  1. add IDs to all stub elements
 *  2. evaluate javadoc tags like @link, @code, @rootDoc, etc.
 *  3. compile inherited methods and fields
 *  4. determine "known implentations" of interfaces, "known subclasses" of classes, etc.
 *
 */
public class JavadocPostProcessor {

    /**
     * Injected reference to DB.
     */
    private DB db;
    
    /**
     * 
     * @return this
     */
    public JavadocPostProcessor inject(DB db) {
        this.db = db;
        return this;
    }
    
    /**
     *
     * Run all javadoc post-processors against all java libraries in the DB.
     */
    public JavadocPostProcessor postProcess() {
        return postProcess( db.getLibraryIds("java") );
    }

    /**
     *
     * Run all javadoc post-processors against the given java libraries.
     */
    public JavadocPostProcessor postProcess(Collection<String> libraryIds ) {

        Log.info(this, "postProcess: SetStubIds");
        db.forAll( libraryIds, new SetStubIds()) ;

        Log.info(this, "postProcess: CollectSuperClasses");
        db.forAll( libraryIds, new CollectSuperClasses() );

        Log.info(this, "postProcess: CollectInheritedMembers");
        db.forAll( libraryIds, new CollectInheritedMembers()) ;

        Log.info(this, "postProcess: LinkOverriddenMethods");
        db.forAll( libraryIds, new LinkOverriddenMethods() );

        return this;
    }


}
