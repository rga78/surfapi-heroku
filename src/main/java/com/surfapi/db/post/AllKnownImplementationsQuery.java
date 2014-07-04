package com.surfapi.db.post;

import java.util.List;
import java.util.Map;

import com.surfapi.db.DB;

public class AllKnownImplementationsQuery {

    private DB db;
    
    public AllKnownImplementationsQuery inject(DB db) {
        this.db = db;
        return this;
    }
    
    protected DB getDb() {
        return db;
    }
    
    public void insert(List<Map> javadocModels) {
        // TODO Auto-generated method stub
        
    }

}
