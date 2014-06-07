package com.surfapi.web.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONValue;

import com.surfapi.db.DBService;

/**
 * Mongo DB REST target
 */
@Path("_db")
public class MongoRest {

    
    /**
     * @return String that will be returned as the JSON response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String go() {
        return JSONValue.toJSONString(DBService.getDb().getStats());
    }

}
