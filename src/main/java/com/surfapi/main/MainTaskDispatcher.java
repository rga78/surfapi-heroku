package com.surfapi.main;

import com.surfapi.log.Log;

/**
 * Entry point.
 */
public class MainTaskDispatcher {

    /**
     * The set of tasks registered with this utility.
     */
    private TaskList tasks = new TaskList();

    /**
     * Register a task.
     * 
     * @param task
     * 
     * @return true
     */
    public boolean registerTask(Task task) {
        return tasks.add(task);
    }
    
    /**
     * @return the task with the given name.
     */
    private Task getTask(String name) {
        return tasks.forName(name);
    }
    
    /**
     * @return the TaskList 
     */
    public TaskList getTaskList() {
        return tasks;
    }

    /**
     * 
     */
    private void printUsage(String errorMsg) {
        Log.error(this, errorMsg);
        Task help = getTask("help");
        if (help != null) {
            Log.info(this, help.getTaskHelp());
        }
    }

    /**
     * Drive the logic of the program.
     * 
     * @param args
     */
    public int runProgram(String[] args) {
        
        // If no args, dump help and exit.
        if (args.length == 0) {
            printUsage("No task specified");
            return 0;
        }

        // Lookup the requested task.  Bail if not found.
        Task task = getTask(args[0]);
        if (task == null) {
            printUsage("Unknown task: " +  args[0]);
            return 0;
        } 
            
        // Run the task.
        try {
            return task.handleTask(args);
        } catch (Exception e) {
            Log.error(this, "Exception: ", e);
            return 255;
        }

    }

}
