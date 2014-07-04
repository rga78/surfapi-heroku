package com.surfapi.db.post;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.coll.EasyMap;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.DB;


/**
 * Iterate thru the DB and for each class/interface, collect the list of inherited methods
 * and add their stubs to the class/interface document.
 *
 * DEPENDENCY: ReferenceNameQuery must be built (for superclass lookups).
 * 
 * TODO: change this to resolve cross-library only (inherited methods from the same
 *       library are already included by MongoDoclet). 
 */
public class CollectInheritedMembers implements DB.ForAll {

    /**
     *
     * { 
     *   "_inherited" : [ { "superclass": {stub}, 
     *                     "methods": [ {stubs} ] ,
     *                     "fields": [ {stubs} ] },
     *
     */
    @Override
    public void call(DB db, String collectionName, Map doc) {
        
        if (JavadocMapUtils.isClass(doc)) {
            List<Map> inheritedMembers = collectInheritedMembers(db, doc);

            if (inheritedMembers.size() > 0) {
                doc.put( JavadocMapUtils.InheritedFieldName , inheritedMembers);
                db.save( collectionName, doc );
            }
        }
    }

    /**
     * @return a list of inherited methods/fields from all known superclasses of the given doc.
     */
    protected List<Map> collectInheritedMembers(DB db, Map doc) {
        
        List<Map> retMe = new ArrayList<Map>();
        
        // Accumulate inherited methods as we move up the inheritance tree.
        // This keeps track of methods that were already inherited, so as not
        // to match them again if a higher superclass also defines them.
        // Start off with the doc's methods.
        List<Map> methodAccumulator = new ArrayList<Map>((List<Map>) new EasyMap(doc).getList("methods"));
        List<Map> fieldAccumulator = new ArrayList<Map>((List<Map>) new EasyMap(doc).getList("fields"));

        ReferenceNameQuery refQuery = new ReferenceNameQuery().inject(db);
        
        Map superClassStub = (Map) doc.get("superclass");

        // Loop thru each superclass in the hierarchy
        for (Map superClassDoc = refQuery.lookupSuperclassDoc(doc);
             superClassDoc != null;
             superClassDoc = refQuery.lookupSuperclassDoc(superClassDoc) ) {

            // Filter for superclass methods that are inherited (i.e not overridden) by this doc
            List<Map> inheritedMethods = selectInheritedMethods(methodAccumulator, (List<Map>)superClassDoc.get("methods"));
            List<Map> inheritedFields = selectInheritedFields(fieldAccumulator, (List<Map>) superClassDoc.get("fields"));

            if (inheritedMethods.size() > 0 || inheritedFields.size() > 0) {
                retMe.add( new MapBuilder().append("superclass", superClassStub)
                                           .append("methods", inheritedMethods)
                                           .append("fields", inheritedFields) );
            }

            methodAccumulator.addAll(inheritedMethods);
            fieldAccumulator.addAll(inheritedFields);

            superClassStub = (Map) superClassDoc.get("superclass");
        }

        return retMe;
    }
    
    /**
     * @return a list of inherited methods/fields from all known superclasses of the given doc.
     */
    protected List<Map> collectInheritedMembers2(DB db, Map doc) {
        
        List<Map> retMe = new ArrayList<Map>();
        
        // Accumulate inherited methods as we move up the inheritance tree.
        // This keeps track of methods that were already inherited, so as not
        // to match them again if a higher superclass also defines them.
        // Start off with the doc's methods.
        List<Map> methodAccumulator = new ArrayList<Map>((List<Map>) new EasyMap(doc).getList("methods"));

        ReferenceNameQuery refQuery = new ReferenceNameQuery().inject(db);
        
        Map superClassStub = (Map) doc.get("superclass");

        // Loop thru each superclass in the hierarchy
        for (Map superClassDoc = refQuery.lookupSuperclassDoc(doc);
             superClassDoc != null;
             superClassDoc = refQuery.lookupSuperclassDoc(superClassDoc) ) {

            // Filter for superclass methods that are inherited (i.e not overridden) by this doc
            List<Map> inheritedMethods = selectInheritedMethods(methodAccumulator, (List<Map>)superClassDoc.get("methods"));

            if (inheritedMethods.size() > 0 ) {
                retMe.add( new MapBuilder().append("superclass", superClassStub)
                                           .append("methods", inheritedMethods) );
            }

            methodAccumulator.addAll(inheritedMethods);


            superClassStub = (Map) superClassDoc.get("superclass");
        }

        return retMe;
    }
    
    /**
     * Return a sublist of parentMethods that includes only those methods that
     * are inherited by the child class.  
     *
     * @param childMethods The child class's methods (which may override some or all of the parent's methods)
     * @param parentMethods The parent's methods.
     *
     * @return a sublist of parentMethods that are not overridden by the given childMethods.
     */
    protected List<Map> selectInheritedMethods(List<Map> childMethods, List<Map> parentMethods) {
        
        List<Map> retMe = new ArrayList<Map>();
        
        for (Map parentMethod : parentMethods) {
            if (! isMethodOverridden( childMethods, parentMethod ) ) {
                // Not overridden - so it's inherited.
                retMe.add(parentMethod);
            }
        }
        return retMe;
    }
    
