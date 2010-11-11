package org.drools.gorm

import org.drools.gorm.session.SessionInfoDomain
import org.drools.gorm.session.SessionInfo
import org.drools.gorm.session.WorkItemInfo
import org.drools.gorm.session.WorkItemInfoDomain
import org.drools.gorm.session.ProcessInstanceInfo
import org.drools.gorm.session.ProcessInstanceInfoDomain
import org.drools.gorm.session.ProcessInstanceEventInfo
import org.drools.gorm.session.ProcessInstanceEventInfoDomain
import org.drools.runtime.Environment;

class GORMDomainService {
    boolean transactional = false

	// SessionInfo --------------------------
    SessionInfo getSessionInfo(id) {
		return SessionInfoDomain.get(id)
	}

    SessionInfo lockSessionInfo(id) {
		return SessionInfoDomain.lock(id)
	}
	
    SessionInfo getNewSessionInfo() {
    	return new SessionInfoDomain()
    }

	
	// ProcessInstanceInfo --------------------------
	ProcessInstanceInfo getProcessInstanceInfo(id) {
		return ProcessInstanceInfoDomain.get(id)
	}
	
	ProcessInstanceInfo getNewProcessInstanceInfo(processInstance) {
    	def pii = new ProcessInstanceInfoDomain()
    	pii.processInstance = processInstance
    	pii.processId = processInstance.getProcessId()
    	return pii
    }

    def getProcessInstancesForEvent(type) {
		def c = ProcessInstanceInfoDomain.createCriteria()
		def results = c.list {
				processInstanceInfoEventType {
					eq("name", type)
       			}
			}
        return results*.id
    }
    
	// ProcessInstanceEventInfo --------------------------
	ProcessInstanceEventInfo getProcessInstanceEventInfo(id) {
		return ProcessInstanceEventInfoDomain.get(id)
	}
	
	ProcessInstanceEventInfo getNewProcessInstanceEventInfo(long processInstanceId,
															String eventType) {
    	return new ProcessInstanceEventInfoDomain(processInstanceId: processInstanceId,
    										eventType: eventType)
    }
	
	// WorkItemInfo --------------------------
	WorkItemInfo getWorkItemInfo(id) {
    	return WorkItemInfoDomain.get(id)
    }
	
	WorkItemInfo getNewWorkItemInfo(workItem) {
		return getNewWorkItemInfo( workItem, null)
	}
	
	WorkItemInfo getNewWorkItemInfo(workItem, Environment env) {
    	def wii = new WorkItemInfoDomain(name: workItem.getName(), 
    					processInstanceId: workItem.getProcessInstanceId())
        wii.workItem = workItem
        return wii
    }
    
	// common --------------------------
    def saveDomain(domainObject) {
    	if(!domainObject.save(flush:true)) {
    		 throw new IllegalArgumentException("Object of '${domainObject.class.simpleName}' couldn't be saved because of validation errors: "+ domainObject.errors.toString())
    	}     	 
    }

    def deleteDomain(domainObject) {
    	return domainObject.delete(flush:true)
    }

    def mergeDomain(domainObject) {
    	return domainObject.merge(flush:true)
    }
}
