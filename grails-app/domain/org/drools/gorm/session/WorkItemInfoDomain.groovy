package org.drools.gorm.session


import org.drools.gorm.DomainUtils 
import org.drools.gorm.GrailsIntegration 
import org.drools.gorm.session.marshalling.GormMarshallerReaderContext 
import org.drools.marshalling.impl.InputMarshaller
import org.drools.marshalling.impl.MarshallerWriteContext
import org.drools.marshalling.impl.OutputMarshaller
import org.drools.process.instance.WorkItem
import org.drools.runtime.Environment;


import java.sql.Blob
import org.hibernate.Hibernate

public class WorkItemInfoDomain implements WorkItemInfo {

	Long id
    String name
    Date creationDate = new Date()
    long processInstanceId
    long state
    Blob workItemBlob
    
    static constraints = {
		workItemBlob(nullable:true)
	}  
    
    static transients = ['workItem', 'workItemByteArray']

	static mapping = {
		workItemBlob type: 'blob'
	}	
	
    WorkItem workItem
	
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
    
    public WorkItem getWorkItem() {
        if ( workItem == null ) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream( getWorkItemByteArray() )
                GormMarshallerReaderContext context = new GormMarshallerReaderContext( bais,
                                                                               null,
                                                                               null,
                                                                               null,
																			   null)
                workItem = InputMarshaller.readWorkItem( context )
                context.close()
            } catch ( IOException e ) {
                throw new IllegalArgumentException( 'IOException while loading process instance: ' + e.getMessage(), e)
            }
        }
        return workItem
    }

    public void update() {
    	def newState = workItem.getState()
        if (this.state != newState) {
        	this.state = newState
        	GrailsIntegration.getGORMDomainService().saveDomain(this)
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream()
        try {
            MarshallerWriteContext context = new MarshallerWriteContext( baos,
                                                                         null,
                                                                         null,
                                                                         null,
                                                                         null )
            OutputMarshaller.writeWorkItem( context,
                                            workItem )
            context.close()

            byte[] newByteArray = baos.toByteArray()
            if (!Arrays.equals(newByteArray, this.getWorkItemByteArray())) {
                this.setWorkItemByteArray(newByteArray)
            	GrailsIntegration.getGORMDomainService().saveDomain(this)
            }
        } catch ( IOException e ) {
            throw new IllegalArgumentException( 'IOException while storing workItem ' + workItem.getId() + ': ' + e.getMessage(), e)
        }
    }

}
