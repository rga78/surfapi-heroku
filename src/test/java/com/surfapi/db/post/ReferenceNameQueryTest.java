package com.surfapi.db.post;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import com.surfapi.coll.Cawls;
import com.surfapi.coll.ListBuilder;
import com.surfapi.coll.MapBuilder;
import com.surfapi.coll.SetBuilder;
import com.surfapi.db.DB;
import com.surfapi.db.DBImpl;
import com.surfapi.db.DBLoader;
import com.surfapi.db.MongoDBImpl;
import com.surfapi.javadoc.SimpleJavadocProcess;
import com.surfapi.junit.CaptureSystemOutRule;
import com.surfapi.junit.DropMongoDBRule;
import com.surfapi.junit.MongoDBProcessRule;
import com.surfapi.log.Log;

/**
 * 
 */
public class ReferenceNameQueryTest {

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
     * Drops the given db before/after each test.
     */
    @Rule
    public DropMongoDBRule dropMongoDBRule = new DropMongoDBRule( mongoDBProcessRule, MongoDbName );

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
        
        File sourcePath = new File("src/test/java");

        new SimpleJavadocProcess().setMongoUri( MongoUri )
                                  .setLibraryId( "/java/com.surfapi/1.0" )
                                  .setSourcePath( sourcePath )
                                  .setPackages( Arrays.asList( "com.surfapi.test" ) )
                                  .run();

        new SimpleJavadocProcess().setMongoUri( MongoUri )
                                  .setLibraryId( "/java/com.surfapi/0.9" )
                                  .setSourcePath( sourcePath )
                                  .setPackages( Arrays.asList( "com.surfapi.test" ) )
                                  .run();
        
        DB db = new MongoDBImpl(MongoDbName);
        
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
        
        // Lookup ctor
        docs = new ReferenceNameQuery().inject(db).query( "com.surfapi.test.DemoJavadoc+DemoJavadoc" );
        
        assertFalse( docs.isEmpty() );
        Log.trace(this, Cawls.pluck(docs, "_id") );
        
