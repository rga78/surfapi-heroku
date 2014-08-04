package com.surfapi.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
    
    /**
     * @param library The library under test
     * @param libraryVersions A list of all versions of the given library
     * 
     * @return true if the given library is the first of its kind or if its version
     *         is newer (greater) than any other of its kind.
     */
    public static boolean isLatestVersion( Map library, List<Map> libraryVersions ) {
        
        // Note: the library we're adding is already in the library versions list.
        // (It was added when the javadoc data was loaded). So if the list only has one entry, then
        // it must be for this library. Otherwise check if this library is newer
        // than every other one in the list.
    
        
        if (libraryVersions.size() == 1) {
            // sanity check
            if (libraryVersions.get(0).equals(library)) {
                return true;
            } else {
                throw new IllegalArgumentException("The given library, " + library + ", doesn't match the "
                                                   + " lone existing library: " + libraryVersions.get(0));
            }
        } else {
            // Sort the libraries in reverse order (newest version first).
            // If the library being added is at the head of the list, then
            // it must be the newest.
            Collections.sort( libraryVersions, new Comparator<Map>() {
                public int compare(Map lib1, Map lib2) {
                    return (-1) * LibraryUtils.libraryCompareVersion(lib1, lib2);
                }
            });
            
            if ( library.get("version").equals( libraryVersions.get(0).get("version") ) ) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * @return /[lang]/[name]  (no version)
     */
    public static String getIdSansVersion(Map library) {
        return "/" + library.get("lang") + "/" + library.get("name") ;
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

