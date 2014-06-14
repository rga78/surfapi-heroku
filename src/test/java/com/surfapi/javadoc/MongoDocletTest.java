package com.surfapi.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.MongoDBImpl;
import com.surfapi.json.JSONTrace;
import com.surfapi.junit.CaptureSystemOutRule;
import com.surfapi.junit.DropMongoDBRule;
import com.surfapi.junit.MongoDBProcessRule;
import com.surfapi.log.Log;

public class MongoDocletTest {
    
    /**
     * Executed before and after the entire collection of tests (like @BeforeClass/@AfterClass).
     * 
     * Ensures a mongodb process is started.
     */
    @ClassRule
    public static MongoDBProcessRule mongoDBProcessRule = new MongoDBProcessRule();
    
    /**
     * Drops the given db before/after the entire collection of tests.
     */
    @ClassRule
    public static DropMongoDBRule dropMongoDBRule = new DropMongoDBRule( mongoDBProcessRule, "test1" );
    
    /**
     * Capture and suppress stdout unless the test fails.
     */
    @Rule
    public CaptureSystemOutRule systemOutRule  = new CaptureSystemOutRule( );

    
    /**
     * 
     */
    @BeforeClass
    public static void beforeClass() throws Exception {
        // Setup the db.
        
        String dbName = "test1";
        String libraryId = "/java/com.surfapi.test/1.0";
        
        File baseDir = new File("src/test/java/com/surfapi/test");
        MongoJavadocProcess javadocProcess = new MongoJavadocProcess(baseDir)
                                                    .setDocletPath( JavadocMain.buildDocletPath() )
                                                    .setDirFilter( TrueFileFilter.INSTANCE )
                                                    .setMongoDBName( dbName )
                                                    .setLibraryId(libraryId );
        javadocProcess.run();
    }
    

    /**
     * 
     */
    @Test
    public void testJavadoc() throws Exception {
        
        String dbName = "test1";
        String libraryId = "/java/com.surfapi.test/1.0";
        
        List<Map> docs = new MongoDBImpl(dbName).find( libraryId, new MapBuilder());
        
        assertFalse( docs.isEmpty() );
        assertEquals( 34, docs.size() );
        
        for (Map doc : docs) {
            Log.trace(this,"testJavadoc: " + doc.get("name") + ": " + doc.get("_id") );
        }
        
    }
    
    /**
     * 
     */
    @Test
    public void testInheritedDoc() throws Exception {
        
        String dbName = "test1";
        String libraryId = "/java/com.surfapi.test/1.0";
       
        Map doc = new MongoDBImpl(dbName).read( "/java/com.surfapi.test/1.0/com.surfapi.test.DemoJavadoc.parse(java.net.URL,java.util.List)" );
        assertNotNull(doc);
        
        Map subDoc = new MongoDBImpl(dbName).read( "/java/com.surfapi.test/1.0/com.surfapi.test.DemoJavadocSubClass.parse(java.net.URL,java.util.List)" );
        assertNotNull(subDoc);
        
        // Test inherited doc
        assertFalse( StringUtils.isEmpty( (String) doc.get("commentText") ) );
        assertEquals( doc.get("commentText"), subDoc.get("commentText") );
        
        assertFalse( ((List)doc.get("paramTags")).isEmpty() );
        assertEquals( doc.get("paramTags"), subDoc.get("paramTags") );
        
        assertNotNull( Cawls.findFirst( (List<Map>) doc.get("tags"), new MapBuilder().append( "name", "@return" ) ) );
        assertNotNull( Cawls.findFirst( (List<Map>) subDoc.get("tags"), new MapBuilder().append( "name", "@return" ) ) );
    }
    
    
    /**
     * 
     */
    @Test
    public void testInlineInheritedDoc() throws Exception {
        
        String dbName = "test1";
        String libraryId = "/java/com.surfapi.test/1.0";
        
        Map subDoc = new MongoDBImpl(dbName).read( "/java/com.surfapi.test/1.0/com.surfapi.test.DemoJavadocSubClass.someAbstractMethod(java.lang.String[])" );
        assertNotNull(subDoc);
        
        Log.trace( JSONTrace.prettyPrint(subDoc) );
        
        assertEquals( "Overridden method, inherit doc: " + "This is an abstract method.",
                      subDoc.get("commentText") );
        
        assertEquals( "inherit doc: " + "an array of strings.",
                      ((List<Map>)subDoc.get("paramTags")).get(0).get("parameterComment"));
        
        Map returnTag = Cawls.findFirst( (List<Map>) subDoc.get("tags"), new MapBuilder().append( "name", "@return" ) );
        assertNotNull(returnTag);
        assertEquals( "inherit doc: " + "an int", returnTag.get("text"));
    }
    
    
    /**
     * 
     */
    // TODO: @Test
    public void testAllSuperclassTypes() throws Exception {
        
        String dbName = "test1";
        String libraryId = "/java/com.surfapi.test/1.0";
        
        Map doc = new MongoDBImpl(dbName).read( "/java/com.surfapi.test/1.0/com.surfapi.test.DemoJavadocSubClass2" );
        assertNotNull(doc);
        
        Log.trace( JSONTrace.prettyPrint(doc) );
        
        List<Map> superclassTypes = (List<Map>) doc.get("allSuperclassTypes");
        assertEquals( 3, superclassTypes.size() );
        
        Map superclassType0 = superclassTypes.get(0);
        assertEquals( "com.surfapi.test.DemoJavadocSubClass", superclassType0.get("qualifiedTypeName"));
        
        Map superclassType1 = superclassTypes.get(1);
        assertEquals( "com.surfapi.test.DemoJavadoc", superclassType1.get("qualifiedTypeName"));
        
        Map superclassType2 = superclassTypes.get(2);
        assertEquals( "java.lang.Object", superclassType2.get("qualifiedTypeName"));
      
    }
    
