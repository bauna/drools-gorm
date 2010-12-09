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
import org.drools.process.instance.ProcessInstance;

class GormDomainService {
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
    def ProcessInstanceInfo getProcessInstanceInfo(id) {
        return ProcessInstanceInfoDomain.get(id)
    }
    
    def ProcessInstanceInfo getNewProcessInstanceInfo(ProcessInstance processInstance, Environment env) {
        return new ProcessInstanceInfoDomain(
            processInstance: processInstance, 
            processId: processInstance.getProcessId(), 
            startDate: new Date(),
            env: env)
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
        return new ProcessInstanceEventInfoDomain(
            processInstanceId: processInstanceId,
            eventType: eventType)
    }
    
    // WorkItemInfo --------------------------
    WorkItemInfo getWorkItemInfo(id) {
        return WorkItemInfoDomain.get(id)
    }
    
    WorkItemInfo getNewWorkItemInfo(workItem, Environment env) {
        return new WorkItemInfoDomain(workItem, env)
    }
    
    // common --------------------------
    def saveDomain(domainObject) {
        if(!domainObject.save()) {
            throw new IllegalArgumentException("Object of '${domainObject.class.simpleName}' couldn't be saved because of validation errors: "+ domainObject.errors.toString())
        }     	 
    }
    
    def deleteDomain(domainObject) {
        return domainObject.delete(flush:true)
    }
    
    def mergeDomain(domainObject) {
        return domainObject.merge(flush:true)
    }
    
    def updateDomain(domainObject) {
        return domainObject.update(flush:true)
    }
}
