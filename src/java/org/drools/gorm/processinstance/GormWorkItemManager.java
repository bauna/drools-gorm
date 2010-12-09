/**
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.gorm.processinstance;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

/**
 * 
 * @author <a href="mailto:kris_verlaenen@hotmail.com">Kris Verlaenen</a>
 */
public class GormWorkItemManager implements WorkItemManager, Externalizable {

	private static final long serialVersionUID = 510l;
	
	private InternalKnowledgeRuntime kruntime;
	private Map<String, WorkItemHandler> workItemHandlers = new HashMap<String, WorkItemHandler>();
	private transient Map<Long, WorkItemInfo> workItems = new ConcurrentHashMap<Long, WorkItemInfo>();
	
	public GormWorkItemManager(InternalKnowledgeRuntime kruntime) {
		this.kruntime = kruntime;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		workItems = (Map<Long, WorkItemInfo>) in.readObject();
		kruntime = (InternalKnowledgeRuntime) in.readObject();
		workItemHandlers = (Map<String, WorkItemHandler>) in.readObject();
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(workItems);
		out.writeObject(kruntime);
		out.writeObject(workItemHandlers);
	}

    @Override
    public void internalExecuteWorkItem(WorkItem workItem) {

        WorkItemInfo workItemInfo = GrailsIntegration.getGormDomainService().getNewWorkItemInfo(
                workItem,
                kruntime.getEnvironment());
        GrailsIntegration.getGormDomainService().saveDomain(workItemInfo);
        Long workItemId = workItemInfo.getId(); // XXX {bauna}(Long) ((GroovyObject) workItemInfo).invokeMethod("getId", null);
        ((WorkItemImpl) workItem).setId(workItemId);
        workItemInfo.generateBlob();
        WorkItemHandler handler = this.workItemHandlers.get(workItem.getName());
        if (handler != null) {
            handler.executeWorkItem(workItem, this);
        } else {
            throw new WorkItemHandlerNotFoundException("Could not find work item handler for "
                    + workItem.getName(), 
                    workItem.getName());
        }
    }

	@Override
	public void internalAddWorkItem(WorkItem workItem) {
	}

	@Override
	public void internalAbortWorkItem(long id) {
	    WorkItemInfo workItemInfo = GrailsIntegration
	        .getGormDomainService().getWorkItemInfo(id);
        // work item may have been aborted
        if (workItemInfo != null) {
            WorkItem workItem = workItemInfo.getWorkItem(this.kruntime.getEnvironment());
            WorkItemHandler handler = workItemHandlers.get(workItem.getName());
            if (handler != null) {
                handler.abortWorkItem(workItem, this);
            } else {
                if ( workItems != null ) {
                    workItems.remove( id );
                    throw new WorkItemHandlerNotFoundException( "Could not find work item handler for " + 
                            workItem.getName(), 
                            workItem.getName() );
                }
            }
            if (workItems != null) {
                workItems.remove(id);
            }
            GrailsIntegration.getGormDomainService().deleteDomain(workItemInfo);
        }
	}

	@Override
	public Set<WorkItem> getWorkItems() {
	    Set<WorkItem> wis = new HashSet<WorkItem>();
	    for (WorkItemInfo wii : workItems.values()) {
            wis.add(wii.getWorkItem(kruntime.getEnvironment()));
        }
		return wis;
	}

	@Override
	public WorkItem getWorkItem(long id) {
		WorkItemInfo workItemInfo = workItems.get(id);
		WorkItem workItem = null;
		if (workItemInfo == null) {
			workItemInfo = GrailsIntegration
					.getGormDomainService().getWorkItemInfo(id);
			if (workItemInfo != null) {
				workItem = workItemInfo.getWorkItem(kruntime.getEnvironment());
				workItems.put(workItemInfo.getId(), workItemInfo);
				this.internalAddWorkItem(workItem);
			}
		} else {
		    workItem = workItemInfo.getWorkItem(kruntime.getEnvironment());
		}

		return workItem;
	}

	@Override
	public void completeWorkItem(long id, Map<String, Object> results) {
        Environment env = this.kruntime.getEnvironment();
        
        WorkItemInfo workItemInfo = null;
            workItemInfo = this.workItems.get(id);
            if (workItemInfo != null) {
                workItemInfo = (WorkItemInfo) GrailsIntegration.getGormDomainService()
                    .mergeDomain(workItemInfo);
            }
        
        if (workItemInfo == null) {
            workItemInfo = GrailsIntegration.getGormDomainService()
                .getWorkItemInfo(id);
        }
        
        // work item may have been aborted
        if (workItemInfo != null) {
            WorkItem workItem = (WorkItemImpl) workItemInfo.getWorkItem(env);
            workItem.setResults(results);
            ProcessInstance processInstance = 
                kruntime.getProcessInstance(workItem.getProcessInstanceId());
            workItem.setState(WorkItem.COMPLETED);
            // process instance may have finished already
            if (processInstance != null) {
                processInstance.signalEvent("workItemCompleted", workItem);
            }
            GrailsIntegration.getGormDomainService().deleteDomain(workItemInfo);
            if (workItems != null) {
                this.workItems.remove(workItem.getId());
            }
        }
    }
	
	@Override
	public void abortWorkItem(long id) {
	    Environment env = this.kruntime.getEnvironment();
        
        WorkItemInfo workItemInfo =  this.workItems.get(id);
        if (workItemInfo != null) {
            GrailsIntegration.getGormDomainService().mergeDomain(workItemInfo);
        }
        
        if (workItemInfo == null) {
            workItemInfo = GrailsIntegration.getGormDomainService().getWorkItemInfo(id);
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
            GrailsIntegration.getGormDomainService().deleteDomain(workItemInfo);
            if (workItems != null) {
                workItems.remove(workItem.getId());
            }
        }
	}

	@Override
	public void registerWorkItemHandler(String workItemName,
			WorkItemHandler handler) {
		this.workItemHandlers.put(workItemName, handler);
	}

	@Override
	public void clear() {
		this.workItems.clear();
	}

}
