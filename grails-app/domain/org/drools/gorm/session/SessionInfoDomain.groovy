package org.drools.gorm.session

import org.drools.gorm.session.marshalling.GormSessionMarshallingHelper
import org.drools.gorm.DomainUtils
import org.drools.gorm.GrailsIntegration

import java.sql.Blob
import org.hibernate.Hibernate

class SessionInfoDomain implements SessionInfo {

	int id
    Date startDate = new Date()
    Date lastModificationDate    
    Blob rulesBlob
    GormSessionMarshallingHelper marshallingHelper

    static constraints = {
    	lastModificationDate(nullable:true)
    	rulesBlob(nullable:true)
	}    
    
    static transients = ['marshallingHelper', 'data', 'rulesByteArray']

	static mapping = {
		rulesBlob type: 'blob'
	}	
	
    
    def getRulesByteArray() {
    	if (rulesBlob) {
    		return DomainUtils.blobToByteArray(rulesBlob)
    	}
    }

    def setRulesByteArray(value) {
    	rulesBlob = Hibernate.createBlob(value)
    }    
    
    public byte[] getData() {
        return this.getRulesByteArray()
    }
    
    def beforeInsert() {
        this.lastModificationDate = new Date()
        this.setRulesByteArray(this.marshallingHelper.getSnapshot())
    }
    
    def beforeUpdate() {
        // we always increase the last modification date for each action, so we know there will be an update
        byte[] newByteArray = this.marshallingHelper.getSnapshot()

        if ( !Arrays.equals( newByteArray,
                             this.getRulesByteArray() ) ) {
            this.lastModificationDate = new Date()
            this.setRulesByteArray(newByteArray)
        }
    }
}
