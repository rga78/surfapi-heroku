package com.surfapi.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.surfapi.coll.Cawls;

/**
 * 
 */
public class LibraryUtils {

    /**
     * @return a list of libraries, based off the given list, that contains only the
     *         latest version of each library.
     */
    public static Collection<Map> latestVersionsOnly( Collection<Map> libraries ) {

        LatestVersionMap latestVersionMap = new LatestVersionMap();

        for (Map library : libraries ) {
            latestVersionMap.add( library );
        }
        
        return latestVersionMap.values();
    }
    
    /**
     * @return (-) if lib1 < lib2;
     *          0  if lib1 == lib2;
     *         (+) if lib1 > lib2
     */
    public static int libraryCompareVersion(Map lib1, Map lib2) {
         // Assumes version is in the format x.x.x
         return Cawls.compareInts( Cawls.toInts( StringUtils.split((String)lib1.get("version"), ".")), 
                                   Cawls.toInts( StringUtils.split((String)lib2.get("version"), ".")) );
    }

    /**
     * @return true if the two libraries have identical lang and name fields.
     */
    public static boolean libraryEqualsIgnoreVersion(Map lib1, Map lib2) {
        return lib1.get("lang").equals( lib2.get("lang") )
                && lib1.get("name").equals( lib2.get("name") );
    }

    /**
     * @return a subset of documents whose _library._id == libraryId.
     */
    public static List<Map> filterOnLibrary(Collection<Map> docs, String libraryId) {
        
        List<Map> retMe = new ArrayList<Map>();
        
        for (Map doc : Cawls.safeIterable(docs)) {
            if ( libraryId.equals( JavadocMapUtils.getLibraryId(doc) ) ) {
                retMe.add(doc);
            }
        }
        
        return retMe;
    }
        
}


/**
 * 
 */
class LatestVersionMap extends HashMap<String, Map> {

    /**
     * Add the given library to the map
     */
    public boolean add(Map library) {

        String key = buildKey(library);

        Map currEntry = get(key);

        if (currEntry == null) {
            put( key, library );
            return true;
        } else if ( LibraryUtils.libraryCompareVersion( library, currEntry ) > 0 ) {
            // replace with newer version.
            put( key, library );
            return true;
        }
        
        return false;
    }
    
    /**
     * @return a map key for the given library
     */
    protected String buildKey( Map library ) {
        return "/" + library.get("lang") + "/" + library.get("name");
    }


}

