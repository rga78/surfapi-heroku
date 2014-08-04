package com.surfapi.app;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;

/**
 * 
 * Note: 
 */
public class JavadocMapUtils {
    
    /**
     * Internal field names.
     */
    public static final String InheritedFieldName = "_inherited";
    public static final String LibraryFieldName = "_library";
    public static final Object SuperclassesFieldName = "_superclasses";
    public static final Object InterfacesFieldName = "_interfaces";
    public static final Object OverridesFieldName = "_overrides";
    public static final Object ImplementsFieldName = "_implements";

    /**
     * metaTypes that correspond to class types.
     */
    public static final Collection<String> ClassMetaTypes = Arrays.asList( new String[] { "class",
                                                                                          "interface",
                                                                                          "annotationType", 
                                                                                          "enum" } );
    
    /**
     * metaTypes that correspond to methods.
     */
    public static final Collection<String> MethodMetaTypes = Arrays.asList( new String[] { "method",
                                                                                          "constructor",
                                                                                          "annotationTypeElement" } );
    
    /**
     * 
     */
    public static String buildId(String libraryId, Map doc) {
        return libraryId + "/" + getRelativeId(doc);
    }
    
    /**
     * @return a Mapping of the given libraryId, with fields for lang, name, and version
     *         e.g. "/java/java-sdk/1.6" maps to: { _id: ""/java/java-sdk/1.6", lang: "java", name: "java-sdk", version: "1.6" }
     */
    public static Map<String, String> mapLibraryId(String libraryId) {
        String[] lib = StringUtils.split(libraryId, "/");
        
        return new MapBuilder<String, String>().append("_id", libraryId)
                                               .append("lang", lib[0])
                                               .append("name", lib[1])
                                               .append("version", lib[2]); 
    }

    
    /**
     * @return the package-qualified name
     */
    public static String getQualifiedName(Map doc) {
        if (isPackage(doc)) {
            return (String) doc.get("name") ;
        } else if (doc != null) {
            return (String) ObjectUtils.firstNonNull( doc.get("qualifiedName"), doc.get("qualifiedTypeName") );
        } else {
            return null;
        }
    }
    
   
    /**
     * @return the "relative" ID for this javadoc element. The relativeId does not contain the libraryId.
     */
    public static String getRelativeId(Map doc) {
        return ( isMethod(doc) ) ? getQualifiedName(doc) + getParameterSignature(doc) : getQualifiedName(doc);
    }
    
    /**
     * @return the fully-qualified list of parameter types, separated by ','.
     */
    public static String getParameterSignature(Map doc) {
        return getQualifiedParameterSignature(doc);
    }
    
    
    /**
     * @return the fully-qualified list of parameter types, separated by ','.
     */
    public static String getQualifiedParameterSignature(Map doc) {
    
        List<Map> parmTypes = (List<Map>) Cawls.pluck((List<Map>)doc.get("parameters"), "type");
        
        StringBuffer retMe = new StringBuffer("(");
        String sep = "";
        for (Map parmType : parmTypes) {
            retMe.append(sep)
                 .append( parmType.get("qualifiedTypeName") )
                 .append( ObjectUtils.firstNonNull( parmType.get("dimension"), "") );
            sep = ",";
        }
        retMe.append(")");
        
        return retMe.toString();
    }
    
    /**
     * @return the non-qualified list of parameter types, separated by ','.
     */
    public static String getNonQualifiedParameterSignature(Map doc) {
        
        List<Map> parmTypes = (List<Map>) Cawls.pluck((List<Map>)doc.get("parameters"), "type");
        
        StringBuffer retMe = new StringBuffer("(");
        String sep = "";
        for (Map parmType : parmTypes) {
            retMe.append(sep)
                 .append( parmType.get("typeName") )
                 .append( ObjectUtils.firstNonNull( parmType.get("dimension"), "") );
            sep = ",";
        }
        retMe.append(")");
        
        return retMe.toString();
    }
    
   
    /**
     * @return the "metaType" field
     */
    public static String getMetaType(Map doc) {
        return (doc != null) ? (String) ObjectUtils.firstNonNull( doc.get("metaType"), "") : "";
    }

    
    /**
     * @return true if the given doc is a package type.
     */
    public static boolean isPackage(Map doc) {
        return getMetaType(doc).equals("package");
    }
    
