package com.surfapi.web.rest;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONValue;

import com.surfapi.db.DBService;
import com.surfapi.db.post.AutoCompleteIndex;

/**
 * Auto complete REST target
 */
@Path("autoComplete")
public class AutoComplete {

    /**
     * the max number of results to return.
     */
    public static final int LimitResults = 25;
    
    /**
     * @return String that will be returned as the JSON response
     */
    @GET
    @Path("index")
    @Produces(MediaType.APPLICATION_JSON)
    public String queryIndex(@QueryParam("str") String str,
                             @DefaultValue( "java" ) @QueryParam("index") String indexName) {

        return JSONValue.toJSONString( new AutoCompleteIndex().inject( DBService.getDb() ).query( indexName, str, LimitResults ) ); 
    }

}
