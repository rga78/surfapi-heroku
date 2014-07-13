package com.surfapi.main;

import java.util.ArrayList;
import java.util.List;

public class TaskList extends ArrayList<Task> {
    

    /**
     * 
     * @param taskName desired task name
     * 
     * @return the JBatchUtilityTask with that name, or null if no match is found
     */
    public Task forName(String taskName) {
        for (Task task : this) {
            if (task.getTaskName().equals(taskName)) {
                return task;
            }
        }
        return null;
    }
    
    /**
     * @return the list of task names.
     */
    public List<String> getTaskNames() {
        List<String> retMe = new ArrayList<String>();
        for (Task task : this) {
            retMe.add( task.getTaskName() );
        }
        return retMe;
    }
}