package com.surfapi.coll;

import java.util.HashSet;

public class SetBuilder<T> extends HashSet<T> {
    
    
    public SetBuilder<T> append(T obj) {
        add(obj);
        return this;
    }
    
    public SetBuilder<T> appendAll(T... objs) {
        for (T obj : objs) {
            add(obj);
        }
        return this;
    }

}
