package com.surfapi.javadoc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.app.JavadocMapUtils;
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
     * For connecting to the mongodb service
     */
    public static final String MongoDbName = "test1";
    public static final String MongoUri = "mongodb://localhost/" + MongoDbName;
    
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
    public static DropMongoDBRule dropMongoDBRule = new DropMongoDBRule( mongoDBProcessRule, MongoDbName );
    
    /**
     * Capture and suppress stdout unless the test fails.
     */
    @Rule
    public CaptureSystemOutRule systemOutRule  = new CaptureSystemOutRule( );

    
    /**
     * Setup the db.
     */
    @BeforeClass
    public static void beforeClass() throws Exception {

        assumeTrue(mongoDBProcessRule.isStarted());
        
        String libraryId = "/java/com.surfapi.test/1.0";
        File baseDir = new File("src/test/java");
        
        SimpleJavadocProcess javadocProcess = new SimpleJavadocProcess()
                                                    .setMongoUri( MongoUri )
                                                    .setLibraryId( libraryId )
                                                    .setSourcePath( baseDir )
                                                    .setPackages( Arrays.asList( "com.surfapi.test" ) );
        javadocProcess.run();
    }
    
    /**
     * 
     */
    @Before
    public void before() {
        assumeTrue(mongoDBProcessRule.isStarted());
    }

    /**
     * 
     */
    @Test
    public void testJavadoc() throws Exception {
        
        String libraryId = "/java/com.surfapi.test/1.0";
        
        List<Map> docs = new MongoDBImpl(MongoDbName).find( libraryId, new MapBuilder());
        
        assertFalse( docs.isEmpty() );
        assertEquals( JsonDocletTest.ExpectedTestJavadocSize, docs.size() );
        
        for (Map doc : docs) {
            Log.trace(this,"testJavadoc: " + doc.get("name") + ": " + doc.get("_id") );
        }
        
    }
    
    /**
     * 
     */
    @Test
    public void testInheritedDoc() throws Exception {

        String libraryId = "/java/com.surfapi.test/1.0";
       
        Map doc = new MongoDBImpl(MongoDbName).read( "/java/com.surfapi.test/1.0/com.surfapi.test.DemoJavadoc.parse(java.net.URL,java.util.List)" );
        assertNotNull(doc);
        
        Map subDoc = new MongoDBImpl(MongoDbName).read( "/java/com.surfapi.test/1.0/com.surfapi.test.DemoJavadocSubClass.parse(java.net.URL,java.util.List)" );
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

        String libraryId = "/java/com.surfapi.test/1.0";
        
        Map subDoc = new MongoDBImpl(MongoDbName).read( "/java/com.surfapi.test/1.0/com.surfapi.test.DemoJavadocSubClass.someAbstractMethod(java.lang.String[])" );
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
    @Test
    public void testAllSuperclassTypes() throws Exception {
        
        String libraryId = "/java/com.surfapi.test/1.0";
        
        Map doc = new MongoDBImpl(MongoDbName).read( "/java/com.surfapi.test/1.0/com.surfapi.test.DemoJavadocSubClass2" );
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
    @Test
    public void testAllInterfaceTypes() throws Exception {
        
        String libraryId = "/java/com.surfapi.test/1.0";
        
        Map doc = new MongoDBImpl(MongoDbName).read( "/java/com.surfapi.test/1.0/com.surfapi.test.DemoJavadocSubClass2" );
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
    @Test
    public void testAllInheritedMethods() throws Exception {
        
        String libraryId = "/java/com.surfapi.test/1.0";
        
        Map doc = new MongoDBImpl(MongoDbName).read( "/java/com.surfapi.test/1.0/com.surfapi.test.DemoJavadocSubClass2" );
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
            List<String> expectedMethodNames1 = Arrays.asList( "call", "getAnnotation", "methodWithTypes", "someStaticMethod", "toString");
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
            List<String> expectedMethodNames2 = Arrays.asList( "clone", "equals", "finalize", "getClass", "hashCode", "notify", "notifyAll", "wait", "wait", "wait");
            assertEquals( expectedMethodNames2.size(), inheritedMethods2.size() );
            for (int i=0; i < expectedMethodNames2.size(); ++i) {
                assertNotNull( Cawls.findFirst( inheritedMethods2, new MapBuilder().append("name",expectedMethodNames2.get(i) ) ) );
                // assertEquals( expectedMethodNames2.get(i), inheritedMethods2.get(i).get("name"));
            }
        }
      
    }
    
    
    /**
     * 
     */
    @Test
    public void testSetStubIds() throws Exception {
        
        String dbName = MongoDbName;
        String libraryId = "/java/com.surfapi.test/1.0";
        
        Map doc = new MongoDBImpl(dbName).read( "/java/com.surfapi.test/1.0/com.surfapi.test.DemoJavadocSubClass2" );
        assertNotNull(doc);
        
        Log.trace( JSONTrace.prettyPrint(doc ) );
        
        List<Map> methods = (List<Map>) doc.get("methods");
        assertFalse( methods.isEmpty() );
        assertAllIdsAreSet(libraryId, methods);
        
        List<Map> ctors = (List<Map>) doc.get("constructors");
        assertFalse( ctors.isEmpty() );
        assertAllIdsAreSet(libraryId, ctors);
        
        Map pkg = (Map) doc.get("containingPackage");
        assertEquals( JavadocMapUtils.buildId(libraryId, pkg), pkg.get("_id") );
        
        // allInterfaceTypes...
        {
            List<Map> interfaceTypes = (List<Map>) doc.get("allInterfaceTypes");
            assertEquals( 3, interfaceTypes.size() );

            Map interfaceType0 = interfaceTypes.get(0);
            assertEquals( "java.io.Serializable", interfaceType0.get("qualifiedTypeName"));
            assertNull( interfaceType0.get("_id") ); // Not in same library.

            Map interfaceType1 = interfaceTypes.get(1);
            assertEquals( "java.util.concurrent.Callable", interfaceType1.get("qualifiedTypeName"));
            assertNull( interfaceType1.get("_id") ); // Not in same library.

            Map interfaceType2 = interfaceTypes.get(2);
            assertEquals( "com.surfapi.test.DemoInterface", interfaceType2.get("qualifiedTypeName"));
            assertEquals( JavadocMapUtils.buildId(libraryId, interfaceType2), interfaceType2.get("_id") );
        }
        
        // allSuperclassTypes...
        {
            List<Map> superclassTypes = (List<Map>) doc.get("allSuperclassTypes");
            assertEquals( 3, superclassTypes.size() );

            Map superclassType0 = superclassTypes.get(0);
            assertEquals( "com.surfapi.test.DemoJavadocSubClass", superclassType0.get("qualifiedTypeName"));
            assertEquals( JavadocMapUtils.buildId(libraryId, superclassType0), superclassType0.get("_id") );

            Map superclassType1 = superclassTypes.get(1);
            assertEquals( "com.surfapi.test.DemoJavadoc", superclassType1.get("qualifiedTypeName"));
            assertEquals( JavadocMapUtils.buildId(libraryId, superclassType1), superclassType1.get("_id") );

            Map superclassType2 = superclassTypes.get(2);
            assertEquals( "java.lang.Object", superclassType2.get("qualifiedTypeName"));
            assertNull( superclassType2.get("_id") ); // Not in same library.
        }
        
        // allInheritedMethods...
        List<Map> allInheritedMethods = (List<Map>) doc.get("allInheritedMethods");
        assertEquals( 3, allInheritedMethods.size() );
        
        {
            Map inheritedMethodsMap0 = allInheritedMethods.get(0);
            Map superclassType0 = (Map) inheritedMethodsMap0.get("superclassType");
            assertEquals( "com.surfapi.test.DemoJavadocSubClass", superclassType0.get("qualifiedTypeName"));
            assertEquals( JavadocMapUtils.buildId(libraryId, superclassType0), superclassType0.get("_id") );

            assertAllIdsAreSet(libraryId, (List<Map>) inheritedMethodsMap0.get("inheritedMethods") );
        }

        {        
            Map inheritedMethodsMap1 = allInheritedMethods.get(1);
            Map superclassType1 = (Map) inheritedMethodsMap1.get("superclassType");
            assertEquals( "com.surfapi.test.DemoJavadoc", superclassType1.get("qualifiedTypeName"));
            assertEquals( JavadocMapUtils.buildId(libraryId, superclassType1), superclassType1.get("_id") );

            assertAllIdsAreSet(libraryId, (List<Map>) inheritedMethodsMap1.get("inheritedMethods") );
        }
        
        {
            Map inheritedMethodsMap2 = allInheritedMethods.get(2);
            Map superclassType2 = (Map) inheritedMethodsMap2.get("superclassType");
            assertEquals( "java.lang.Object", superclassType2.get("qualifiedTypeName"));
            assertNull( superclassType2.get("_id") ); // Not same library.

            assertAllIdsAreNull((List<Map>) inheritedMethodsMap2.get("inheritedMethods") ); // Not same library.
        }
      
    }
    
    /**
     * 
     */
    @Test
    public void testSetStubIdOverriddenMethod() throws Exception {
        
        String dbName = MongoDbName;
        String libraryId = "/java/com.surfapi.test/1.0";
        
        Map doc = new MongoDBImpl(dbName).read( "/java/com.surfapi.test/1.0/com.surfapi.test.DemoJavadocSubClass.parse(java.net.URL,java.util.List)" );
        assertNotNull(doc);
        
        Map overriddenMethod = (Map) doc.get("overriddenMethod");
        assertEquals( JavadocMapUtils.buildId(libraryId, overriddenMethod), overriddenMethod.get("_id") );
    }
    
    /**
     * 
     */
    @Test
    public void testSpecifiedByMethod() throws Exception {
        
        String dbName = MongoDbName;
        String libraryId = "/java/com.surfapi.test/1.0";
        
        Map doc = new MongoDBImpl(dbName).read( "/java/com.surfapi.test/1.0/com.surfapi.test.DemoJavadocSubClass.interfaceMethod(java.lang.String)" );
        assertNotNull(doc);
        
        Log.trace( JSONTrace.prettyPrint(doc) );
        
        Map specifiedByMethod = (Map) doc.get("specifiedByMethod");
        assertNotNull( specifiedByMethod );
        
        assertEquals( "com.surfapi.test.DemoInterface.interfaceMethod", JavadocMapUtils.getQualifiedName(specifiedByMethod) );
        
        assertEquals( "Interface documentation for the method interfaceMethod.", doc.get("commentText") );
    }
        
    
    /**
     * 
     */
    @Test
    public void testAllInheritedMethodsInterface() throws Exception {
        
        String dbName = MongoDbName;
        String libraryId = "/java/com.surfapi.test/1.0";
        
        Map doc = new MongoDBImpl(dbName).read( "/java/com.surfapi.test/1.0/com.surfapi.test.DemoInterfaceSubIntf" );
        assertNotNull(doc);
        
        Log.trace( JSONTrace.prettyPrint((List<Map>) doc.get("allInheritedMethods") ) );
        
        List<Map> allInheritedMethods = (List<Map>) doc.get("allInheritedMethods");
        assertEquals( 2, allInheritedMethods.size() );
        
        {
            Map inheritedMethodsMap = allInheritedMethods.get(0);
            Map superclassType = (Map) inheritedMethodsMap.get("superclassType");
            assertEquals( "com.surfapi.test.DemoInterface", superclassType.get("qualifiedTypeName"));

            List<Map> inheritedMethods = (List<Map>) inheritedMethodsMap.get("inheritedMethods");
            List<String> expectedMethodNames = Arrays.asList( "interfaceMethod");
            assertEquals( expectedMethodNames.size(), inheritedMethods.size() );
            for (int i=0; i < expectedMethodNames.size(); ++i) {
                assertNotNull( Cawls.findFirst( inheritedMethods, new MapBuilder().append("name",expectedMethodNames.get(i) ) ) );
            }
        }
        
        {
            Map inheritedMethodsMap = allInheritedMethods.get(1);
            Map superclassType = (Map) inheritedMethodsMap.get("superclassType");
            assertEquals( "com.surfapi.test.DemoInterface2", superclassType.get("qualifiedTypeName"));

            List<Map> inheritedMethods = (List<Map>) inheritedMethodsMap.get("inheritedMethods");
            List<String> expectedMethodNames = Arrays.asList( "interfaceMethod2");
            assertEquals( expectedMethodNames.size(), inheritedMethods.size() );
            for (int i=0; i < expectedMethodNames.size(); ++i) {
                assertNotNull( Cawls.findFirst( inheritedMethods, new MapBuilder().append("name",expectedMethodNames.get(i) ) ) );
            }
        }
      
    }
    
    /**
     * 
     */
    @Test
    public void testAllSuperclassTypesForInterface() throws Exception {
        
        String dbName = MongoDbName;
        String libraryId = "/java/com.surfapi.test/1.0";
        
        Map doc = new MongoDBImpl(dbName).read( "/java/com.surfapi.test/1.0/com.surfapi.test.DemoInterfaceSubIntf" );
        assertNotNull(doc);
        
        List<Map> allSuperclassTypes = (List<Map>) doc.get("allSuperclassTypes");
        assertEquals( 2, allSuperclassTypes.size() );
        assertNotNull( Cawls.findFirst( allSuperclassTypes, new MapBuilder().append("qualifiedTypeName","com.surfapi.test.DemoInterface" ) ) );
        assertNotNull( Cawls.findFirst( allSuperclassTypes, new MapBuilder().append("qualifiedTypeName","com.surfapi.test.DemoInterface2" ) ) );
    }
    
    /**
     * Helper method, asserts all _ids are null.
     */
    private Collection<Map> assertAllIdsAreNull( Collection<Map> stubs) {
        for (Map stub : Cawls.safeIterable(stubs)) {
            assertNull( stub.get("_id") );
        }
        return stubs;
    }
    
    /**
     * Helper method, asserts all _ids are non null.
     */
    private Collection<Map> assertAllIdsAreSet( String libraryId, Collection<Map> stubs) {
        for (Map stub : stubs) {
            assertEquals( JavadocMapUtils.buildId(libraryId, stub), stub.get("_id") );
        }
        return stubs;
    }
}
