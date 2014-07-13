package com.surfapi.main.tasks;

import com.surfapi.main.Task;
import com.surfapi.main.TaskList;

public class HelpTask implements Task {
    
    /**
     * List of registered tasks.
     */
    private final TaskList tasks;

    /**
     * CTOR.
     */
    public HelpTask(TaskList tasks) {
        this.tasks = tasks;
    }


    @Override
    public String getTaskName() {
        return "help";
    }

    @Override
    public String getTaskHelp() {
        StringBuilder sb = new StringBuilder();
        
        for (Task task : tasks) {
            if (task != this) {
                sb.append("\t" + task.getTaskHelp())
                .append("\n")
                .append(task.getTaskDescription())
                .append("\n");
            }
        }
        
        return sb.toString();
    }

    @Override
    public String getTaskDescription() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int handleTask(String[] args) throws Exception {
        System.out.println( getTaskHelp() );
        return 0;
    }

}
