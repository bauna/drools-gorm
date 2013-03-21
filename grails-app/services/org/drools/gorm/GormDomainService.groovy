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
import org.jbpm.process.instance.ProcessInstance;

class GormDomainService {
//    static transactional = false
    
    // SessionInfo --------------------------
    def SessionInfo getSessionInfo(id, env) {
        def sid = SessionInfoDomain.get(id)
        if (sid != null) {
            sid.setEnv(env)
        }
        return sid
    }
    
    def SessionInfo lockSessionInfo(id) {
        return SessionInfoDomain.lock(id)
    }
    
    def SessionInfo getNewSessionInfo(Environment env) {
        def sess = new SessionInfoDomain()
        sess.env = env
        return sess
    }
    
    // ProcessInstanceInfo --------------------------
    def ProcessInstanceInfo getProcessInstanceInfo(id, Environment env) {
        def pii = ProcessInstanceInfoDomain.get(id)
        if (pii != null) {
            pii.setEnv(env)
        }
        return pii
    }
    
    def ProcessInstanceInfo getNewProcessInstanceInfo(ProcessInstance processInstance, Environment env) {        
        def pii = new ProcessInstanceInfoDomain()
        pii.processInstance = processInstance
        pii.processId = processInstance.getProcessId()
        pii.startDate = new Date()
        pii.env = env
        pii.save(flush: true)
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
    def ProcessInstanceEventInfo getProcessInstanceEventInfo(id) {
        return ProcessInstanceEventInfoDomain.get(id)
    }
    
    def ProcessInstanceEventInfo getNewProcessInstanceEventInfo(long processInstanceId,
            String eventType) {            
        def pied = new ProcessInstanceEventInfoDomain()
        pied.processInstanceId = processInstanceId
        pied.eventType = eventType
        return pied
    }
    
    // WorkItemInfo --------------------------
    def WorkItemInfo getWorkItemInfo(id) {
        return WorkItemInfoDomain.get(id)
    }
    
    def WorkItemInfo getNewWorkItemInfo(workItem, Environment env) {
        return new WorkItemInfoDomain(workItem, env)
    }
    
    // common --------------------------
    def saveDomain(domainObject) {
        if(!domainObject.save(flush: true)) {
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