    /**
     * @return true if the given doc is a class type (enum, interface, class, annotationType)
     */
    public static boolean isClass(Map doc) {
        return ClassMetaTypes.contains( getMetaType(doc) );
    }
    
    /**
     * @return true if the given doc is a method type (method, constructor, annotationTypeElement)
     */
    public static boolean isMethod(Map doc) {
        return MethodMetaTypes.contains( getMetaType(doc) );
    }

    /**
     * @return the _id field from the given doc, or null if the doc is null.
     */
    public static String getId(Map doc) {
        return (doc != null) ? (String) doc.get("_id") : null;
    }

    /**
     * @return the first method in the parentMethods list that is overridden by the given childMethod.
     *         or null if there are no matches.
     */
    public static Map findOverriddenMethod(Map childMethod, List<Map> parentMethods) {

        for (Map parentMethod : parentMethods) {

            if ( JavadocMapUtils.methodEquals(childMethod, parentMethod) ) {
                return parentMethod;
            }
        }

        return null;
    }

    /**
     * TODO: this should be named methodOverrides or something like that, because 
     *       the parentMethod's parameter types are treated differently (if they're 
     *       template types, they match automatically).
     *
     * @return true if the given childMethod overrides the given parentMethod.
     *         The methods are equal if the names and signatures match.
     */
    public static boolean methodEquals(Map childMethod, Map parentMethod) {
        return ( ObjectUtils.equals( childMethod.get("name"), parentMethod.get("name") )
                 && methodParmsEquals( (List<Map>) childMethod.get("parameters"), 
                                       (List<Map>) parentMethod.get("parameters") ) );
    }

    /**
     * @return true if the given parms lists match in number and types.
     */
    public static boolean methodParmsEquals(List<Map> childParms, List<Map> parentParms) {

        if (childParms == null || parentParms == null) {
            return (childParms == parentParms);
        } else if (childParms.size() != parentParms.size()) {
            return false;
        } else {
            for (int i=0; i < childParms.size(); ++i) {
                if (! typeEquals( (Map) childParms.get(i).get("type"), (Map) parentParms.get(i).get("type") ) ) {
                    return false;
                }
            }
            // If we got here, they must all match.
            return true;
        }
    }

    /**
     * @return true if the two types are the same.  If the parentType is a template type, always return true.
     */
    public static boolean typeEquals(Map childType, Map parentType) {
        if (childType == null || parentType == null) {
            return (childType == parentType);
        } else if ( ObjectUtils.equals( childType.get("qualifiedTypeName"), parentType.get("qualifiedTypeName") ) ) {
            return true;
        } else if ( StringUtils.length( (String) parentType.get("qualifiedTypeName") ) == 1 ) {
            // Assuming the parent type is a template type, so it matches everything.
            // This isn't 100% accurate - really should be checking the type declaration in the child classDoc.
            // But that's a bit complicated.
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return the _library._id field in the given doc.
     */
    public static String getLibraryId(Map doc) {
        return (String) ((Map)doc.get(JavadocMapUtils.LibraryFieldName)).get("_id");
    }
    
    /**
     * @return the _library.version field.
     */
    public static String getLibraryVersion(Map doc) {
        return (String) ((Map)doc.get(JavadocMapUtils.LibraryFieldName)).get("version");
    }


    /**
     * @return true if metaType="interface"
     */
    public static boolean isInterface(Map javadocModel) {
        return "interface".equals(JavadocMapUtils.getMetaType(javadocModel));
    }
    
    /**
     * @return a subset of fields for displaying in an <sa-type> element.
     */
    public static Map buildTypeStub(Map javadocModel) {
        return  Cawls.pick( javadocModel, 
                            Arrays.asList( "_id",
                                           "name", 
                                           "qualifiedName", 
                                           "qualifiedTypeName", 
                                           "dimension", 
                                           "typeName", 
                                           "parameterizedType",
                                           "wildcardType") ); 
    }

    /**
     * @return an id that doesn't include the library versions
     */
    public static String getIdSansVersion(Map javadocModel) {
        return LibraryUtils.getIdSansVersion((Map) javadocModel.get(JavadocMapUtils.LibraryFieldName))
                + "/" + getRelativeId(javadocModel) ;
    }

    /**
     * @return doc with the _id field removed
     */
    public static Map removeId(Map doc) {
        doc.remove("_id");
        return doc;
    }


}
