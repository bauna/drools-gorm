package org.drools.gorm.session

import org.drools.gorm.session.marshalling.GormSessionMarshallingHelper
import org.drools.gorm.DomainUtils
import org.drools.gorm.GrailsIntegration
import org.drools.runtime.Environment;

import java.io.OutputStream;
import java.sql.Blob
import org.hibernate.Hibernate

class SessionInfoDomain implements SessionInfo {

	int id
    Date startDate = new Date()
    Date lastModificationDate    
    byte[] data
    GormSessionMarshallingHelper marshallingHelper
    
    Environment env

    static constraints = {
    	lastModificationDate(nullable:true)
        data(nullable:true, maxSize:1073741824)
	}    
    
    static transients = ['marshallingHelper', 'rulesByteArray', 'env']
    
    def getRulesByteArray() {
    	return data;
    }

    def setRulesByteArray(byte[] value) {
        data = value;
    }    
    
    public byte[] getData() {
        return this.getRulesByteArray()
    }
    
    def beforeInsert() {
        this.lastModificationDate = new Date()
        //this.setRulesByteArray(this.marshallingHelper.getSnapshot())
        generateBlob(true)
    }
    
    def beforeUpdate() {
        generateBlob(this.env.get(SessionInfo.SAFE_GORM_COMMIT_STATE))
    }
	
	public void generateBlob(boolean safeToCommit) {
		if (safeToCommit) {
			// we always increase the last modification date for each action, so we know there will be an update
			byte[] newByteArray = this.marshallingHelper.getSnapshot()
			
			if ( !Arrays.equals( newByteArray,
			this.getRulesByteArray() )) {
				this.lastModificationDate = new Date()
				this.setRulesByteArray(newByteArray)
			}
		} 
	}
}
