package com.surfapi.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.surfapi.coll.MapBuilder;

/**
 * 
 */
public class JavadocObjectTest {
    
    /**
     * Sample javadoc objects for testing.
     */
    private static Map methodDoc;
    private static Map classDoc;
    private static Map packageDoc;

    /**
     * Set up some example javadoc json objects for testing.
     */
    @BeforeClass
    public static void beforeClass() {
        
        Map library = new MapBuilder()
                                .append("_id", "/java/com.surfapi.test/1.0.SNAPSHOT")
                                .append("lang", "java")
                                .append("name", "com.surfapi.test")
                                .append("version", "1.0.SNAPSHOT");

        packageDoc = new MapBuilder()
                            .append("metaType", "package")
                            .append("name", "com.surfapi.test");

        classDoc = new MapBuilder()
                            .append("qualifiedName", "com.surfapi.test.DemoJavadoc")
                            .append("metaType", "class")
                            .append("name", "DemoJavadoc")
                            .append("containingPackage", packageDoc);

        methodDoc = new MapBuilder()
                            .append("qualifiedName", "com.surfapi.test.DemoJavadoc.parse")
                            .append("metaType", "method")
                            .append("name", "parse")
                            .append("flatSignature", "(URL, List<T>)")
                            .append("signature", "(java.net.URL, java.util.List<T>)")
                            .append("containingClass", classDoc)
                            .append("containingPackage", packageDoc);
    }

    /**
     *
     */
    @Test
    public void testCtorWithLibraryId() {
        
        String libraryId = "/java/com.surfapi.test/1.0";

        JavadocObject docObject = new JavadocObject(libraryId, methodDoc);

        assertEquals( libraryId, docObject.getLibraryId());
        assertEquals( libraryId + "/" + "com.surfapi.test.DemoJavadoc.parse(URL, List<T>)", docObject.getId());
        assertEquals( "com.surfapi.test.DemoJavadoc.parse(URL, List<T>)", docObject.getQualifiedNameWithSignature());
        
        // Verify the _library map was parsed and populated.
        Map lib = (Map) docObject.getJson().get("_library");
        assertEquals( "java", lib.get("lang"));
        assertEquals( "com.surfapi.test", lib.get("name"));
        assertEquals( "1.0", lib.get("version"));
        assertEquals( libraryId, lib.get("_id"));
    }

    /**
     *
     */
    @Test
    public void testGetId() {
        
        String libraryId = "/java/com.surfapi.test/1.2";
        JavadocObject docObject = new JavadocObject(libraryId, methodDoc);

        assertEquals( "/java/com.surfapi.test/1.2", docObject.getLibraryId());
        assertEquals( "/java/com.surfapi.test/1.2/com.surfapi.test.DemoJavadoc.parse(URL, List<T>)", docObject.getId());
        
    }

    /**
     *
     */
    @Test
    public void testGetQualifiedNameWithSignature() {
        String libraryId = "/java/com.surfapi.test/1.2";
        JavadocObject docObject = new JavadocObject(libraryId, methodDoc);

        assertEquals( "com.surfapi.test.DemoJavadoc.parse(URL, List<T>)", docObject.getQualifiedNameWithSignature());
        
        docObject = new JavadocObject(libraryId, classDoc);
        assertEquals( "com.surfapi.test.DemoJavadoc", docObject.getQualifiedNameWithSignature());
    }

    /**
     *
     */
    @Test
    public void testGetQualifiedName() {

        String libraryId = "/java/com.surfapi.test/1.2";
        assertEquals( "com.surfapi.test.DemoJavadoc.parse", new JavadocObject(libraryId, methodDoc).getQualifiedName() );

        assertEquals( "com.surfapi.test.DemoJavadoc", new JavadocObject(libraryId, classDoc).getQualifiedName() );

        assertEquals( "com.surfapi.test", new JavadocObject(libraryId, packageDoc).getQualifiedName() );
    }

        
    /**
     *
     */
    @Test
    public void testIsClass() {

        assertFalse( new JavadocObject(methodDoc).isClass() );

        assertTrue( new JavadocObject(classDoc).isClass() );

        assertFalse( new JavadocObject(packageDoc).isClass() );
    }


    /**
     *
     */
    @Test
    public void testIsPackage() {
        assertFalse( new JavadocObject(methodDoc).isPackage() );

        assertFalse( new JavadocObject(classDoc).isPackage() );

        assertTrue( new JavadocObject(packageDoc).isPackage() );
    }


    /**
     *
     */
    @Test
    public void testCompareTo() {
        assertTrue( new JavadocObject(methodDoc).compareTo( new JavadocObject(methodDoc) ) == 0 );
        assertFalse( new JavadocObject(methodDoc).compareTo( new JavadocObject(classDoc) ) == 0 );
        assertFalse( new JavadocObject(methodDoc).compareTo( new JavadocObject(packageDoc) ) == 0 );
        assertFalse( new JavadocObject(classDoc).compareTo( new JavadocObject(packageDoc) ) == 0 );
    }
}


