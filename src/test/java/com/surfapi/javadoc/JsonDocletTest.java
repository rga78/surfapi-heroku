
package com.surfapi.javadoc;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Rule;
import org.junit.Test;

import com.sun.javadoc.MethodDoc;
import com.surfapi.junit.CaptureSystemOutRule;

/**
 *
 */
public class JsonDocletTest {

 
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

        // assertEquals("C:\\easy\\mysandbox\\javadoc-parser", System.getProperty("user.dir") );

        Pair<List<String>,List<String>> out = new JavadocProcessForTesting( new File("src/test/java/com/surfapi/test") ).run();
        
        // For manual verification...
        // FileUtils.write( new File("test.out"), StringUtils.join( out.getLeft(), "\n") );
        
        JSONArray doc = (JSONArray) new JSONParser().parse( "[" + StringUtils.join( out.getLeft(), "" ) + "]" );
        
        // The package is added last.
        assertFalse( doc.isEmpty() );
        assertEquals( 34, doc.size() );
        assertEquals( "package", ((JSONObject)doc.get(doc.size()-1)).get("metaType"));
        assertEquals( "com.surfapi.test", ((JSONObject)doc.get(doc.size()-1)).get("name"));
    }
    
    /**
     * 
     */
    @Test
    public void testGetInheritedCommentText() throws Exception {
        
        Mockery mockery = new JUnit4Mockery();
        
        final MethodDoc methodDoc1 = mockery.mock(MethodDoc.class, "methodDoc1");
        final MethodDoc methodDoc2 = mockery.mock(MethodDoc.class, "methodDoc2"); // parent
        
        mockery.checking(new Expectations() {
            {
                oneOf(methodDoc1).overriddenMethod();
                will(returnValue(methodDoc2));
                
                oneOf(methodDoc2).overriddenMethod();
                will(returnValue(null));
                
                oneOf(methodDoc1).commentText();
                will(returnValue("methodDoc1 commentText: {@inheritDoc}"));
                
                oneOf(methodDoc2).commentText();
                will(returnValue("methodDoc2 commentText"));
            }
        });

        
        assertEquals( "methodDoc1 commentText: methodDoc2 commentText",
                      new JsonDoclet(null).getInheritedCommentText(methodDoc1) );

    }

}


