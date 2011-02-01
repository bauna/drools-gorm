package org.drools.gorm.session

import org.drools.gorm.session.marshalling.GormSessionMarshallingHelper
import org.drools.gorm.DomainUtils
import org.drools.gorm.GrailsIntegration
import org.drools.runtime.Environment;

import java.io.OutputStream;
import java.sql.Blob
import java.util.Set;

import org.hibernate.Hibernate

class SessionInfoDomain implements SessionInfo {
    
    Integer id
    Date startDate = new Date()
    Date lastModificationDate    
    byte[] data
    boolean deleted = false
    GormSessionMarshallingHelper marshallingHelper
    
    Environment env
    
    static mapping = {
        version false
        table 'session_info'
    }
    
    static constraints = {
        lastModificationDate(nullable:true)
        data(nullable:true, maxSize:1073741824)
    }    
    
    static transients = ['marshallingHelper', 'rulesByteArray', 
        'env', 'tableName', 'deleted']
    
    public Integer getId() {
        return id;
    }
    
    def getRulesByteArray() {
        return data;
    }
    
    def setRulesByteArray(byte[] value) {
        data = value;
    }    
    
    public byte[] getData() {
        return this.getRulesByteArray()
    }
    
    def beforeDelete() {
        deleted = true;
    }
    
    def beforeInsert() {
        this.lastModificationDate = new Date()
        Set updates = env.get(GORM_UPDATE_SET);
        updates.add(this)
    }
    
    def beforeUpdate() {
        Set updates = env.get(GORM_UPDATE_SET);
        updates.add(this)
    }
    
    public byte[] generateBlob() {
        // we always increase the last modification date for each action, so we know there will be an update
        byte[] newByteArray = this.marshallingHelper.getSnapshot()
        
        if ( !Arrays.equals( newByteArray,
        this.getRulesByteArray() )) {
            this.lastModificationDate = new Date()
            this.setRulesByteArray(newByteArray)
            return newByteArray;
        } 
        return null;
    }
    
    public String getTableName() {
        return "session_info_domain";
    }
}
