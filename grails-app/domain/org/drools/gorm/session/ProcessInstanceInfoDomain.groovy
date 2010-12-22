package org.drools.gorm.session

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Blob;
import java.util.Arrays;
import java.util.Date;

import org.drools.common.InternalKnowledgeRuntime;
import org.drools.common.InternalRuleBase;
import org.drools.gorm.DomainUtils;
import org.drools.impl.InternalKnowledgeBase;
import org.drools.impl.StatefulKnowledgeSessionImpl;
import org.drools.marshalling.impl.MarshallerReaderContext;
import org.drools.marshalling.impl.MarshallerWriteContext;
import org.drools.marshalling.impl.ProcessInstanceMarshaller;
import org.drools.marshalling.impl.ProcessMarshallerRegistry;
import org.drools.process.instance.ProcessInstance;
import org.drools.process.instance.impl.ProcessInstanceImpl;
import org.drools.runtime.Environment;
import org.hibernate.Hibernate;

class ProcessInstanceInfoDomain implements ProcessInstanceInfo {    
    long id
    String processId
    Date startDate = new Date()
    Date lastReadDate
    Date lastModificationDate
    int state
    byte[] data
    
    ProcessInstance processInstance
    Environment env
    
    static hasMany = [ eventTypes : ProcessInstanceInfoEventTypeDomain ]
    
    static constraints = {
        lastModificationDate(nullable:true)
        lastReadDate(nullable:true)
        data(nullable:true, maxSize:1073741824)
    }  
    
    static transients = ['processInstance', 'MarshallerFromContext', 'env',
        'ProcessInstanceId', 'processInstanceByteArray']
    
    def long getId() {
        return id
    }
    
    def getProcessInstanceByteArray() {
        return data
    }
    
    def setProcessInstanceByteArray(value) {
        data = value
    }    
    
    def getProcessInstanceId() {
        return id
    }
    
    public void updateLastReadDate() {
        lastReadDate = new Date()
    }
    
    def ProcessInstance getProcessInstance(InternalKnowledgeRuntime kruntime, Environment env) {
        this.env = env;
        if ( processInstance == null ) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream( processInstanceByteArray );
                MarshallerReaderContext context = new MarshallerReaderContext( bais,
                        (InternalRuleBase) ((InternalKnowledgeBase) kruntime.getKnowledgeBase()).getRuleBase(),
                        null,
                        null,
                        this.env
                        );
                ProcessInstanceMarshaller marshaller = getMarshallerFromContext( context );
                context.wm = ((StatefulKnowledgeSessionImpl) kruntime).getInternalWorkingMemory();
                processInstance = marshaller.readProcessInstance(context);
                context.close();
            } catch ( IOException e ) {
                throw new IllegalStateException( "IOException while loading process instance: ", e );
            }
        }
        return processInstance;
    }
    
    private ProcessInstanceMarshaller getMarshallerFromContext(MarshallerReaderContext context) throws IOException {
        String processInstanceType = context.stream.readUTF()
        return ProcessMarshallerRegistry.INSTANCE.getMarshaller(processInstanceType)
    }
    
    private void saveProcessInstanceType(MarshallerWriteContext context, 
            ProcessInstance processInstance, String processInstanceType)
            throws IOException {
        // saves the processInstance type first
        context.stream.writeUTF(processInstanceType)
    }
    
    def beforeUpdate() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            MarshallerWriteContext context = new MarshallerWriteContext( baos,
                    null,
                    null,
                    null,
                    null,
                    this.env );
            String processType = ((ProcessInstanceImpl) processInstance).getProcess().getType();
            saveProcessInstanceType( context,
                    processInstance,
                    processType );
            ProcessInstanceMarshaller marshaller = ProcessMarshallerRegistry.INSTANCE.getMarshaller( processType );
            marshaller.writeProcessInstance( context,
                    processInstance);
            context.close();
        } catch ( IOException e ) {
            throw new IllegalStateException( 
            "IOException while storing process instance " + processInstance.getId(), e);
        }
        byte[] newByteArray = baos.toByteArray();
        if ( !Arrays.equals( newByteArray, getProcessInstanceByteArray())) {
            this.state = processInstance.getState();
            this.lastModificationDate = new Date();
            setProcessInstanceByteArray(newByteArray);
            if (this.eventTypes) {
                this.eventTypes.clear();
            }
            for ( String type : processInstance.getEventTypes() ) {
                eventTypes.add( type );
            }
        }
    }
}
