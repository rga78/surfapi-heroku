
package com.surfapi.db.post;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.coll.Cawls;
import com.surfapi.db.DB;



/**
 * Iterate thru the DB and set the _id field for all javadoc element stubs.
 *
 * DONE: perhaps instead there should be an index of qualifiedNames. The front-end
 *       can link to a lookup of the type's qualifiedName against the index and
 *       return a list of all matches in a little pop-up.  This would eliminate
 *       the need for SetStubIds on most if not all fields.  If the qualifiedName
 *       exists in the same library, automatically jump to that one.
 *       The problem with that is several other javadoc post-processors depend
 *       on SetStubIds.  They would have to be changed to use the qualifiedNames index as well.
 *
 * TODO: Post-process cross-library setStubIds still useful for:
 *          overriddenMethod (not guaranteed)
 *          inheritedMethods
 *          
 */
public class SetStubIds implements DB.ForAll {

    
    /**
     * TODO: this currently isn't called. is it needed?
     */
    @Override
    public void call(DB db, String collectionName, Map doc) {
        
         db.save( collectionName, setStubIdsCrossLibrary(db, collectionName, doc) );
    }

    /**
     *  TODO: this currently isn't called. is it needed?
     *  
     * Add _id fields to cross-library stubs, e.g. overriddenMethod and allInheritedMethods.
     */
    @SuppressWarnings("rawtypes")
    protected Map setStubIdsCrossLibrary(DB db, String libraryId, Map doc) {
        
        if (JavadocMapUtils.isClass(doc)) {
            setStubIdsCrossLibraryForClass(db, libraryId, doc);
        }
        
        return setStubIdsForSameLibrary(libraryId, doc);
    }
    
    /**
     *  TODO: this currently isn't called. is it needed?
     *  
     * Set stub IDs for stubs in the given class that may reference javadoc models from
     * other libraries.  
     * 
     * @return doc
     */
    protected Map setStubIdsCrossLibraryForClass(DB db, String libraryId, Map doc) {

        for ( Map inherited : Cawls.safeIterable( (List<Map>) doc.get("allInheritedMethods") ) ) {
            
            Map superclassStub = (Map) inherited.get("superclassType");
            
            if ( superclassStub.get("_id") == null) {
                // If the _id isn't set yet, then this stub must not be in the same library.
                // Do a refernece name lookup to find the javadoc model.
                Map lookupSuperclassStub = new ReferenceNameQuery().queryOne( JavadocMapUtils.getQualifiedName( superclassStub ), libraryId );
                
                if (lookupSuperclassStub != null) {
                    // Found it.  Set its ID into the stub (and all inherited method stubs)
                    superclassStub.put("_id", JavadocMapUtils.getId(lookupSuperclassStub));
                    String crossLibraryId = JavadocMapUtils.getLibraryId(lookupSuperclassStub);
                    
                    for (Map inheritedMethodStub : (List<Map>)inherited.get("inheritedMethods")) {
                        inheritedMethodStub.put("_id", JavadocMapUtils.buildId(crossLibraryId, inheritedMethodStub ));
                    }
                }
            }
        }
        
        return doc;
    }
    
    
    /**
     * Called by MongoDoclet (not by PostProcessor).
     * 
     * @return javadocModels
     */
    public List<Map> setStubIdsForSameLibrary( String libraryId, List<Map> javadocModels, Set<String> libraryClassNames) {
        for (Map javadocModel : javadocModels) {
            setStubIdsForSameLibrary(libraryId, javadocModel, libraryClassNames);
        }
        return javadocModels;
    }
    
    /**
     * Called by MongoDoclet (not by PostProcessor).
     * 
     * @return javadocModel
     */
    public Map setStubIdsForSameLibrary( String libraryId, Map javadocModel, Set<String> libraryClassNames) {
        
        setStubIdsForSameLibrary(libraryId, javadocModel);
        
        if (JavadocMapUtils.isClass(javadocModel)) {
            
            // Set stub IDs for allSuperclassTypes, allInterfaceTypes, and allInheritedMethods,
            // so long as the class is in the same library.
            setClassStubIdsIfSameLibrary( libraryId,
                                          (List<Map>) javadocModel.get("allSuperclassTypes"),
                                          libraryClassNames );
            
            setClassStubIdsIfSameLibrary( libraryId,
                                          (List<Map>) javadocModel.get("allInterfaceTypes"),
                                          libraryClassNames );
            
            for ( Map inherited : Cawls.safeIterable( (List<Map>) javadocModel.get("allInheritedMethods") ) ) {
                Map superclassStub = (Map) inherited.get("superclassType");
                if ( libraryClassNames.contains( JavadocMapUtils.getQualifiedName( superclassStub ) ) ) {
                    setStubIdHelper( libraryId, superclassStub );
                    setStubIdsHelper( libraryId, (List<Map>) inherited.get("inheritedMethods"));
                }
            }
        } else if (JavadocMapUtils.isMethod(javadocModel)) {
            
            setOverriddenMethodStubIdIfSameLibrary(libraryId, javadocModel, libraryClassNames);
            
        }
        
        return javadocModel;
    }

        
        
    /**
     * Set stubIds for all stubs that are guaranteed to be in the same library.
     * @return doc
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
        }
        return stub;
    }
    
    
    /**
     * Set Ids into the list of stubs by combining the stub's relativeId with
     * the given libraryId -- IF AND ONLY IF the stub actually exists in this library.
     * 
     * Note: the stub objects are modified in place.
     */
    protected List<Map> setClassStubIdsIfSameLibrary(String libraryId, 
                                                     List<Map> classStubs,  
                                                     Set<String> libraryClassNames) {
        
        for (Map stub : Cawls.safeIterable(classStubs)) {
            if ( libraryClassNames.contains( JavadocMapUtils.getQualifiedName(stub) ) ) {
                setStubIdHelper(libraryId, stub);
            }
        }
        
        return classStubs;
    }
    
    /**
     * Set Ids into the list of stubs by combining the stub's relativeId with
     * the given libraryId -- IF AND ONLY IF the stub actually exists in this library.
     * 
     * Note: the stub objects are modified in place.
     */
    protected Map setOverriddenMethodStubIdIfSameLibrary(String libraryId, 
                                                         Map methodDoc, 
                                                         Set<String> libraryClassNames) {
        
        Map overriddenType = (Map) methodDoc.get("overriddenType");
        if (overriddenType != null && libraryClassNames.contains( JavadocMapUtils.getQualifiedName(overriddenType) )) {
            setStubIdHelper(libraryId, (Map) methodDoc.get("overriddenMethod"));
        }
      
        return methodDoc;
    }

    @Override
    public void after(DB db, String collection) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void before(DB db, String collection) {
        // TODO Auto-generated method stub
        
    }
 

}



