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
import org.drools.runtime.process.ProcessInstance;
import org.drools.runtime.process.WorkItemHandler;

/**
 * 
 * @author <a href="mailto:kris_verlaenen@hotmail.com">Kris Verlaenen</a>
 */
public class GormWorkItemManager implements WorkItemManager, Externalizable {

	private static final long serialVersionUID = 510l;

	private Map<Long, WorkItem> workItems = new ConcurrentHashMap<Long, WorkItem>();
	private InternalKnowledgeRuntime kruntime;
	private Map<String, WorkItemHandler> workItemHandlers = new HashMap<String, WorkItemHandler>();

	public GormWorkItemManager(InternalKnowledgeRuntime kruntime) {
		this.kruntime = kruntime;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		workItems = (Map<Long, WorkItem>) in.readObject();
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
	    
		WorkItemInfo workItemInfo = 
			GrailsIntegration.getGORMDomainService().getNewWorkItemInfo(workItem);
		GrailsIntegration.getGORMDomainService().saveDomain(workItemInfo);
        Long workItemId = workItemInfo.getId(); //XXX {bauna}(Long) ((GroovyObject) workItemInfo).invokeMethod("getId", null);
		((WorkItemImpl) workItem).setId(workItemId);
       
        workItemInfo.update();
        
		internalAddWorkItem(workItem);
		WorkItemHandler handler = this.workItemHandlers.get(workItem.getName());
		if (handler != null) {
			handler.executeWorkItem(workItem, this);
		} else
			throw new WorkItemHandlerNotFoundException(
					"Could not find work item handler for "
							+ workItem.getName(), workItem.getName());
	}

	@Override
	public void internalAddWorkItem(WorkItem workItem) {
		workItems.put(workItem.getId(), workItem);
	}

	@Override
	public void internalAbortWorkItem(long id) {
		WorkItemImpl workItem = (WorkItemImpl) workItems.get(id);
		// work item may have been aborted
		if (workItem != null) {
			WorkItemHandler handler = this.workItemHandlers.get(workItem
					.getName());
			if (handler != null) {
				handler.abortWorkItem(workItem, this);
			} else {
				workItems.remove(workItem.getId());
				throw new WorkItemHandlerNotFoundException(
						"Could not find work item handler for "
								+ workItem.getName(), workItem.getName());
			}
			WorkItemInfo workItemInfo =
            	GrailsIntegration.getGORMDomainService().getWorkItemInfo(id);
            if (workItemInfo != null) {
            	GrailsIntegration.getGORMDomainService().deleteDomain(workItemInfo);
            }
			workItems.remove(workItem.getId());
		}
	}

	@Override
	public Set<WorkItem> getWorkItems() {
		return new HashSet<WorkItem>(workItems.values());
	}

	@Override
	public WorkItem getWorkItem(long id) {
		WorkItem workItem = workItems.get(id);
		if (workItem == null) {
			WorkItemInfo workItemInfo = GrailsIntegration
					.getGORMDomainService().getWorkItemInfo(id);
			if (workItemInfo != null) {
				workItem = workItemInfo.getWorkItem();
				this.internalAddWorkItem(workItem);
			}
		}

		return workItem;
	}

	@Override
	public void completeWorkItem(long id, Map<String, Object> results) {
		WorkItem workItem = getWorkItem(id);
		// work item may have been aborted
		if (workItem != null) {
			workItem.setResults(results);
			ProcessInstance processInstance = kruntime
					.getProcessInstance(workItem.getProcessInstanceId());
			workItem.setState(WorkItem.COMPLETED);
			// process instance may have finished already
			if (processInstance != null) {
				processInstance.signalEvent("workItemCompleted", workItem);
			}
			//XXX{bauna} test if we still delete it from here.
			WorkItemInfo workItemInfo =
            	GrailsIntegration.getGORMDomainService().getWorkItemInfo(id);
            if (workItemInfo != null) {
            	GrailsIntegration.getGORMDomainService().deleteDomain(workItemInfo);
            }
			
			workItems.remove(new Long(id));
		}
	}

	@Override
	public void abortWorkItem(long id) {
		WorkItemImpl workItem = (WorkItemImpl) workItems.get(id);
		// work item may have been aborted
		if (workItem != null) {
			ProcessInstance processInstance = kruntime
					.getProcessInstance(workItem.getProcessInstanceId());
			workItem.setState(WorkItem.ABORTED);
			// process instance may have finished already
			if (processInstance != null) {
				processInstance.signalEvent("workItemAborted", workItem);
			}
			//XXX{bauna} test if we still delete it from here.
			WorkItemInfo workItemInfo =
            	GrailsIntegration.getGORMDomainService().getWorkItemInfo(id);
            if (workItemInfo != null) {
            	GrailsIntegration.getGORMDomainService().deleteDomain(workItemInfo);
            }
			workItems.remove(id);
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