    /**
     * 
     */
    // TODO: @Test
    public void testAllInterfaceTypes() throws Exception {
        
        String dbName = "test1";
        String libraryId = "/java/com.surfapi.test/1.0";
        
        Map doc = new MongoDBImpl(dbName).read( "/java/com.surfapi.test/1.0/com.surfapi.test.DemoJavadocSubClass2" );
        assertNotNull(doc);
        
        Log.trace( JSONTrace.prettyPrint(doc) );
        
        List<Map> interfaceTypes = (List<Map>) doc.get("allInterfaceTypes");
        assertEquals( 3, interfaceTypes.size() );
        
        Map interfaceType0 = interfaceTypes.get(0);
        assertEquals( "java.io.Serializable", interfaceType0.get("qualifiedTypeName"));
        
        Map interfaceType1 = interfaceTypes.get(1);
        assertEquals( "java.util.concurrent.Callable", interfaceType1.get("qualifiedTypeName"));
        
        Map interfaceType2 = interfaceTypes.get(2);
        assertEquals( "com.surfapi.test.DemoInterface", interfaceType2.get("qualifiedTypeName"));
      
    }
    
    /**
     * 
     */
    // TODO: @Test
    public void testAllInheritedMethods() throws Exception {
        
        String dbName = "test1";
        String libraryId = "/java/com.surfapi.test/1.0";
        
        Map doc = new MongoDBImpl(dbName).read( "/java/com.surfapi.test/1.0/com.surfapi.test.DemoJavadocSubClass2" );
        assertNotNull(doc);
        
        Log.trace( JSONTrace.prettyPrint((List<Map>) doc.get("allInheritedMethods") ) );
        
        List<Map> allInheritedMethods = (List<Map>) doc.get("allInheritedMethods");
        assertEquals( 3, allInheritedMethods.size() );
        
        {
            Map inheritedMethodsMap0 = allInheritedMethods.get(0);
            Map superclassType0 = (Map) inheritedMethodsMap0.get("superclassType");
            assertEquals( "com.surfapi.test.DemoJavadocSubClass", superclassType0.get("qualifiedTypeName"));

            List<Map> inheritedMethods0 = (List<Map>) inheritedMethodsMap0.get("inheritedMethods");
            List<String> expectedMethodNames0 = Arrays.asList( "call", "interfaceMethod", "parse", "someAbstractMethod");
            assertEquals( expectedMethodNames0.size(), inheritedMethods0.size() );
            for (int i=0; i < expectedMethodNames0.size(); ++i) {
                assertNotNull( Cawls.findFirst( inheritedMethods0, new MapBuilder().append("name",expectedMethodNames0.get(i) ) ) );
                // assertEquals( expectedMethodNames0.get(i), inheritedMethods0.get(i).get("name"));
            }
        }

        {        
            Map inheritedMethodsMap1 = allInheritedMethods.get(1);
            Map superclassType1 = (Map) inheritedMethodsMap1.get("superclassType");
            assertEquals( "com.surfapi.test.DemoJavadoc", superclassType1.get("qualifiedTypeName"));

            List<Map> inheritedMethods1 = (List<Map>) inheritedMethodsMap1.get("inheritedMethods");
            List<String> expectedMethodNames1 = Arrays.asList( "call", "getAnnotation", "methodWithTypes", "someStaticMethod");
            assertEquals( expectedMethodNames1.size(), inheritedMethods1.size() );
            for (int i=0; i < expectedMethodNames1.size(); ++i) {
                assertNotNull( Cawls.findFirst( inheritedMethods1, new MapBuilder().append("name",expectedMethodNames1.get(i) ) ) );
                // assertEquals( expectedMethodNames1.get(i), inheritedMethods1.get(i).get("name"));
            }
        }
        
        {
            Map inheritedMethodsMap2 = allInheritedMethods.get(2);
            Map superclassType2 = (Map) inheritedMethodsMap2.get("superclassType");
            assertEquals( "java.lang.Object", superclassType2.get("qualifiedTypeName"));

            List<Map> inheritedMethods2 = (List<Map>) inheritedMethodsMap2.get("inheritedMethods");
            List<String> expectedMethodNames2 = Arrays.asList( "clone", "equals", "finalize", "getClass", "hashCode", "notify", "notifyAll", "toString", "wait", "wait", "wait");
            assertEquals( expectedMethodNames2.size(), inheritedMethods2.size() );
            for (int i=0; i < expectedMethodNames2.size(); ++i) {
                assertNotNull( Cawls.findFirst( inheritedMethods2, new MapBuilder().append("name",expectedMethodNames2.get(i) ) ) );
                // assertEquals( expectedMethodNames2.get(i), inheritedMethods2.get(i).get("name"));
            }
        }
      
    }
}
