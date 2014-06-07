package com.surfapi.db.post;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.surfapi.app.JavadocMapUtils;
import com.surfapi.coll.Cawls;
import com.surfapi.db.DB;
import com.surfapi.db.DBImpl;
import com.surfapi.db.DBLoader;

/**
 *
 */
public class SetStubIdsTest {

    
    /**
     * 
     */
    @Test
    public void testSetStubIds() throws Exception {
        
        DB db = new DBImpl();
        new DBLoader().inject(db).loadFile( new File("src/test/resources/com.surfapi_1.0.json") );
        
        String libraryId = "/java/com.surfapi/1.0";
        
        Map doc = db.read(libraryId, "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc");
        assertNotNull(doc);
        
        // Verify initially that no _ids are present in any of the method stubs.
        // Note: also verifying that the lists are not empty
        assertTrue( assertAllIdsAreNull( (List<Map>) doc.get("methods") ).size() > 0) ;
        assertTrue( assertAllIdsAreNull( (List<Map>) doc.get("fields") ).size() > 0) ;
        assertTrue( assertAllIdsAreNull( (List<Map>) doc.get("constructors") ).size() > 0) ;
        assertTrue( assertAllIdsAreNull( (List<Map>) doc.get("innerClasses") ).size() > 0) ;
        
        // Now run the post-processor
        db.forAll(Arrays.asList(libraryId), new SetStubIds() );
        
        // Re-fetch the document
        doc = db.read(libraryId, "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc");
        assertNotNull(doc);
        
        // Verify that all _ids have been set.
        // Note: also verifying that the lists are not empty
        assertTrue( assertAllIdsAreSet( libraryId, (List<Map>) doc.get("methods") ).size() > 0) ;
        assertTrue( assertAllIdsAreSet( libraryId, (List<Map>) doc.get("fields") ).size() > 0) ;
        assertTrue( assertAllIdsAreSet( libraryId, (List<Map>) doc.get("constructors") ).size() > 0) ;
        
        Map containingPackage = (Map) doc.get("containingPackage");
        assertNotNull(containingPackage);
        assertEquals( JavadocMapUtils.buildId(libraryId, containingPackage), containingPackage.get("_id") );
        
        // superclass and interfaces are not in the same library as DemoJavadoc, so their _ids should NOT
        // have been set.
        
        assertNull( ((Map)doc.get("superclass")).get("_id") );
        Map callableInterface = ((List<Map>)doc.get("interfaces")).get(0);
        assertNull( callableInterface.get("_id") );
        
    }
    
    private Collection<Map> assertAllIdsAreNull( Collection<Map> stubs) {
        for (Map stub : Cawls.safeIterable(stubs)) {
            assertNull( stub.get("_id") );
        }
        return stubs;
    }
    
    private Collection<Map> assertAllIdsAreSet( String libraryId, Collection<Map> stubs) {
        for (Map stub : stubs) {
            assertEquals( JavadocMapUtils.buildId(libraryId, stub), stub.get("_id") );
        }
        return stubs;
    }

    /**
     *
     */
    // @Test
    public void testSetStubIdsIfSameLibrary() throws Exception {

        DB db = new DBImpl();
        new DBLoader().inject(db).loadFile( new File("src/test/resources/com.surfapi_1.0.json") );
        
        String libraryId = "/java/com.surfapi/1.0";
        
        Map doc = db.read(libraryId, "/java/com.surfapi/1.0/com.surfapi.test.DemoJavadoc.getAnnotation(DemoJavadoc)");
        assertNotNull(doc);
        assertAllIdsAreNull( (Collection<Map>) doc.get("thrownExceptions") );
        assertAllIdsAreNull( Arrays.asList( (Map) doc.get("returnType") ) );
        assertAllIdsAreNull( (Collection<Map>) Cawls.pluck( (List<Map>) doc.get("parameters"), "type" ) );

        new SetStubIds().setStubIds(db, libraryId, doc);

        assertTrue( assertAllIdsAreSet( libraryId, (Collection<Map>) doc.get("thrownExceptions") ).size() > 0 );
        assertTrue( assertAllIdsAreSet( libraryId, Arrays.asList( (Map) doc.get("returnType") ) ).size() > 0);
        assertTrue( assertAllIdsAreSet( libraryId, (Collection<Map>) Cawls.pluck( (List<Map>) doc.get("parameters"), "type" ) ).size() > 0);
    }
}