    /**
     * @return true if the given parent method (from a superclass) is overridden by any of
     *         the methods in the list of childMethods. 
     */
    protected boolean isMethodOverridden(List<Map> childMethods, Map parentMethod) {
        for (Map childMethod : childMethods) {
            if ( JavadocMapUtils.methodEquals(childMethod, parentMethod) ) {
                return true;
            }
        }

        // If we got here, then none of the childMethods matched.  
        return false;
    }

    /**
     * TODO
     */
    protected List<Map> selectInheritedFields(List<Map> fieldAccumulator, List<Map> fields) {
        return Collections.EMPTY_LIST;
    }

//    /**
//     * 
//     */
//    protected List<Map> xx_collectInheritedMembers(DB db, Map doc) {
//        
//        List<Map> retMe = new ArrayList<Map>();
//        
//        // Accumulate inherited methods as we move up the inheritance tree.
//        // This keeps track of methods that were already inherited, so as not
//        // to match them again if a higher superclass also defines them.
//        // Start off with this doc's methods.
//        List<Map> methodAccumulator = new ArrayList<Map>((List<Map>) doc.get("methods"));
//        List<Map> fieldAccumulator = new ArrayList<Map>((List<Map>) doc.get("fields"));
//        
//        // This list is used for... somethign... linking up this doc's methods with the
//        // methods they override. As methods are linked up they are removed from this list
//        // (so as not to match them again in a superclass). 
//        List<Map> originalMethodList = new ArrayList<Map>((List<Map>) doc.get("methods") );
//
//        ReferenceNameQuery refQuery = new ReferenceNameQuery().inject(db);
//        
//        Map superClassStub = (Map) doc.get("superclass");
//
//        // Loop thru each superclass in the hierarchy
//        for (Map superClassDoc = refQuery.lookupSuperclassDoc(doc);
//             superClassDoc != null;
//             superClassDoc = refQuery.lookupSuperclassDoc(superClassDoc) ) {
//
//            // Filter for superclass methods that are inherited (i.e not overridden) by this doc
//            List<Map> inheritedMethods = selectInheritedMethods(methodAccumulator, (List<Map>)superClassDoc.get("methods"));
//            List<Map> inheritedFields = selectInheritedFields(fieldAccumulator, (List<Map>) superClassDoc.get("fields"));
//
//            // Add any results to the output list.
//            if (inheritedMethods.size() > 0 || inheritedFields.size() > 0) {
//                retMe.add( new MapBuilder().append("superclass", superClassStub)
//                                           .append("methods", inheritedMethods)
//                                           .append("fields", inheritedFields) );
//            }
//            
//            linkOverriddenMethods( db, originalMethodList, (List<Map>)superClassDoc.get("methods") );
//    
//            methodAccumulator.addAll(inheritedMethods);
//            fieldAccumulator.addAll(inheritedFields);
//
//            superClassStub = (Map) superClassDoc.get("superclass");
//        }
//
//        return retMe;
//    }
//
//    /**
//     * 
//     */
//    protected void linkOverriddenMethods( DB db, List<Map> childMethods, List<Map> parentMethods ) {
//        for ( Iterator<Map> iter = childMethods.iterator(); iter.hasNext(); ) {
//            
//            Map childMethod = iter.next();
//       
//            // Examine each methd in the parent class, looking for one that "equals" 
//            // (i.e is-overridden-by) the child method
//            Map overriddenMethod = JavadocMapUtils.findOverriddenMethod( childMethod, parentMethods);
//
//            if (overriddenMethod != null) {
//                linkOverriddenMethod( db, childMethod, overriddenMethod );
//                
//                // Remove the childMethod from the list so that it isn't match again
//                // on subsequent searches against further superclasses.
//                iter.remove();   
//            }
//        }
//    }
//    
//    /**
//     * Link the given overriddenMethod to the given childMethod in the childMethod's "_overrides"
//     * field in the db.
//     */
//    protected void linkOverriddenMethod( DB db, Map childMethod, Map overriddenMethod ) {
//
//        Map childMethodDoc = db.read( JavadocMapUtils.getId(childMethod) );
//        childMethodDoc.put( JavadocMapUtils.OverridesFieldName, buildOverridesStub( overriddenMethod ) );
//        db.save( JavadocMapUtils.getLibraryId(childMethodDoc), childMethodDoc );
//    }
//    
//    
//    /**
//     * @return a stub of the given overriddenMethod for the _overrides field.
//     */
//    protected Map buildOverridesStub(Map overriddenMethod) {
//        return Cawls.pick(overriddenMethod, Arrays.asList( "_id", "qualifiedName" ));
//    }
//

}




