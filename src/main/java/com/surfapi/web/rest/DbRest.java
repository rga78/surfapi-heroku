package com.surfapi.web.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.json.simple.JSONValue;

import com.surfapi.coll.MapBuilder;
import com.surfapi.db.DB;
import com.surfapi.db.DBService;

/**
 * DB REST target for /java _ids.
 */
@Path("/java")
public class DbRest {
 
    /**
     * TODO: possible to inject?
     */
    protected DB getDb() {
        return DBService.getDb();
    }
    
    /**
     * @return list of java libraries 
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLangLibraries() {
        return JSONValue.toJSONString( new MapBuilder().append("name", "java")
                                                       .append("metaType", "lang" )
                                                       .append("libraries", getDb().getLibraryList("java")) );
    }
    
    /**
     * @return A list of versions for the given library
     */
    @GET
    @Path("/{libName}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getLibraryVersions( @PathParam("libName") String libName) {
        return JSONValue.toJSONString( new MapBuilder().append( "name", libName)
                                                       .append( "metaType", "library.versions" )
                                                       .append( "lang", "java" )
                                                       .append( "versions", getDb().getLibraryVersions("java",  libName)) );
    }
    
    /**
     * @return The package list/summary for the given library.
     */
    @GET
    @Path("/{libName}/{libVersion}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getLibrary( @PathParam("libName") String libName,
                              @PathParam("libVersion") String libVersion ) {
        return JSONValue.toJSONString(getDb().getLibrary("/java/" + libName + "/" + libVersion));
    }
    
    /**
     * @return The specified javadoc record from the db.
     */
    @GET
    @Path("/{libName}/{libVersion}/{itemId:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getDoc( @PathParam("libName") String libName,
                          @PathParam("libVersion") String libVersion,
                          @PathParam("itemId") String itemId,
                          @Context UriInfo uriInfo) {
        
        
        String collection = "/java/" + libName + "/" + libVersion;
        return JSONValue.toJSONString(getDb().read( collection, 
                                                    collection + "/" + itemId, 
                                                    convertFieldsMap( uriInfo.getQueryParameters() ) ) 
                                      );
    }
    
    /**
     * @param fields a query param, specifies fields to include/exclude from the doc.
     *        E.g: fields=!methods  ==> include all fields *except* the 'methods' field
     *        E.g: fields=methods   ==> include *only* the 'methods' fields
     *        E.g: fields=!methods,allInheritedMethods => exclude 'methods' and 'allInheritedMethods'
     *        E.g: fields=methods,allInheritedMethods => include *only* 'methods' and 'allInheritedMethods'
     *        
     */
    protected Map<String, Integer> convertFieldsMap(MultivaluedMap<String, String> queryParams) {
        Map<String, Integer> retMe = new HashMap<String, Integer>();
        
        for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
            retMe.put(entry.getKey(), Integer.parseInt(entry.getValue().get(0)) );
        }
      
        return retMe;
    }

}
