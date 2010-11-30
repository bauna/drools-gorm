package org.drools.gorm.session

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.drools.marshalling.impl.InputMarshaller;
import org.drools.marshalling.impl.MarshallerReaderContext;

import java.util.Date;

import org.drools.process.instance.WorkItem;
import org.drools.runtime.Environment;

import org.drools.gorm.DomainUtils 
import org.drools.gorm.GrailsIntegration 
import org.drools.gorm.session.marshalling.GormMarshallerReaderContext 
import org.drools.marshalling.impl.InputMarshaller
import org.drools.marshalling.impl.MarshallerWriteContext
import org.drools.marshalling.impl.OutputMarshaller
import org.drools.process.instance.WorkItem
import org.drools.runtime.Environment;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Blob
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.Hibernate

public class WorkItemInfoDomain implements WorkItemInfo {
	Long id
    String name
    Date creationDate = new Date()
    long processInstanceId
    long state
    Blob workItemBlob
    WorkItem workItem
    Environment env
    
    WorkItemInfoDomain() {}
    
    WorkItemInfoDomain(WorkItem workItem, Environment env) {
        this.workItem = workItem;
        this.name = workItem.getName();
        this.creationDate = new Date();
        this.processInstanceId = workItem.getProcessInstanceId();
        this.env = env;
    }
    
    static constraints = {
		workItemBlob(nullable:true)
	}  
    
    static transients = ['workItem', 'workItemByteArray', 'env']

	static mapping = {
		workItemBlob type: 'blob'
	}	
    
	def Long getId(){
		return id;
	}
	
	def void setId(Long id) {
		this.id = id
	}

    def getWorkItemByteArray() {
    	if (workItemBlob) {
    		return DomainUtils.blobToByteArray(workItemBlob)
    	}
    }

    def setWorkItemByteArray(value) {
    	this.setWorkItemBlob(Hibernate.createBlob(value))
    }    
    
    def WorkItem getWorkItem(Environment env) {
        this.env = env;
        if ( workItem == null ) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream( getWorkItemByteArray() );
                MarshallerReaderContext context = new MarshallerReaderContext( bais,
                    null,
                    null,
                    null,
                    env);
                workItem = InputMarshaller.readWorkItem( context );
                context.close();
            } catch ( IOException e ) {
                throw new IllegalStateException( "IOException while loading process instance: ", e);
            }
        }
        return workItem;
    }

    def beforeInsert() {
        beforeUpdate()
    }
    
    def beforeUpdate() {
        this.state = workItem.getState();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean variablesChanged = false;
        try {
            MarshallerWriteContext context = new MarshallerWriteContext( baos,
                null,
                null,
                null,
                null,
                this.env);
            
            OutputMarshaller.writeWorkItem( context,
            workItem );
            
            context.close();
            this.workItemByteArray = baos.toByteArray();
        } catch ( IOException e ) {
            throw new IllegalStateException( "IOException while storing workItem " + workItem.getId(), e);
        }
    }
}
