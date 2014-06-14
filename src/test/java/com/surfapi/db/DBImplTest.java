package com.surfapi.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.json.JSONTrace;
import com.surfapi.junit.CaptureSystemOutRule;

/**
 * 
 */
public class DBImplTest {
    
    /**
     * Capture and suppress stdout unless the test fails.
     */
    @Rule
    public CaptureSystemOutRule systemOutRule  = new CaptureSystemOutRule( );
    
    /**
     * 
     */
    @Test
    public void test() throws Exception {
        
        File testJsonFile = new File("src/test/resources/DBTest.test_1.0.3.json");
        assertTrue(testJsonFile.exists());
        
        String libraryId = DBLoader.parseLibraryId( testJsonFile.getName() );
        assertEquals("/java/DBTest.test/1.0.3", libraryId) ;
        
        DB db = new DBImpl();
        new DBLoader().inject( db ).loadDir( new File("src/test/resources") );
        
        // System.out.println( DB._.toString() );
        
        String key = DBLoader.parseLibraryId( testJsonFile.getName() ) + "/com.surfapi.test.DemoJavadoc" ;
        
        Map obj = db.read( libraryId, key );
        Map obj2 = db.read(key);
        
        assertNotNull(obj);
        assertEquals(obj, obj2);
        
        assertEquals( "class", JavadocMapUtils.getMetaType(obj) );
        assertEquals( "com.surfapi.test.DemoJavadoc", JavadocMapUtils.getQualifiedName(obj));
        
        obj = db.read( libraryId, key + ".parse(java.net.URL,java.util.List)");
        
        assertNotNull(obj);
        assertEquals( "method", JavadocMapUtils.getMetaType(obj) );
        assertEquals( "com.surfapi.test.DemoJavadoc.parse", JavadocMapUtils.getQualifiedName(obj));
        
        // Verify that the library was added to the libraries collection
        List<Map> javaLibs = db.getLibraryList("java");
        Map libObj = Cawls.findFirst(javaLibs, new MapBuilder<String, String>().append("_id", libraryId)) ;
        assertNotNull(libObj);
        
        assertEquals(libraryId, libObj.get("_id"));
        assertEquals("java", libObj.get("lang"));
        assertEquals("DBTest.test", libObj.get("name"));
        assertEquals("1.0.3", libObj.get("version"));
        assertEquals("library", libObj.get("metaType"));
        assertNull(libObj.get("packages")); // The 'packages' field should have been excluded.
        
    }
    
    /**
     * 
     */
    @Test
    public void testLotsOfData() throws Exception {
        
        assumeTrue( Boolean.getBoolean("runFat") );
        
        File testJsonFile = new File("data/java-sdk_1.6.json");
        assertTrue(testJsonFile.exists());
        
        String libraryId = DBLoader.parseLibraryId( testJsonFile.getName() );
        assertEquals("/java/java-sdk/1.6",libraryId) ;
        
        DB db = new DBImpl();
        new DBLoader().inject( db ).loadDir( new File("data") );
        
        // System.out.println( DB._.toString() );
        
        String key = DBLoader.parseLibraryId( testJsonFile.getName() ) + "/" + String.class.getCanonicalName() ;
        
        Map obj = db.read( libraryId, key );
        
        assertNotNull(obj);
        assertEquals( "class", JavadocMapUtils.getMetaType(obj) );
        assertEquals( String.class.getCanonicalName(), JavadocMapUtils.getQualifiedName(obj));
        System.out.println( "-rx- found String: " + JSONTrace.prettyPrint( obj ));
        
        obj = db.read( libraryId, key + ".replace(char, char)");
        
        assertNotNull(obj);
        assertEquals( "method", JavadocMapUtils.getMetaType(obj) );
        assertEquals( String.class.getCanonicalName() + ".replace", JavadocMapUtils.getQualifiedName(obj));
        System.out.println( "-rx- found String.replace: " + JSONTrace.prettyPrint( obj ));
    }
    
    /**
     * 
     */        
    @Test
    public void testGetLibraryList() throws Exception {
        
        DBImpl db = new DBImpl();

        // Add 
        db.save( DB.LibraryCollectionName, new MapBuilder().append("lang", "java")
                                      .append("_id", "/java/java-sdk/1.6")
                                      .append("name", "java-sdk")
                                      .append("version", "1.6") );
        db.save( DB.LibraryCollectionName, new MapBuilder().append("lang", "scala")
                                      .append("_id", "/scala/scala-collection/1.6")
                                      .append("name", "scala-collection")
                                      .append("version", "1.6") );
        db.save(  DB.LibraryCollectionName, new MapBuilder().append("lang", "java")
                                      .append("_id", "/java/org.junit/4.11")
                                      .append("name", "org.junit")
                                      .append("version", "4.11") );
        
        List<Map> javaLibs = db.getLibraryList("java");
        
        assertNotNull( Cawls.findFirst(javaLibs, new MapBuilder<String, String>().append("_id", "/java/java-sdk/1.6")) );
        assertNotNull( Cawls.findFirst(javaLibs, new MapBuilder<String, String>().append("_id", "/java/org.junit/4.11")) );
        assertNull( Cawls.findFirst(javaLibs, new MapBuilder<String, String>().append("_id", "/scala/scala-collection/1.6")) );
        
    }
    
    /**
     * 
     */        
    @Test
    public void testGetLibraryIds() throws Exception {
        DBImpl db = new DBImpl();

        // Add 
        db.save( DB.LibraryCollectionName, new MapBuilder().append("lang", "java")
                                      .append("_id", "/java/java-sdk/1.6")
                                      .append("name", "java-sdk")
                                      .append("version", "1.6") );
        db.save( DB.LibraryCollectionName, new MapBuilder().append("lang", "scala")
                                      .append("_id", "/scala/scala-collection/1.6")
                                      .append("name", "scala-collection")
                                      .append("version", "1.6") );
        db.save(  DB.LibraryCollectionName, new MapBuilder().append("lang", "java")
                                      .append("_id", "/java/org.junit/4.11")
                                      .append("name", "org.junit")
                                      .append("version", "4.11") );
        
        List<String> javaLibs = db.getLibraryIds("java");
        
        assertTrue( javaLibs.contains( "/java/java-sdk/1.6" ) );
        assertFalse( javaLibs.contains( "/scala/scala-collection/1.6" ) );
        assertTrue( javaLibs.contains( "/java/org.junit/4.11" ) );
    }
    
    /**
     * 
     */
    @Test
    public void testParseCollectionName() {
        String _id = "/java/java-sdk/1.6/java.lang.String";
        assertEquals( "/java/java-sdk/1.6", new DBImpl().parseCollectionName(_id));
        assertEquals( "", new DBImpl().parseCollectionName("java.lang.String"));
    }
    
    /**
     * 
     */
    @Test(expected=RuntimeException.class)
    public void testValidateSaveEmptyCollection() {
        new DBImpl().validateSave("", new MapBuilder().append("_id", "non-null-id"));
    }

    /**
     * 
     */
    @Test(expected=RuntimeException.class)
    public void testValidateSaveNullObject() {
        new DBImpl().validateSave("non-null-collection", null);
    }
    
    /**
     * 
     */
    @Test(expected=RuntimeException.class)
    public void testValidateSaveMissingId() {
        new DBImpl().validateSave("non-null-collection", new MapBuilder().append("somekey", "somevalue"));
    }
}
