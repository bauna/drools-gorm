package org.drools.gorm.session


import org.drools.common.InternalKnowledgeRuntime;
import org.drools.common.InternalRuleBase
import org.drools.marshalling.impl.MarshallerWriteContext
import org.drools.marshalling.impl.ProcessInstanceMarshaller
import org.drools.marshalling.impl.ProcessMarshallerRegistry
import org.drools.process.instance.impl.ProcessInstanceImpl
import org.drools.runtime.Environment;
import org.drools.runtime.process.ProcessInstance

import org.drools.gorm.session.marshalling.GormMarshallerReaderContext
import org.drools.gorm.DomainUtils
import org.drools.gorm.GrailsIntegration
import org.drools.impl.StatefulKnowledgeSessionImpl 

import java.sql.Blob
import org.hibernate.Hibernate

class ProcessInstanceInfoDomain implements ProcessInstanceInfo {

    Long id
    String processId
    Date startDate = new Date()
    Date lastReadDate
    Date lastModificationDate
    int state
    Blob processInstanceBlob
    
    static hasMany = [ processInstanceInfoEventType : ProcessInstanceInfoEventTypeDomain ]

    static constraints = {
    	lastModificationDate(nullable:true)
    	lastReadDate(nullable:true)
    	processInstanceBlob(nullable:true)
	}  
    
    static transients = ['processInstance', 'MarshallerFromContext',
                         'ProcessInstanceId', 'eventTypes', 'processInstanceByteArray']

	static mapping = {
    	processInstanceBlob type: 'blob'
	}	
    
    ProcessInstance processInstance
    
	def long getId() {
		return id
	}
	
    def getProcessInstanceByteArray() {
    	if (processInstanceBlob) {
    		return DomainUtils.blobToByteArray(processInstanceBlob)
    	}
    }

    def setProcessInstanceByteArray(value) {
    	this.setProcessInstanceBlob(Hibernate.createBlob(value))
    }    
    
    def getProcessInstanceId() {
        return id
    }

    public void updateLastReadDate() {
        lastReadDate = new Date()
    }

    public ProcessInstance getProcessInstance(InternalKnowledgeRuntime kruntime,
			Environment env) {
        if (processInstance == null) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(getProcessInstanceByteArray())

                GormMarshallerReaderContext context = new GormMarshallerReaderContext(
                        bais, (InternalRuleBase) kruntime.getRuleBase(), null, null)
                ProcessInstanceMarshaller marshaller = getMarshallerFromContext( context );
                context.wm = ((StatefulKnowledgeSessionImpl) kruntime).getInternalWorkingMemory();
                processInstance = marshaller.readProcessInstance(context);
                context.close();
            } catch (IOException e) {
                throw new IllegalArgumentException(
                        'IOException while loading process instance', e)
            }
        }
        return processInstance
    }

    private ProcessInstanceMarshaller getMarshallerFromContext(
            GormMarshallerReaderContext context) throws IOException {
        ObjectInputStream stream = context.stream
        String processInstanceType = stream.readUTF()
        return ProcessMarshallerRegistry.INSTANCE
                .getMarshaller(processInstanceType)
    }

    private void saveProcessInstanceType(MarshallerWriteContext context,
            ProcessInstance processInstance, String processInstanceType)
            throws IOException {
        ObjectOutputStream stream = context.stream
        // saves the processInstance type first
        stream.writeUTF(processInstanceType)
    }

    public void update() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        try {
            MarshallerWriteContext context = new MarshallerWriteContext(baos,
                    null, null, null, null)
            String processType = ((ProcessInstanceImpl) processInstance).getProcess()
                    .getType()
            saveProcessInstanceType(context, processInstance, processType)
            ProcessInstanceMarshaller marshaller = ProcessMarshallerRegistry.INSTANCE.getMarshaller(processType)
            marshaller.writeProcessInstance(
                    context, processInstance)
            context.close()
        } catch (IOException e) {
            throw new IllegalArgumentException('IOException while storing process instance ' +
                  processInstance.getId(), e)
        }
        byte[] newByteArray = baos.toByteArray()
        
        if (!Arrays.equals(newByteArray, getProcessInstanceByteArray())) {
        	this.state = processInstance.getState()
            this.lastModificationDate = new Date()
            this.setProcessInstanceByteArray(newByteArray)
            this.processInstanceInfoEventType.each {
            	it.delete()
            }
            processInstance.getEventTypes().each {
            	new ProcessInstanceInfoEventTypeDomain(
            			processInstanceInfo: this, name: it)
            }
            GrailsIntegration.getGORMDomainService().saveDomain(this)
        }
    }
}
