package com.surfapi.app;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONAware;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * 
 * Note: 
 */
public class JavadocObject implements Comparable<JavadocObject>, JSONAware {
    

    /**
     * metaTypes that correspond to class types.
     */
    public static final Collection<String> ClassMetaTypes = Arrays.asList( new String[] { "class",
                                                                                          "interface",
                                                                                          "annotationType", 
                                                                                          "enum" } );
    /**
     * The javadoc JSON.
     */
    private Map json;
   
    /**
     * CTOR
     *
     * Note: wraps the given map directly (i.e does NOT make a copy).
     */
    public JavadocObject(Map json) {
        this.json = json;
    }

    /**
     * CTOR.
     * 
     * Note: makes a copy of the given Map
     * Note: adds new fields "_library" and "_id" 
     *
     */
    public JavadocObject(String libraryId, Map json) {
        this( new HashMap(json) );
        
        this.json.put("_library", parseLibraryId(libraryId)); 
        this.json.put("_id", buildId());
    }
    
    /**
     * Copy CTOR.
     */
    public JavadocObject(JavadocObject dbObject) {
        this(dbObject.getJson());
    }
    
    /**
     * 
     */
    protected String buildId() {
        return getLibraryId() + "/" + getQualifiedNameWithSignature();
    }
    
    /**
     * 
     */
    protected String xx_buildId() {
        if (isPackage()) {
            return getLibraryId() + "/" + getPackageName();
        } else if (isClass()) {
            return getLibraryId() + "/" + getPackageName() + "/" + getClassName();
        } else {
            return getLibraryId() + "/" + getPackageName() + "/" + getClassName() + "/" + getNameWithSignature();
        }
    }
    
    /**
     * @return a Mapping of the given libraryId, with fields for lang, name, and version
     *         e.g. "/java/java-sdk/1.6" maps to: { _id: ""/java/java-sdk/1.6", lang: "java", name: "java-sdk", version: "1.6" }
     */
    public static Map parseLibraryId(String libraryId) {
        String[] lib = StringUtils.split(libraryId, "/");
        
        JSONObject retMe = new JSONObject();
        
        retMe.put("_id", libraryId);
        retMe.put("lang", lib[0]);
        retMe.put("name", lib[1]);
        retMe.put("version", lib[2]);
        
        return retMe;
    }
    
    /**
     * @return the doc json object
     */
    public Map getJson() {
        return json;
    }

    /**
     * @return the libraryId for this doc element
     */
    public String getLibraryId() {
        return (String) getMap( JavadocMapUtils.LibraryFieldName ).get("_id");
    }
    
    /**
     * @return the library map
     */
    public Map<String, String> getLibrary() {
        return getMap( JavadocMapUtils.LibraryFieldName );
    }
    
    /**
     * @return the package qualified name, without the flatSignature (if a method).
     *         e.g. java.lang, java.lang.String, java.lang.String.replace
     */
    public String getQualifiedName() {
        // return (isPackage()) ? getString("name") : ObjectUtils.firstNonNull( getString("qualifiedName"), getString("qualifiedTypeName") );
        return (isPackage()) ? getString("name") : getString("qualifiedName") ;
    }
    
    /**
     * @return the package name for this javadoc element.
     */
    protected String getPackageName() {
        return (isPackage()) ? getString("name") : (String) getMap("containingPackage").get("name");
    }
    
    /**
     * @return the package name for this javadoc element.
     */
    protected String getClassName() {
        return (isClass()) ? getString("name") : (String) getMap("containingClass").get("name"); // TODO: NPE when called by a package
    }
    
    /**
     * @return a unique identifier for this db record.
     */
    public String getId() {
        return getString("_id");
    }
    
    /**
     * @return the "relative" ID for this javadoc element. The relativeId does not contain the libraryId.
     */
    public String getRelativeId() {
        return getQualifiedNameWithSignature();
    }
    
    /**
     * @return the package-qualified name + flatSignature
     *         e.g: java.lang, java.lang.String, java.lang.String.replace(char,char)
     */
    public String getQualifiedNameWithSignature() {
        return StringUtils.join( getQualifiedName(), getString("flatSignature"));
    }
    
    /**
     * @return the method name + flatSignature
     */
    public String getNameWithSignature() {
        return StringUtils.join( getString("name"), getString("flatSignature"));
    }
    
    /**
     * @return an array of package-qualified names. Normally this returns only 1 name but if it's
     *         a method it will return the name with and without the flatSignature.
     *         e.g: [java.lang], [java.lang.String], [java.lang.String.replace,java.lang.String.replace(char,char)]
     * 
     * @deprecated - not using
     */
    public Collection<String> getQualifiedNames() {
        Collection<String> retMe = new ArrayList<String>();
        
        retMe.add( StringUtils.join( getLibraryId(), getQualifiedName()));
        
        String flatSignature = getString("flatSignature");
        if (!StringUtils.isEmpty( flatSignature)) {
            retMe.add( getQualifiedNameWithSignature() );
        }
        
        return retMe;
    }

    /**
     * @return true if the two DBObject's IDs are the same; false otherwise.
     *       
     */
    public boolean equals(Object o) {
        if (o instanceof JavadocObject ) {
            return this.getId().equals( ((JavadocObject)o).getId() );
        } else {
            return false;
        }
    }
    
    /**
     * @return the String value at the given key.
     */
    public String getString(String key) {
        return (String) json.get(key);
    }
    
    /**
     * @return the map at the given key.
     */
    public Map getMap(String key) {
        return (Map) json.get(key);
    }
            
    /**
     * 
     */
    public String getMetaType() {
        String metaType = getString("metaType");
        
        if (metaType == null) {
            throw new IllegalArgumentException("metaType cannot be null.  JSONObject: " + json);
        }   
        
        return metaType;
    }
    
    /**
     * @return true if the given doc is a package type.
     */
    public boolean isPackage() {
        return getMetaType().equals("package");
    }
    
    /**
     * @return true if the given doc is a class type (enum, interface, class, annotationType)
     */
    public boolean isClass() {
        return ClassMetaTypes.contains( getMetaType() );
    }
    
    /**
     * 
     */
    @Override
    public String toString() {
        return getQualifiedName() + " (" + json.get("metaType") + ")";
    }

    /**
     * 
     */
    @Override
    public int compareTo(JavadocObject that) {
        return this.getQualifiedNameWithSignature().compareTo( that.getQualifiedNameWithSignature() );
    }
    
    /**
     * 
     */
    @Override
    public String toJSONString() {
        return JSONValue.toJSONString(json);
    }
}
