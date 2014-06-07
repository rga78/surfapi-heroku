package com.surfapi.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.surfapi.coll.ListBuilder;
import com.surfapi.coll.MapBuilder;

/**
 *
 */
public class JavadocMapUtilsTest {

    
    /**
     * 
     */
    @Test
    public void testTypeEquals() {
        
        Map childType1 = new MapBuilder().append("qualifiedTypeName", "java.util.List");
        Map childType2 = new MapBuilder().append("qualifiedTypeName", "E");

        Map parentType1 = new MapBuilder().append("qualifiedTypeName", "java.util.List");
        Map parentType2 = new MapBuilder().append("qualifiedTypeName", "java.util.Map");
        Map parentType3 = new MapBuilder().append("qualifiedTypeName", "T");
        
        assertTrue( JavadocMapUtils.typeEquals( childType1, parentType1 ) );
        assertFalse( JavadocMapUtils.typeEquals( childType1, parentType2 ) );
        assertTrue( JavadocMapUtils.typeEquals( childType1, parentType3 ) );
        assertFalse( JavadocMapUtils.typeEquals( childType2, parentType1 ) );

        assertFalse( JavadocMapUtils.typeEquals( childType2, null) );
        assertFalse( JavadocMapUtils.typeEquals( null, parentType2 ) );
        assertTrue( JavadocMapUtils.typeEquals( null, null) );
    }

    /**
     *
     */
    @Test
    public void testMethodParmsEquals() {

        Map childType1 = new MapBuilder().append("type", new MapBuilder().append("qualifiedTypeName", "java.util.List") );
        Map parentType1 = new MapBuilder().append("type", new MapBuilder().append("qualifiedTypeName", "java.util.List") );

        List<Map> childParms = (List<Map>) new ListBuilder().append( childType1 );
        List<Map> parentParms = (List<Map>) new ListBuilder().append( parentType1 );

        assertTrue( JavadocMapUtils.methodParmsEquals( childParms, parentParms ) );

        // With template type
        Map parentType2 = new MapBuilder().append("type", new MapBuilder().append("qualifiedTypeName", "T") );
        parentParms = (List<Map>) new ListBuilder().append( parentType2 );

        assertTrue( JavadocMapUtils.methodParmsEquals( childParms, parentParms ) );

        // Different num parms.
        parentParms = (List<Map>) new ListBuilder().append(parentType1).append(parentType2);

        assertFalse( JavadocMapUtils.methodParmsEquals( childParms, parentParms ) );

        // Same num parms, with template type.
        childParms = (List<Map>) new ListBuilder().append( childType1 ).append(childType1);
        parentParms = (List<Map>) new ListBuilder().append( parentType1).append(parentType2);

        assertTrue( JavadocMapUtils.methodParmsEquals( childParms, parentParms ) );

        // Same num, Different types
        Map parentType3 = new MapBuilder().append("type", new MapBuilder().append("qualifiedTypeName", "java.util.Map") );
        childParms = (List<Map>) new ListBuilder().append( childType1 );
        parentParms = (List<Map>) new ListBuilder().append( parentType3);

        assertFalse( JavadocMapUtils.methodParmsEquals( childParms, parentParms ) );
    }

