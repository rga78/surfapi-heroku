package com.surfapi.main;

public interface Task {

    /**
     * Answers the name of the task, which should be as succinct as possible.
     * The task name is used in help display and is how the task is invoked
     * by the script user.
     * 
     * @return the name of the task
     */
    String getTaskName();

    /**
     * Answers the help message for the task, which is used by the script
     * help statement. This message should be more verbose than the usage
     * statement, and should explain the required and optional arguments
     * that the task supports.
     * 
     * @return the help message for the task
     */
    String getTaskHelp();

    /**
     * Answer the description of of the task, which will be used in help display
     * to show what does the task do.
     * 
     * @return the description of the task
     */
    String getTaskDescription();

    /**
     * Perform the task logic.
     * 
     * @param args The arguments passed to the script, including the task name
     * 
     * @return the task RC - this will be passed back as the shell return code.
     * 
     * @throws IllegalArgumentException if the task was called with invalid arguments
     */
    int handleTask(String[] args) throws Exception;
}
