
package com.surfapi.coll;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Just a wrapper around a Map that provides some easy type-specific accessor methods.
 *
 */
public class EasyMap extends HashMap {

    /**
     * CTOR.
     */
    public EasyMap(Map map) {
        super(map);
    }

    /**
     * @return the string at the given key.
     */
    public String getString(String key) {
        return (String) get(key);
    }

    /**
     * @return the list at the given key, or an empty list if the key is not present
     *         (will never return null).
     */
    public List getList(String key) {
        List retMe = (List) get(key);
        return (retMe != null) ? retMe : Collections.EMPTY_LIST;
    }

    /**
     * @return the map at the given key, or an empty map if the key is not present
     *         (will never return null).
     */
    public Map getMap(String key) {
        Map retMe = (Map) get(key);
        return (retMe != null) ? retMe : Collections.EMPTY_MAP;
    }

    /**
     * @return the map at the given key, or an empty map if the key is not present
     *         (will never return null).
     */
    public EasyMap getEasyMap(String key) {
        return new EasyMap( getMap(key) );
    }


}
