package com.surfapi.db.post;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.coll.Cawls;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.DB;
import com.surfapi.db.DBImpl;
import com.surfapi.db.DBLoader;
import com.surfapi.db.MongoDBImpl;
import com.surfapi.junit.CaptureSystemOutRule;
import com.surfapi.junit.DropMongoDBRule;
import com.surfapi.junit.MongoDBProcessRule;

/**
 * 
 */
public class ReferenceNameQueryTest {

    /**
     * Executed before and after the entire collection of tests (like @BeforeClass/@AfterClass).
     * 
     * Ensures a mongodb process is started.
     */
    @ClassRule
    public static MongoDBProcessRule mongoDBProcessRule = new MongoDBProcessRule();
    
    /**
     * Drops the given db before/after each test.
     */
    @Rule
    public DropMongoDBRule dropMongoDBRule = new DropMongoDBRule( mongoDBProcessRule, "test1" );

    /**
     * Capture and suppress stdout unless the test fails.
     */
    @Rule
    public CaptureSystemOutRule systemOutRule  = new CaptureSystemOutRule( );

    /**
     * 
     */
    @Test
    public void testBuildAndQuery() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        DB db = new MongoDBImpl("test1");
        new DBLoader().inject(db).loadFile( new File("src/test/resources/com.surfapi_1.0.json") )
                                 .loadFile( new File("src/test/resources/com.surfapi_0.9.json") );
        
        new ReferenceNameQuery().inject(db).buildIndex();
        
        List<Map> docs = new ReferenceNameQuery().inject(db).query( "com.surfapi.test.DemoJavadoc" );
        assertFalse( docs.isEmpty() );
        assertEquals( 2, docs.size() );
        
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/0.9/com.surfapi.test.DemoJavadoc") ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc") ) );
        
//        for (Map doc : docs) {
//            Log.trace(this, "testBasic: " + doc.get("_id"));
//            assertEquals( "com.surfapi.test.DemoJavadoc", doc.get("qualifiedName") );
//            assertEquals( "class", doc.get("metaType") );
//        }
        
        docs = new ReferenceNameQuery().inject(db).query( "com.surfapi.test.DemoJavadoc+DemoJavadoc" );
        
        assertFalse( docs.isEmpty() );
        assertEquals( 3, docs.size() );
        
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/0.9/com.surfapi.test.DemoJavadoc(java.lang.String)") ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc()") ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc(java.lang.String)") ) );
        
//        for (Map doc : docs) {
//            Log.trace(this, "testBasic: " + doc.get("_id"));
//            assertEquals( "com.surfapi.test.DemoJavadoc", doc.get("qualifiedName") );
//            assertEquals( "constructor", doc.get("metaType") );
//        }
        
        docs = new ReferenceNameQuery().inject(db).query( "com.surfapi.test.DemoJavadoc+parse" );
        
        assertFalse( docs.isEmpty() );
        assertEquals( 2, docs.size() );
        
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/0.9/com.surfapi.test.DemoJavadoc.parse(java.net.URL,java.util.List)") ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc.parse(java.net.URL,java.util.List)") ) );
        
//        
//        for (Map doc : docs) {
//            Log.trace(this, "testBasic: " + doc.get("_id"));
//            assertEquals( "com.surfapi.test.DemoJavadoc.parse", doc.get("qualifiedName") );
//            assertEquals( "method", doc.get("metaType") );
//        }
        
    }

    
    /**
     * 
     */
    @Test
    public void testGetReferenceName() {
        
        Map packageModel = new MapBuilder().append("metaType", "package") 
                                            .append("name", "com.surfapi.test") ;
        
        Map classModel =  new MapBuilder().append("metaType", "class") 
                                          .append("name", "DemoJavadoc") 
                                          .append("qualifiedName", "com.surfapi.test.DemoJavadoc") ;
        
        Map methodModel =  new MapBuilder().append("metaType", "method") 
                                           .append("name", "parse") 
                                           .append("qualifiedName", "com.surfapi.test.DemoJavadoc.parse")
                                           .append("containingClass", classModel);
        
        Map ctorModel =  new MapBuilder().append("metaType", "constructor") 
                                           .append("name", "DemoJavadoc") 
                                           .append("qualifiedName", "com.surfapi.test.DemoJavadoc")
                                           .append("containingClass", classModel);
        
        assertEquals("com.surfapi.test", new ReferenceNameQuery.IndexBuilder().getReferenceName( packageModel ) );
        
        assertEquals("com.surfapi.test.DemoJavadoc", new ReferenceNameQuery.IndexBuilder().getReferenceName( classModel ) );
        
        assertEquals("com.surfapi.test.DemoJavadoc+parse", new ReferenceNameQuery.IndexBuilder().getReferenceName( methodModel ) );
        
        assertEquals("com.surfapi.test.DemoJavadoc+DemoJavadoc", new ReferenceNameQuery.IndexBuilder().getReferenceName( ctorModel ) );
    }
    
    
    /**
     * 
     */
    @Test
    public void testQueryOne() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        DB db = new MongoDBImpl("test1");
        new DBLoader().inject(db).loadFile( new File("src/test/resources/com.surfapi_1.0.json") )
                                 .loadFile( new File("src/test/resources/com.surfapi_0.9.json") );
        
        new ReferenceNameQuery().inject(db).buildIndex();
        
        List<Map> docs = new ReferenceNameQuery().inject(db).query( "com.surfapi.test.DemoJavadoc" );
        assertFalse( docs.isEmpty() );
        assertEquals( 2, docs.size() );
        
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/0.9/com.surfapi.test.DemoJavadoc") ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc") ) );
        
        Map doc = new ReferenceNameQuery().inject(db).queryOne( "com.surfapi.test.DemoJavadoc", "/java/com.surfapi/1.0" );
        assertEquals( "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc", doc.get("_id") );
        
        doc = new ReferenceNameQuery().inject(db).queryOne( "com.surfapi.test.DemoJavadoc", "/java/com.surfapi/0.9" );
        assertEquals( "/java/com.surfapi/0.9/com.surfapi.test.DemoJavadoc", doc.get("_id") );
        
        doc = new ReferenceNameQuery().inject(db).queryOne( "com.surfapi.test.DemoJavadoc", "/java/com.surfapi/9.9.9" );
        assertEquals( docs.get(0), doc);     // It should match the first one (assuming mongo always returns in the same order).
        
    }
    
    /**
     * 
     */
    @Test
    public void testLookupSuperclassDoc() throws Exception {
        
        DB db = new DBImpl();
        new DBLoader().inject(db).loadFile( new File("src/test/resources/com.surfapi_1.0.json") )
                                 .loadFile( new File("src/test/resources/com.surfapi_0.9.json") );
        
        String libraryId = "/java/com.surfapi/1.0";
        Map doc = db.read(libraryId + "/com.surfapi.test.DemoJavadocSubClass");
        assertNotNull(doc);

        Map superclassDoc = new ReferenceNameQuery().inject(db).buildIndex().lookupSuperclassDoc(doc);
        
        assertEquals( libraryId + "/com.surfapi.test.DemoJavadoc", superclassDoc.get("_id"));
    }
        
}

