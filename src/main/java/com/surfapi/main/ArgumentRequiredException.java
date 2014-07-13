package com.surfapi.main;

public class ArgumentRequiredException extends RuntimeException {

    public ArgumentRequiredException(String argName) {
        super("Missing required argument: " + argName);
    }
   
}
