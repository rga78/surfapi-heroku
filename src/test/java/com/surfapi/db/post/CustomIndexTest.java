package com.surfapi.db.post;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import com.surfapi.junit.CaptureSystemOutRule;
import com.surfapi.log.Log;

public class CustomIndexTest {

    /**
     * Capture and suppress stdout unless the test fails.
     */
    @Rule
    public CaptureSystemOutRule systemOutRule  = new CaptureSystemOutRule( );
    
    @Test
    public void testGetAllIndexes() {
        
        List<CustomIndex<?>> indexes = CustomIndex.getAllIndexes();
        
        assertEquals(4, indexes.size());
        
        // Map to class names and verify
        List<String> classNames = new ArrayList<String>();
        for (CustomIndex<?> customIndex : indexes) {
            classNames.add( customIndex.getClass().getSimpleName() );
        }
        
        Log.trace(this, "testGetAllIndexes: simple name: ", classNames);
        
        assertTrue( classNames.contains("AutoCompleteIndex") );
        assertTrue( classNames.contains("ReferenceNameQuery") );
        assertTrue( classNames.contains("AllKnownSubclassesQuery") );
        assertTrue( classNames.contains("AllKnownImplementorsQuery") );
        
    }
    
    @Test
    public void testGetIndexes() {
          
        List<CustomIndex<?>> indexes = CustomIndex.getIndexes("AutoCompleteIndex");
        assertEquals(1, indexes.size());
        assertEquals("AutoCompleteIndex", indexes.get(0).getClass().getSimpleName());
        
        indexes = CustomIndex.getIndexes("ReferenceNameQuery");
        assertEquals(1, indexes.size());
        assertEquals("ReferenceNameQuery", indexes.get(0).getClass().getSimpleName());
        
        indexes = CustomIndex.getIndexes("AllKnownSubclassesQuery");
        assertEquals(1, indexes.size());
        assertEquals("AllKnownSubclassesQuery", indexes.get(0).getClass().getSimpleName());
        
        indexes = CustomIndex.getIndexes("AllKnownImplementorsQuery");
        assertEquals(1, indexes.size());
        assertEquals("AllKnownImplementorsQuery", indexes.get(0).getClass().getSimpleName());
        
    }
    
    @Test
    public void testGetIndexesEmptyFilter() {
          
        List<CustomIndex<?>> indexes = CustomIndex.getIndexes("");
        assertEquals(4, indexes.size());
        
        indexes = CustomIndex.getIndexes(null);
        assertEquals(4, indexes.size());
    }
}
