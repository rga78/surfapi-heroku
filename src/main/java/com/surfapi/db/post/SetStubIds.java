
package com.surfapi.db.post;

import java.util.List;
import java.util.Map;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.coll.Cawls;
import com.surfapi.db.DB;



/**
 * Iterate thru the DB and set the _id field for all javadoc element stubs.
 *
 * TODO: search across libraries for stub _id's.  Currently it only sets 
 *      _ids for stubs that reference a document from the same library.
 *
 * TODO: perhaps instead there should be an index of qualifiedNames. The front-end
 *       can link to a lookup of the type's qualifiedName against the index and
 *       return a list of all matches in a little pop-up.  This would eliminate
 *       the need for SetStubIds on most if not all fields.  If the qualifiedName
 *       exists in the same library, automatically jump to that one.
 *       The problem with that is several other javadoc post-processors depend
 *       on SetStubIds.  They would have to be changed to use the qualifiedNames index as well.
 *
 */
public class SetStubIds implements DB.ForAll {

    /**
     *
     */
    @Override
    public void call(DB db, String collectionName, Map doc) {
        db.save( collectionName, setStubIds(db, collectionName, doc) );
    }

    /**
     * Add _id fields into all the stubs in the given document.
     */
    @SuppressWarnings("rawtypes")
    protected Map setStubIds(DB db, String libraryId, Map doc) {
        
        return setStubIdsForSameLibrary(libraryId, doc);
    }
    
    /**
     * 
     */
    public Map setStubIdsForSameLibrary( String libraryId, Map doc) {

        setStubIdsHelper( libraryId, (List) doc.get("methods") );
        setStubIdsHelper( libraryId, (List) doc.get("fields") );
        setStubIdsHelper( libraryId, (List) doc.get("enumConstants") );
        setStubIdsHelper( libraryId, (List) doc.get("constructors") );
        setStubIdHelper( libraryId, (Map) doc.get("containingPackage") );
        setStubIdHelper( libraryId, (Map) doc.get("containingClass") );
        setStubIdsHelper( libraryId, (List) doc.get("innerClasses") );
        
        // Package-specific stuff
        if (JavadocMapUtils.isPackage(doc)) {
            setStubIdsHelper( libraryId, (List) doc.get("ordinaryClasses") );
            setStubIdsHelper( libraryId, (List) doc.get("interfaces") );
            setStubIdsHelper( libraryId, (List) doc.get("exceptions") );
            setStubIdsHelper( libraryId, (List) doc.get("enums") );
            setStubIdsHelper( libraryId, (List) doc.get("errors") );
            setStubIdsHelper( libraryId, (List) doc.get("annotationTypes") );
        }
        
        return doc;
    }

    /**
     * Set Ids into the list of stubs by combining the stub's relativeId with
     * the given libraryId.
     * 
     * Note: the stub objects are modified in place.
     */
    protected List setStubIdsHelper(String libraryId, List<Map> stubs) {
        
        for (Map stub : Cawls.safeIterable(stubs)) {
            setStubIdHelper(libraryId, stub);
        }
        
        return stubs;
    }

    /**
     * Set the _id into the given stub by combining the stub's relativeId with
     * the given libraryId.
     * 
     * Note: the stub object is modified in place.
     */
    protected Map setStubIdHelper(String libraryId, Map stub) {
        if (stub != null) {
            // stub.put("_id", new JavadocObject(libraryId, stub).getId()); 
            stub.put("_id", JavadocMapUtils.buildId(libraryId, stub));
        }
        return stub;
    }
    
    /**
     * Set Ids into the list of stubs by combining the stub's relativeId with
     * the given libraryId -- IF AND ONLY IF the stub actually exists in this library.
     * 
     * Note: the stub objects are modified in place.
     */
    protected List setStubIdsIfSameLibrary(DB db, String libraryId, List<Map> stubs) {
        
        for (Map stub : Cawls.safeIterable(stubs)) {
            setStubIdIfSameLibrary(db, libraryId, stub);
        }
        
        return stubs;
    }

    /**
     * Set the _id into the given stub by combining the stub's relativeId with
     * the given libraryId -- IF AND ONLY IF the stub actually exists in this library.
     * 
     * Note: the stub object is modified in place.
     */
    protected Map setStubIdIfSameLibrary(DB db, String libraryId, Map stub) {
        if (stub != null ) {
            String _id = JavadocMapUtils.buildId(libraryId, stub); // new DocObject(libraryId, stub).getId();
            if (db.read(libraryId, _id) != null) {
                stub.put("_id", _id); 
            }
            // TODO: find the library associated with the given stub in order to set the _id.
            //       Could either maintain a mapping of relativeIds -> libraryIds, or
            //       could just search every collection for the id.
            // TODO: refs to foreign libraries that are hardened in the DB would get stale
            //       as newer versions of the foreign library are added.  Perhaps instead
            //       of storing the full id, just store the relative, and handle the relativeID
            //       lookup on the REST call.
            //       OR... periodically run the post-processor to update stale links.
            //       (remember - the post-processor is idempotent, can run it over and over
            //       and still end up with the same results (well, except for maybe an updated
            //       version of a foreign-library _id).
        }
        return stub;
    }

}



