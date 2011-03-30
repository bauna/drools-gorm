package org.drools.gorm

import org.drools.runtime.process.WorkItem
import org.drools.runtime.process.WorkItemHandler
import org.drools.runtime.process.WorkItemManager

public class TestWorkItemHandler implements WorkItemHandler {

	private static final TestWorkItemHandler INSTANCE = new TestWorkItemHandler()
	
	private WorkItem workItem
	
	private TestWorkItemHandler() {
	}
	
	public static TestWorkItemHandler getInstance() {
		return INSTANCE
	}
	
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		println("executeWorkItem: ${workItem}")
		this.workItem = workItem
	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
	}
	
	public WorkItem getWorkItem() {
		println("getWorkItem: ${this.workItem}")
		WorkItem result = workItem
		workItem = null
		return result
	}

}
