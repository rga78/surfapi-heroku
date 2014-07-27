package com.surfapi.main;

import com.surfapi.db.DB;

public abstract class Task<T extends Task> {
    
    /**
     * Injected REF to DB.
     */
    private DB db;
    
    /**
     * inject DB.
     */
    public T inject(DB db) {
        this.db = db;
        return (T) this;
    }

    /**
     * @return the injected db
     */
    protected DB getDb() {
        return db;
    }
    
    /**
     * Answers the name of the task, which should be as succinct as possible.
     * The task name is used in help display and is how the task is invoked
     * by the script user.
     * 
     * @return the name of the task
     */
    public abstract String getTaskName();

    /**
     * Answers the help message for the task, which is used by the script
     * help statement. This message should be more verbose than the usage
     * statement, and should explain the required and optional arguments
     * that the task supports.
     * 
     * @return the help message for the task
     */
    public abstract String getTaskHelp();

    /**
     * Answer the description of of the task, which will be used in help display
     * to show what does the task do.
     * 
     * @return the description of the task
     */
    public abstract String getTaskDescription();

    /**
     * Perform the task logic.
     * 
     * @param args The arguments passed to the script, including the task name
     * 
     * @return the task RC - this will be passed back as the shell return code.
     * 
     * @throws IllegalArgumentException if the task was called with invalid arguments
     */
    public abstract int handleTask(String[] args) throws Exception;
}
