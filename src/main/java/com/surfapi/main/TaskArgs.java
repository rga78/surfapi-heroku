package com.surfapi.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;


/**
 * Helper class for parsing command-line arguments.
 * 
 * Arguments are specified via <name>=<value> pairs.
 * No-value arguments are specified with just their name: <name>
 * 
 */
public class TaskArgs extends HashMap<String, Object> {
    
    /**
     * CTOR.
     */
    public TaskArgs(String[] args) {
        parseArgs(args);
    }
    
    /**
     * @return a Map of argName=argValue pairs.
     */
    protected Map<String, Object> parseArgs(String[] args) {
        
        for (String arg : args) {
            put( parseArgName(arg), parseArgValue(arg) );
        }
        
        return this;
    }
    
    /**
     * @return the arg name (e.g. "--argName=argValue" returns "--argName")
     */
    protected String parseArgName(String arg) {
        int idx = arg.indexOf("=");
        return (idx >= 0) ? arg.substring(0, idx) : arg;
    }
    
    /**
     * @return the arg value (e.g. "--argName=argValue" returns "argValue"),
     *         or null if the argument doesn't have a value.
     */
    protected String parseArgValue(String arg) {
        int idx = arg.indexOf("=");
        return (idx >= 0) ? arg.substring(idx + 1) : null;
    }
    
    /**
     * @return the value associated with the given arg.
     */
    public String getStringValue(String argName) {
        return (String) get(argName);
    }
    
    /**
     * @return the long value associated with the given arg
     */
    public Long getLongValue(String argName, Long defaultValue) {
        String val = getStringValue(argName);
        return ( val != null) ? new Long( val ) : defaultValue;
    }
    
    /**
     * @return true if the given argName was specified (i.e exists in the map).
     */
    public boolean isSpecified(String argName) {
        return containsKey(argName);
    }
    
    /**
     * @return the value associated with the given arg.
     * 
     * @throws IllegalArgumentException if the value is null or empty.
     */
    public String getRequiredStringValue(String argName) {
        String retMe = getStringValue(argName);
        
        if ( StringUtils.isEmpty(retMe) ) {
            throw new ArgumentRequiredException(argName);
        }
        
        return retMe;
    }
    
    /**
     * @return the given key value as a File, or null if the key doesn't exist.
     */
    public File getFileValue( String key ) {
        
        String fileName = getStringValue(key);
        
        return (StringUtils.isEmpty(fileName)) ? null : new File(fileName);
    }
    
    /**
     * 
     * @return the given key value as a Properties object.  The properties are read from
     *         the fileName associated with the given key.  If the key does not exist,
     *         an empty Properties object is returned.
     */
    public Properties getPropsValue( String key ) throws IOException {
        
        Properties retMe = new Properties();
        
        File propsFile = getFileValue( key );
        
        if (propsFile != null) {
            InputStreamReader isr = new InputStreamReader( new FileInputStream(propsFile), Charset.forName("ISO-8859-1"));
            try {
                retMe.load( isr );
            } finally {
                isr.close();
            }
        }
        
        return retMe;
    }
           
}