        assertEquals( 4, docs.size() );
        
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/0.9/com.surfapi.test.DemoJavadoc()") ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/0.9/com.surfapi.test.DemoJavadoc(java.lang.String)") ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc()") ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc(java.lang.String)") ) );

        // Lookup with parm signature
        docs = new ReferenceNameQuery().inject(db).query( "com.surfapi.test.DemoJavadoc+DemoJavadoc()" );
        assertEquals( 2, docs.size() );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/0.9/com.surfapi.test.DemoJavadoc()") ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc()") ) );

        // Lookup with fully-qualified parm signature
        docs = new ReferenceNameQuery().inject(db).query( "com.surfapi.test.DemoJavadoc+DemoJavadoc(java.lang.String)" );
        assertEquals( 2, docs.size() );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/0.9/com.surfapi.test.DemoJavadoc(java.lang.String)") ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc(java.lang.String)") ) );

        // Lookup with non-qualified parm signature
        docs = new ReferenceNameQuery().inject(db).query( "com.surfapi.test.DemoJavadoc+DemoJavadoc(String)" );
        assertEquals( 2, docs.size() );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/0.9/com.surfapi.test.DemoJavadoc(java.lang.String)") ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc(java.lang.String)") ) );
        
        // Lookup method
        docs = new ReferenceNameQuery().inject(db).query( "com.surfapi.test.DemoJavadoc+parse" );
        assertEquals( 2, docs.size() );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/0.9/com.surfapi.test.DemoJavadoc.parse(java.net.URL,java.util.List)") ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc.parse(java.net.URL,java.util.List)") ) );
        
        // Lookup with fully-qualified parm signature
        docs = new ReferenceNameQuery().inject(db).query( "com.surfapi.test.DemoJavadoc+parse(java.net.URL,java.util.List)" );
        assertEquals( 2, docs.size() );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/0.9/com.surfapi.test.DemoJavadoc.parse(java.net.URL,java.util.List)") ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc.parse(java.net.URL,java.util.List)") ) );

        // Lookup with non-qualified parm signature
        docs = new ReferenceNameQuery().inject(db).query( "com.surfapi.test.DemoJavadoc+parse(URL,List)" );
        assertEquals( 2, docs.size() );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/0.9/com.surfapi.test.DemoJavadoc.parse(java.net.URL,java.util.List)") ) );
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc.parse(java.net.URL,java.util.List)") ) );
    }

    
    /**
     * 
     */
    @Test
    public void testGetReferenceNames() {
        
        Map packageModel = new MapBuilder().append("metaType", "package") 
                                            .append("name", "com.surfapi.test") ;
        
        Map classModel =  new MapBuilder().append("metaType", "class") 
                                          .append("name", "DemoJavadoc") 
                                          .append("qualifiedName", "com.surfapi.test.DemoJavadoc") ;

        List<Map> methodParms = new ListBuilder().append( new MapBuilder().append( "type", new MapBuilder().append( "typeName", "URL")
                                                                                                           .append("qualifiedTypeName", "java.net.URL" ) ) )
                                                 .append( new MapBuilder().append( "type", new MapBuilder().append( "typeName", "String")
                                                                                                            .append("qualifiedTypeName", "java.lang.String")
                                                                                                            .append("dimension", "[]") )  );
        
        Map methodModel =  new MapBuilder().append("metaType", "method") 
                                           .append("name", "parse") 
                                           .append("qualifiedName", "com.surfapi.test.DemoJavadoc.parse")
                                           .append("containingClass", classModel)
                                           .append("parameters", methodParms);
        
        Map ctorModel =  new MapBuilder().append("metaType", "constructor") 
                                           .append("name", "DemoJavadoc") 
                                           .append("qualifiedName", "com.surfapi.test.DemoJavadoc")
                                           .append("containingClass", classModel);

        assertEquals(new SetBuilder<String>().append("com.surfapi.test"),
                   new ReferenceNameQuery.IndexBuilder().getReferenceNames( packageModel ) );
        
        assertEquals(new SetBuilder<String>().append("com.surfapi.test.DemoJavadoc"),
                   new ReferenceNameQuery.IndexBuilder().getReferenceNames( classModel ) );
        
        Log.trace(this, new ReferenceNameQuery.IndexBuilder().getReferenceNames( methodModel ) );
        
        assertEquals(new SetBuilder<String>().appendAll("com.surfapi.test.DemoJavadoc+parse",
                                                        "com.surfapi.test.DemoJavadoc+parse(java.net.URL,java.lang.String[])",
                                                        "com.surfapi.test.DemoJavadoc+parse(URL,String[])"),
                     new ReferenceNameQuery.IndexBuilder().getReferenceNames( methodModel ) );

        assertEquals(new SetBuilder<String>().appendAll("com.surfapi.test.DemoJavadoc+DemoJavadoc",
                                                        "com.surfapi.test.DemoJavadoc+DemoJavadoc()"),
                     new ReferenceNameQuery.IndexBuilder().getReferenceNames( ctorModel ) ) ;
    }
    
    
    /**
     * 
     */
    @Test
    public void testQueryOne() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        DB db = new MongoDBImpl(MongoDbName);
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
    
    
    /**
     * 
     */
    @Test
    public void testNoDups() throws Exception {
        
        assumeTrue( mongoDBProcessRule.isStarted() );
        
        File sourcePath = new File("src/test/java");

        new SimpleJavadocProcess().setMongoUri( MongoUri )
                                  .setLibraryId( "/java/com.surfapi/1.0" )
                                  .setSourcePath( sourcePath )
                                  .setPackages( Arrays.asList( "com.surfapi.test" ) )
                                  .run();

        DB db = new MongoDBImpl(MongoDbName);
        
        new ReferenceNameQuery().inject(db).addLibraryToIndex("/java/com.surfapi/1.0" );
        
        List<Map> docs = new ReferenceNameQuery().inject(db).query( "com.surfapi.test.DemoJavadoc" );
        assertFalse( docs.isEmpty() );
        assertEquals( 1, docs.size() );
        
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc") ) );
        
        new ReferenceNameQuery().inject(db).addLibraryToIndex("/java/com.surfapi/1.0" );
        
        docs = new ReferenceNameQuery().inject(db).query( "com.surfapi.test.DemoJavadoc" );
        assertFalse( docs.isEmpty() );
        assertEquals( 1, docs.size() );
        
        assertNotNull( Cawls.findFirst( docs, new MapBuilder().append( "_id", "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc") ) );

        
    }
        
}

