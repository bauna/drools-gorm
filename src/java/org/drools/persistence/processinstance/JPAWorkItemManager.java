package org.drools.persistence.processinstance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.drools.WorkItemHandlerNotFoundException;
import org.drools.common.InternalKnowledgeRuntime;
import org.drools.gorm.GrailsIntegration;
import org.drools.gorm.session.WorkItemInfo;
import org.drools.process.instance.WorkItem;
import org.drools.process.instance.WorkItemManager;
import org.drools.process.instance.impl.WorkItemImpl;
import org.drools.runtime.Environment;
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkItemHandler;

public class JPAWorkItemManager implements WorkItemManager {

    private InternalKnowledgeRuntime kruntime;
	private Map<String, WorkItemHandler> workItemHandlers = new HashMap<String, WorkItemHandler>();
    private transient Map<Long, WorkItemInfo> workItems;
    
    public JPAWorkItemManager(InternalKnowledgeRuntime kruntime) {
    	this.kruntime = kruntime;
    }
    
	public void internalExecuteWorkItem(WorkItem workItem) {
        //WorkItemInfo workItemInfo = new WorkItemInfo(workItem, env);
        WorkItemInfo workItemInfo = GrailsIntegration.getGORMDomainService().getNewWorkItemInfo(workItem, true);
        ((WorkItemImpl) workItem).setId(workItemInfo.getId());
        workItemInfo.update();
        
		if (this.workItems == null) {
        	this.workItems = new HashMap<Long, WorkItemInfo>();
        }
		workItems.put(workItem.getId(), workItemInfo);
        
        WorkItemHandler handler = (WorkItemHandler) this.workItemHandlers.get(workItem.getName());
	    if (handler != null) {
	        handler.executeWorkItem(workItem, this);
	    } else {
	        throwWorkItemNotFoundException( workItem );
	    }
	}

    private void throwWorkItemNotFoundException(WorkItem workItem) {
        throw new WorkItemHandlerNotFoundException( "Could not find work item handler for " + workItem.getName(),
                                                    workItem.getName() );
	}

	public void internalAbortWorkItem(long id) {
        Environment env = this.kruntime.getEnvironment();
	    
        WorkItemInfo workItemInfo = GrailsIntegration.getGORMDomainService().getWorkItemInfo(id);
        // work item may have been aborted
        if (workItemInfo != null) {
            WorkItemImpl workItem = (WorkItemImpl) workItemInfo.getWorkItem(env);
            WorkItemHandler handler = (WorkItemHandler) this.workItemHandlers.get(workItem.getName());
            if (handler != null) {
                handler.abortWorkItem(workItem, this);
            } else {
                if ( workItems != null ) {
                    workItems.remove( id );
                    throwWorkItemNotFoundException( workItem );
                }
            }
            if (workItems != null) {
            	workItems.remove(id);
            }
            GrailsIntegration.getGORMDomainService().deleteDomain(workItemInfo);
        }
	}

	public void internalAddWorkItem(WorkItem workItem) {
	}

    public void completeWorkItem(long id, Map<String, Object> results) {
        Environment env = this.kruntime.getEnvironment();
        
        WorkItemInfo workItemInfo = null;
        if (this.workItems != null) {
	    	workItemInfo = this.workItems.get(id);
	    	if (workItemInfo != null) {
	    		workItemInfo = (WorkItemInfo) GrailsIntegration.getGORMDomainService().mergeDomain(workItemInfo);
	    	}
    	}
        
        if (workItemInfo == null) {
        	workItemInfo = GrailsIntegration.getGORMDomainService().getWorkItemInfo(id);
        }
        
    	// work item may have been aborted
        if (workItemInfo != null) {
            WorkItem workItem = (WorkItemImpl) workItemInfo.getWorkItem(env);
            workItem.setResults(results);
            ProcessInstance processInstance = kruntime.getProcessInstance(workItem.getProcessInstanceId());
            workItem.setState(WorkItem.COMPLETED);
            // process instance may have finished already
            if (processInstance != null) {
                processInstance.signalEvent("workItemCompleted", workItem);
            }
            GrailsIntegration.getGORMDomainService().deleteDomain(workItemInfo);
            if (workItems != null) {
            	this.workItems.remove(workItem.getId());
            }
    	}
    }

    public void abortWorkItem(long id) {
        Environment env = this.kruntime.getEnvironment();
        
        WorkItemInfo workItemInfo = null;
        if (this.workItems != null) {
	    	workItemInfo = this.workItems.get(id);
	    	if (workItemInfo != null) {
	    		GrailsIntegration.getCurrentSession().update(workItemInfo);
	    	}
    	}
        
        if (workItemInfo == null) {
        	workItemInfo = GrailsIntegration.getGORMDomainService().getWorkItemInfo(id);
        }
        
    	// work item may have been aborted
        if (workItemInfo != null) {
            WorkItem workItem = (WorkItemImpl) workItemInfo.getWorkItem(env);
            ProcessInstance processInstance = kruntime.getProcessInstance(workItem.getProcessInstanceId());
            workItem.setState(WorkItem.ABORTED);
            // process instance may have finished already
            if (processInstance != null) {
                processInstance.signalEvent("workItemAborted", workItem);
            }
            GrailsIntegration.getGORMDomainService().deleteDomain(workItemInfo);
            if (workItems != null) {
            	workItems.remove(workItem.getId());
            }
        }
    }

	public WorkItem getWorkItem(long id) {
        Environment env = this.kruntime.getEnvironment();

        WorkItemInfo workItemInfo = null;
        if (this.workItems != null) {
	    	workItemInfo = this.workItems.get(id);
    	}
        
        if (workItemInfo == null) {
        	workItemInfo = GrailsIntegration.getGORMDomainService().getWorkItemInfo(id);
        }

        if (workItemInfo == null) {
            return null;
        }
        return workItemInfo.getWorkItem(env);
	}

	public Set<WorkItem> getWorkItems() {
		return new HashSet<WorkItem>();
	}

	public void registerWorkItemHandler(String workItemName, WorkItemHandler handler) {
        this.workItemHandlers.put(workItemName, handler);
	}

    public void clearWorkItems() {
    	if (workItems != null) {
    		workItems.clear();
    	}
    }

	public void clear() {
		clearWorkItems();
	}
}
