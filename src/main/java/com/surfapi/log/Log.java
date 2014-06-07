package com.surfapi.log;

public class Log {

    public static void info(Object obj, String msg) {
        info(obj.getClass().getName() + ": " + msg);
    }
    
    public static void info(String msg) {
        log("INFO", msg);
    }
    
    public static void error(String msg) {
        log("ERROR", msg);
    }
    
    public static void error(Object obj, String msg) {
        error(obj.getClass().getName() + ": " + msg);
    }
    
    private static void log(String type, String msg) {
        System.out.println("[" + type + "] " + msg);
    }

    public static void trace(Object obj, String msg) {
        trace(obj.getClass().getName() + ": " + msg);
    }
    
    public static void trace(String msg) {
        log("TRACE", msg);
    }

}
