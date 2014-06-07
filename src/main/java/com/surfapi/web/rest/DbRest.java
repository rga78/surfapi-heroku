package com.surfapi.web.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONValue;

import com.surfapi.coll.MapBuilder;
import com.surfapi.db.DBService;

/**
 * DB REST target for /java _ids.
 */
@Path("/java")
public class DbRest {
 
    
    /**
     * @return list of java libraries 
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getLangLibraries() {
        return JSONValue.toJSONString( new MapBuilder().append("name", "java")
                                                       .append("metaType", "lang" )
                                                       .append("libraries", DBService.getDb().getLibraryList("java")) );
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
                                                       .append( "versions", DBService.getDb().getLibraryVersions("java",  libName)) );
    }
    
    /**
     * @return The package list/summary for the given library.
     */
    @GET
    @Path("/{libName}/{libVersion}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getLibrary( @PathParam("libName") String libName,
                              @PathParam("libVersion") String libVersion ) {
        return JSONValue.toJSONString(DBService.getDb().getLibrary("/java/" + libName + "/" + libVersion));
    }
    
    /**
     * @return The specified javadoc record from the db.
     */
    @GET
    @Path("/{libName}/{libVersion}/{itemId:.*}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getDoc( @PathParam("libName") String libName,
                          @PathParam("libVersion") String libVersion,
                          @PathParam("itemId") String itemId ) {
        
        String collection = "/java/" + libName + "/" + libVersion;
        return JSONValue.toJSONString(DBService.getDb().read( collection, collection + "/" + itemId ));
    }

}
