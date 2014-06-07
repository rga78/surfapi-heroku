package com.surfapi.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;

/**
 * 
 */
public class LibraryUtilsTest {
    
    /**
     * 
     */
    @Test
    public void testLatestVersionsOnly() {
        
        Map lib1 = JavadocMapUtils.mapLibraryId( "/java/com.surfapi/1.0" );
        Map lib2 = JavadocMapUtils.mapLibraryId( "/java/com.surfapi/1.1" );
        Map lib3 = JavadocMapUtils.mapLibraryId( "/java/com.surfapi/0.9" );
        Map lib4 = JavadocMapUtils.mapLibraryId( "/java/java-sdk/1.6" );
        Map lib5 = JavadocMapUtils.mapLibraryId( "/java/java-sdk/1.7" );

        Collection<Map> latest = LibraryUtils.latestVersionsOnly( Arrays.asList( lib1, lib2, lib3, lib4, lib5) );

        assertEquals( 2, latest.size() );

        assertNotNull( Cawls.findFirst( latest, new MapBuilder().append( "_id", "/java/com.surfapi/1.1" ) ) );
        assertNotNull( Cawls.findFirst( latest, new MapBuilder().append( "_id", "/java/java-sdk/1.7" ) ) );
    }  
    
    /**
     * 
     */
    @Test
    public void testLibraryCompareVersion() {
        
        Map lib1 = new MapBuilder().append("lang", "java")
                                   .append("name", "lib1")
                                   .append("version", "1.0");
        
        Map lib2 = new MapBuilder().append("lang", "java")
                .append("name", "lib2")
                .append("version", "1.1");
        
        Map lib3 = new MapBuilder().append("lang", "java")
                .append("name", "lib3")
                .append("version", "1.0");
        
        assertTrue( LibraryUtils.libraryCompareVersion(lib1, lib2) < 0 );
        assertTrue( LibraryUtils.libraryCompareVersion(lib1, lib3) == 0 );
        assertTrue( LibraryUtils.libraryCompareVersion(lib2, lib3) > 0 );
    }
    
    /**
     * 
     */
    @Test
    public void testLibraryEqualsIgnoreVersion() {
        
        Map lib1 = new MapBuilder().append("lang", "java")
                                   .append("name", "lib1")
                                   .append("version", "1.0");
        
        Map lib2 = new MapBuilder().append("lang", "java")
                .append("name", "lib2")
                .append("version", "1.1");
        
        Map lib3 = new MapBuilder().append("lang", "java")
                .append("name", "lib3")
                .append("version", "1.0");
        
        Map lib1a = new MapBuilder().append("lang", "java")
                .append("name", "lib1")
                .append("version", "2.0");
        
        assertTrue( LibraryUtils.libraryEqualsIgnoreVersion(lib1, lib1a) );
        assertFalse( LibraryUtils.libraryEqualsIgnoreVersion(lib1, lib2) );
        assertFalse( LibraryUtils.libraryEqualsIgnoreVersion(lib1, lib3) );
        assertFalse( LibraryUtils.libraryEqualsIgnoreVersion(lib2, lib3) );
    }
    
    /**
     * 
     */
    @Test
    public void testFilterOnLibraryId() {
        
        // Build some libraries.
        Map lib1 = JavadocMapUtils.mapLibraryId( "/java/com.surfapi/1.0" );
        Map lib2 = JavadocMapUtils.mapLibraryId( "/java/com.surfapi/0.9" );
        Map lib3 = JavadocMapUtils.mapLibraryId( "/java/java-sdk/1.6" );
        
        // Build some documents for those libraries.
        Map doc1 = new MapBuilder().append("_id", "doc1").append( JavadocMapUtils.LibraryFieldName, lib1 );
        Map doc2 = new MapBuilder().append("_id", "doc2").append( JavadocMapUtils.LibraryFieldName, lib2 );
        Map doc3 = new MapBuilder().append("_id", "doc3").append( JavadocMapUtils.LibraryFieldName, lib3 );
        Map doc4 = new MapBuilder().append("_id", "doc4").append( JavadocMapUtils.LibraryFieldName, lib1 );
        

        Collection<Map> docs =  Arrays.asList( doc1, doc2, doc3, doc4 );
        List<Map> filteredDocs = LibraryUtils.filterOnLibrary(docs, "/java/com.surfapi/1.0");

        assertEquals( 2, filteredDocs.size() );

        assertNotNull( Cawls.findFirst( filteredDocs, doc1) );
        assertNotNull( Cawls.findFirst( filteredDocs, doc4) );
        
        filteredDocs = LibraryUtils.filterOnLibrary(docs, "/java/com.surfapi/9.9.9");
        assertTrue( filteredDocs.isEmpty() );
    }  
    
    /**
     * 
     */
    @Test
    public void testFilterOnLibraryIdEmptyOrNullParms() {
        
       
        List<Map> filteredDocs = LibraryUtils.filterOnLibrary(null, "/java/com.surfapi/1.0");
        assertEquals( 0, filteredDocs.size() );
        
        filteredDocs = LibraryUtils.filterOnLibrary(new ArrayList<Map>(), "/java/com.surfapi/1.0");
        assertEquals( 0, filteredDocs.size() );

    }  
    

}