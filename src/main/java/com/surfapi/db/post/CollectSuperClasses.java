
package com.surfapi.db.post;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.coll.Cawls;
import com.surfapi.db.DB;

/**
 * Post-processor - finds all super classes for a given document and sets them
 * into a list in the document's _superclassTypes field.
 * 
 * Also collects all interfaces implemented not only by this object but all those implemented
 * by superclasses as well and sets them into the object's _interfaceTypes field.
 *
 */
public class CollectSuperClasses implements DB.ForAll {

//    /**
//     *
//     */
//    @Override
//    public void call(DB db, String collection, Map obj) {
//        
//        List<Map> superclasses = getSuperclasses( db, obj );
//
//        List<Map> interfaces = Cawls.uniqueForField( getInterfaces(db, obj), "qualifiedTypeName" );
//        
//        if ( ! superclasses.isEmpty()) {
//            obj.put( JavadocMapUtils.SuperclassesFieldName, superclasses );
//        }
//
//        if ( ! interfaces.isEmpty() ) {
//            obj.put( JavadocMapUtils.InterfacesFieldName, interfaces);
//        }
//
//        // If either was updated, save to the DB.
//        if ( !superclasses.isEmpty() || !interfaces.isEmpty() ) {
//            db.save(collection, obj);
//        }
//
//    }
    
    
    /**
     *
     */
    @Override
    public void call(DB db, String collection, Map doc) {

        if (JavadocMapUtils.isClass(doc) ) {
        
            List<Map> superclasses = getAllSuperclassTypesAcrossLibraries( db, doc );

            List<Map> interfaces = Cawls.uniqueForField( getAllInterfaceTypesAcrossLibraries(db, doc), "qualifiedTypeName" );

            doc.put( JavadocMapUtils.SuperclassesFieldName, superclasses );
            
            doc.put( JavadocMapUtils.InterfacesFieldName, interfaces);

            db.save(collection, doc);
        }

    }

    /**
     * Recursively gets the list of superclasses for the given doc object.
     *
     * @return List of superclasses for the given doc object. The List is ordered
     *         from the greatest grand-parent class (base class) down to the 
     *         most derived (the immediate parent class).
     */
    protected List<Map> getSuperclasses(DB db, Map doc) {

        Map superclass = (doc != null) ? (Map) doc.get("superclassType") : null;

        if (superclass == null) {
            // Base case - return empty map.
            return new ArrayList<Map>();

        } else {
            
            // Use the ReferenceNameQuery to lookup the superclass.
            // queryOne() will return the first match in the same library.
            // If none match the library, then the first result is returned
            
            Map lookupSuperclassDoc =  new ReferenceNameQuery().inject(db).queryOne( JavadocMapUtils.getQualifiedName(superclass),
                                                                                     JavadocMapUtils.getLibraryId( doc ) );
            
            List<Map> retMe = getSuperclasses(db, db.read( JavadocMapUtils.getId(lookupSuperclassDoc) ) );
            
            retMe.add( superclass );
            return retMe;
        }

    }
    
    /**
     * Recursively gets the list of superclasses for the given doc object,
     * searching across libraries to resolve the complete list.
     * 
     * TODO: need a test for this.
     *
     * @return fully resolved list of superclass types for the given doc.
     */
    protected List<Map> getAllSuperclassTypesAcrossLibraries(DB db, Map doc) {

        List<Map> retMe = new ArrayList<Map>();
            
        List<Map> allSuperclassTypes = (doc != null) ? (List<Map>) doc.get("allSuperclassTypes") : null;

        if (allSuperclassTypes != null && allSuperclassTypes.size() > 0) {

            retMe.addAll( allSuperclassTypes );
            
            // Jump to the last superclassType in the list (oldest parent)
            Map oldestParent = Cawls.getLast( allSuperclassTypes ); 

            // Use the ReferenceNameQuery to lookup the _id of the oldestParent.
            // queryOne() will return the first match in the same library.
            // If none match the library, then the first result is returned
            oldestParent = new ReferenceNameQuery().inject(db).queryOne( JavadocMapUtils.getQualifiedName(oldestParent),
                                                                         JavadocMapUtils.getLibraryId( doc ) );
            
            
            retMe.addAll( getAllSuperclassTypesAcrossLibraries(db, db.read( JavadocMapUtils.getId(oldestParent) ) ) );
        }
        
        return retMe;
    }
    
    /**
     * Recursively gets the list of interfaces for the given doc object.
     * An object implements not only the interfaces that it explicitly declares but
     * also the interfaces implemented by all its superclasses.
     *
     * @return List of interfaces implemented by the given doc object (including those
     *         implemented by superclasses).
     */
    protected List<Map> getInterfaces(DB db, Map doc) {

        List<Map> retMe = new ArrayList<Map>();

        // PackageDocs have an "interfaces" field but it's not the one we're looking for.
        if (doc == null || JavadocMapUtils.isPackage(doc)) {
            return retMe;

        } else {
            retMe.addAll( ObjectUtils.firstNonNull( (List<Map>) doc.get("interfaceTypes"), new ArrayList<Map>() ) );

            // Recurse if we have a superclass.
            Map superclass = (Map) doc.get("superclass");

            if (superclass == null) {
                return retMe;

            } else {
                
                Map lookupSuperclassDoc =  new ReferenceNameQuery().inject(db).queryOne( JavadocMapUtils.getQualifiedName(superclass),
                                                                                         JavadocMapUtils.getLibraryId( doc ) );


                
                retMe.addAll( getInterfaces(db, db.read( JavadocMapUtils.getId(lookupSuperclassDoc) ) ) );
                return retMe;
            }
        }
    }
    
    
    /**
     * Recursively gets the list of interfaces for the given doc object.
     * An object implements not only the interfaces that it explicitly declares but
     * also the interfaces implemented by all its superclasses.
     * 
     * We can assume that the doclet already populated all interfaceTypes that were
     * available at the time the doclet was run.  That means the only interfaceTypes
     * we need to search for are those across libraries that weren't present when
     * then doclet was run.  So jump to the oldest superclass type, search for it
     * across libraries, and add all its interfaceTypes to the list.
     *
     * @return fully resolved list of interfaceTypes implemented by the given doc.
     */
    protected List<Map> getAllInterfaceTypesAcrossLibraries(DB db, Map doc) {

        List<Map> retMe = new ArrayList<Map>();

        if (doc != null) {
        
            // Add all interfaceTypes implemented by this doc.
            retMe.addAll( ObjectUtils.firstNonNull( (List<Map>) doc.get("allInterfaceTypes"), new ArrayList<Map>() ) );

            // Jump to the last superclassType in the list (oldest parent)
            Map oldestParent = Cawls.getLast( (List<Map>) doc.get("allSuperclassTypes") ); 
            
                
            oldestParent =  new ReferenceNameQuery().inject(db).queryOne( JavadocMapUtils.getQualifiedName(oldestParent),
                                                                          JavadocMapUtils.getLibraryId( doc ) );

            retMe.addAll( getAllInterfaceTypesAcrossLibraries(db, db.read( JavadocMapUtils.getId(oldestParent) ) ) );
        }
        
        return retMe;
    }
}