    /**
     *
     */
    @Test
    public void testMethodEquals() {

        Map childType1 = new MapBuilder().append("type", new MapBuilder().append("qualifiedTypeName", "java.util.List") );
        Map parentType1 = new MapBuilder().append("type", new MapBuilder().append("qualifiedTypeName", "java.util.List") );

        List<Map> childParms = (List<Map>) new ListBuilder().append( childType1 );
        List<Map> parentParms = (List<Map>) new ListBuilder().append( parentType1 );

        Map childMethod = new MapBuilder().append("name", "parse")
                                          .append("parameters", childParms);
        Map parentMethod = new MapBuilder().append("name", "parse")
                                           .append("parameters", parentParms);

        assertTrue( JavadocMapUtils.methodEquals( childMethod, parentMethod ) );

        // Different name
        parentMethod = new MapBuilder().append("name", "parse1")
                                       .append("parameters", parentParms);
        assertFalse( JavadocMapUtils.methodEquals( childMethod, parentMethod ) );

        // With template type
        Map parentType2 = new MapBuilder().append("type", new MapBuilder().append("qualifiedTypeName", "T") );
        parentParms = (List<Map>) new ListBuilder().append( parentType2 );
        parentMethod = new MapBuilder().append("name", "parse")
                                       .append("parameters", parentParms);

        assertTrue( JavadocMapUtils.methodEquals( childMethod, parentMethod ) );

        // Different num parms.
        parentParms = (List<Map>) new ListBuilder().append(parentType1).append(parentType2);
        parentMethod = new MapBuilder().append("name", "parse")
                                       .append("parameters", parentParms);

        assertFalse( JavadocMapUtils.methodEquals( childMethod, parentMethod ) );

        // Same num parms, with template type.
        childParms = (List<Map>) new ListBuilder().append( childType1 ).append(childType1);
        parentParms = (List<Map>) new ListBuilder().append( parentType1).append(parentType2);

        childMethod = new MapBuilder().append("name", "parse")
                                      .append("parameters", childParms);
        parentMethod = new MapBuilder().append("name", "parse")
                                       .append("parameters", parentParms);

        assertTrue( JavadocMapUtils.methodEquals( childMethod, parentMethod ) );

        // Same num, Different types
        Map parentType3 = new MapBuilder().append("type", new MapBuilder().append("qualifiedTypeName", "java.util.Map") );
        childParms = (List<Map>) new ListBuilder().append( childType1 ).append(childType1);
        parentParms = (List<Map>) new ListBuilder().append( parentType1).append(parentType3);

        childMethod = new MapBuilder().append("name", "parse")
                                      .append("parameters", childParms);
        parentMethod = new MapBuilder().append("name", "parse")
                                       .append("parameters", parentParms);

        assertFalse( JavadocMapUtils.methodEquals( childMethod, parentMethod ) );
    }
    
    
    /**
     * 
     */
    @Test
    public void testMapLibraryId() {
        
        String libraryId = "/java/com.surfapi.test/1.2.SNAPSHOT";
        
        // Verify the _library map was parsed and populated.
        Map lib = JavadocMapUtils.mapLibraryId( libraryId );
        
        assertEquals( "java", lib.get("lang"));
        assertEquals( "com.surfapi.test", lib.get("name"));
        assertEquals( "1.2.SNAPSHOT", lib.get("version"));
        assertEquals( libraryId, lib.get("_id"));
    }
    
    
    /**
     * 
     */
    @Test
    public void testGetParameterSignature() {
        
        Map parmType1 = new MapBuilder().append("qualifiedTypeName", "com.surfapi.test.DemoJavadoc")
                                        .append("dimension", "");
        Map parmType2 = new MapBuilder().append("qualifiedTypeName", "java.lang.String")
                                        .append("dimension", "[]");
        Map parmType3 = new MapBuilder().append("qualifiedTypeName", "int")
                                        .append("dimension", "");
        Map parmType4 = new MapBuilder().append("qualifiedTypeName", "T")
                                        .append("dimension", "");
        
        List<Map> parms = new ListBuilder<Map>()
                                .append( new MapBuilder().append("type", parmType1) )
                                .append( new MapBuilder().append("type", parmType2) )
                                .append( new MapBuilder().append("type", parmType3) )
                                .append( new MapBuilder().append("type", parmType4) );
                                        
        Map doc = new MapBuilder().append("parameters", parms);
        
        String expectedSignature = "(com.surfapi.test.DemoJavadoc,java.lang.String[],int,T)";
        assertEquals( expectedSignature, JavadocMapUtils.getParameterSignature(doc)) ;
    }

    
    /**
     * 
     */
    @Test
    public void testGetQualifiedName() {
        
        Map doc1 = new MapBuilder().append("qualifiedTypeName", "com.surfapi.test.DemoJavadoc");
        Map doc2 = new MapBuilder().append("qualifiedName", "com.surfapi.test.DemoJavadoc");
        Map doc3 = new MapBuilder().append("name", "com.surfapi.test.DemoJavadoc");
        Map doc4 = new MapBuilder().append("name", "com.surfapi.test")
                                   .append("metaType", "package");
        
        assertEquals( "com.surfapi.test.DemoJavadoc", JavadocMapUtils.getQualifiedName(doc1)) ;
        assertEquals( "com.surfapi.test.DemoJavadoc", JavadocMapUtils.getQualifiedName(doc2)) ;
        assertNull( JavadocMapUtils.getQualifiedName(doc3)) ;
        assertEquals( "com.surfapi.test", JavadocMapUtils.getQualifiedName(doc4)) ;
        assertNull( JavadocMapUtils.getQualifiedName(null)) ;
    }
}
