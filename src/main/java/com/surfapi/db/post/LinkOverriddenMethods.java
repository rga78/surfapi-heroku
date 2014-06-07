
package com.surfapi.db.post;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.coll.Cawls;
import com.surfapi.db.DB;

/**
 * Post-processor - Link all methods with the method they override and/or implement.
 * 
 * Sets the _overrides and _implements fields in the methodDoc.
 * 
 * !!! NOTE: Post-processor dependencies !!!
 *      0. ReferenceNameQuery index
 *      1. SetStubIds (_ids are used to lookup containingClass)
 *      2. CollectSuperClasses (resolves all inherited interfaces)
 *      3. CollectInheritedMembers (resolves all inherited methods in each interface).
 *
 *
 * TODO: this is very similar to CollectInheritedMembers. The two could probably be combined.
 *
 *
 */
public class LinkOverriddenMethods implements DB.ForAll {

    /**
     *
     */
    @Override
    public void call(DB db, String collection, Map doc) {

        if ( JavadocMapUtils.getMetaType(doc).equals("method") ) {

            Map overriddenMethod = findOverriddenMethod(db, doc);
            Map implementedMethod = findImplementedMethod(db, doc);     // implements an interface method
            
            // Set the _overrides field if we got one
            if (overriddenMethod != null) {
                doc.put( JavadocMapUtils.OverridesFieldName, buildOverridesStub( overriddenMethod ) );
            }

            // Set the _implements field if we got one
            if (implementedMethod != null) {
                doc.put( JavadocMapUtils.ImplementsFieldName, buildOverridesStub( implementedMethod ) );
            }

            // Save back to the DB if updates were made
            if (overriddenMethod != null || implementedMethod != null) {
                db.save( collection, doc);
            }

        } else {
            // TODO: any other types to worry about? ClassDoc -> SuperClassDoc? (it already does).
        }
    }

    /**
     *
     * @param db a ref to the DB.
     * @param methodDoc javadoc document, must be a method
     *
     * @return The nearest super method overridden by the given methodDoc.
     */
    protected Map findOverriddenMethod(DB db, Map methodDoc) {

        Map containingClass = db.read( JavadocMapUtils.getId( (Map) methodDoc.get("containingClass") ) );

        // containingClass should NEVER be null (so long as SetStubIds has been run).
        Map superClass = new ReferenceNameQuery().inject(db).lookupSuperclassDoc( containingClass ) ;

        return findOverriddenMethodHelper(db, methodDoc, superClass);
    }

    /**
     *
     * @param db a ref to the DB.
     * @param childMethod javadoc document, must be a method
     * @param superClassDoc the class in which to look for the overridden method
     *
     * @return The nearest super method overridden by the given methodDoc.
     */
    protected Map findOverriddenMethodHelper(DB db, Map childMethod, Map superClassDoc) {

        // Could not be found or at the top of the inheritence tree.
        if (superClassDoc == null) {
            return null;
        }

        // Examine each methd in the parent class, looking for one that "equals" 
        // (i.e is-overridden-by) the child method
        Map overriddenMethod = JavadocMapUtils.findOverriddenMethod( childMethod, (List<Map>) superClassDoc.get("methods"));

        if (overriddenMethod != null) {
            return overriddenMethod;
        }

        // If we got here, then we didn't find the overridden method in this superclass.
        // Recurse to the next superclass.
        superClassDoc = new ReferenceNameQuery().inject(db).lookupSuperclassDoc( superClassDoc ) ;

        return findOverriddenMethodHelper(db, childMethod, superClassDoc );
    }

    /**
     *
     * NOTE:  Must be run AFTER CollectSuperClasses, which sets the _interfaces field
     *        to ALL implmemented interfaces of the containing class (including the
     *        interfaces of its parent classes).  
     *
     * @param db a ref to the DB.
     * @param methodDoc javadoc document, must be a method
     * 
     * @return The interface method implemented by the given methodDoc.
     *
     */
    protected Map findImplementedMethod(DB db, Map methodDoc) {

        Map containingClass = db.read( JavadocMapUtils.getId( (Map) methodDoc.get("containingClass") ) );

        for (Map interfaceDoc : Cawls.safeIterable( (List<Map>) containingClass.get( JavadocMapUtils.InterfacesFieldName ) ) ) {

            Map lookupInterfaceDoc = new ReferenceNameQuery().inject(db).queryOne( JavadocMapUtils.getQualifiedName(interfaceDoc),
                                                                                   JavadocMapUtils.getLibraryId(methodDoc) );
            
            Map implementedMethod = findImplementedMethodHelper( db, 
                                                                 methodDoc,
                                                                 db.read( JavadocMapUtils.getId( lookupInterfaceDoc )  ) );

            if (implementedMethod != null) {
                return implementedMethod;
            }
        }

        // Implemented method does not exist or could not be found.
        return null;
    }

    /**
     *
     * NOTE:  Must be run AFTER CollectedInheritedMembers, which sets the _inherited methods 
     *        for the given interfaceDoc so I don't need to recurse to find them.
     *
     * @param db a ref to the DB.
     * @param childMethod javadoc document, must be a method
     * @param interfaceDoc the class in which to look for the implemented method
     *
     * @return The interface method implemented by the given methodDoc.
     */
    protected Map findImplementedMethodHelper(DB db, Map childMethod, Map interfaceDoc ) {

        // Inteface could not be found 
        if (interfaceDoc == null) {
            return null;
        }

        Map implementedMethod = JavadocMapUtils.findOverriddenMethod(childMethod, (List<Map>) interfaceDoc.get("methods") );

        if (implementedMethod != null) {
            return implementedMethod;
        } 

        // Not found.  Check inherited methods.
        for (Map inheritedDoc : Cawls.safeIterable( (List<Map>) interfaceDoc.get( JavadocMapUtils.InheritedFieldName ) ) ) {

            implementedMethod = JavadocMapUtils.findOverriddenMethod(childMethod, (List<Map>) inheritedDoc.get("methods") );

            if (implementedMethod != null) {
                return implementedMethod;
            } 
        }

        // Still not found. Oh well, we tried everything.
        return null;
    }


    /**
     * @return a stub of the given methodDoc for the _overrides field.
     */
    protected Map buildOverridesStub(Map methodDoc) {
        return Cawls.pick(methodDoc, Arrays.asList( "_id", "qualifiedName" ));
    }

}


