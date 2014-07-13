package com.surfapi.web.rest;

import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONValue;

import com.surfapi.coll.ListBuilder;
import com.surfapi.coll.MapBuilder;
import com.surfapi.db.DBService;
import com.surfapi.db.post.AllKnownImplementorsQuery;
import com.surfapi.db.post.AllKnownSubclassesQuery;
import com.surfapi.db.post.ReferenceNameQuery;

/**
 * Query REST /q/* for DB queries.
 */
@Path("/q")
public class QueryRest {
    
    /**
     * @return A list of queries?? YEAH. Think like an API!
     */
    @GET
    @Path("/{lang}")
    @Produces(MediaType.APPLICATION_JSON)
    public String getLangQueries( @PathParam("lang") String lang) {

        List<Map> queryList = new ListBuilder<Map>().append( new MapBuilder().append("lang", "java")
                                                                             .append("uri", "/q/java/qn/{referenceName}") )
                                                    .append( new MapBuilder().append("lang", "java")
                                                                             .append("uri", "/q/java/allKnownSubclasses/{superclassName}") )
                                                    .append( new MapBuilder().append("lang", "java")
                                                                             .append("uri", "/q/java/allKnownImplementors/{interfaceName}") );
        return JSONValue.toJSONString( queryList );
    }
    
    /**
     * @return the results of the java quickName query
     */
    @GET
    @Path("/java/qn/{referenceName}")
    @Produces(MediaType.APPLICATION_JSON)
    public String javaQuickNameQuery( @PathParam("referenceName") String referenceName ) {

        List<Map> results = new ReferenceNameQuery().inject( DBService.getDb() ).query( referenceName );

        return JSONValue.toJSONString( results );
    }
   
    /**
     * @return the results of the 
     */
    @GET
    @Path("/java/allKnownSubclasses/{superclassName}")
    @Produces(MediaType.APPLICATION_JSON)
    public String javaAllKnownSubclasses( @PathParam("superclassName") String superclassName ) {

        List<Map> results = new AllKnownSubclassesQuery().inject( DBService.getDb() ).query( superclassName );

        return JSONValue.toJSONString( results );
    }
    
    
    /**
     * @return the results of the java quickName query
     */
    @GET
    @Path("/java/allKnownImplementors/{interfaceName}")
    @Produces(MediaType.APPLICATION_JSON)
    public String javaAllKnownImplementors( @PathParam("interfaceName") String interfaceName ) {

        List<Map> results = new AllKnownImplementorsQuery().inject( DBService.getDb() ).query( interfaceName );

        return JSONValue.toJSONString( results );
    }
    


}
