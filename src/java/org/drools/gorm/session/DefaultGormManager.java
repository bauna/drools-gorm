package org.drools.gorm.session;

import org.drools.gorm.GrailsIntegration;
import org.drools.persistence.session.JpaManager;
import org.drools.runtime.Environment;
import org.hibernate.Session;

public class DefaultGormManager
    implements JpaManager {

    public DefaultGormManager(Environment env) {
        //TODO {bauna} check if we need to store the environment.
//    	this.env = env;
    }    
    
    public Session getApplicationScopedEntityManager() {
    	return GrailsIntegration.getCurrentSession();
    }

    public Session getCommandScopedEntityManager() {
        return getApplicationScopedEntityManager();
    }    

    public void beginCommandScopedEntityManager() {
    	//TODO {bauna} do nothing for now.
    	//should we register for rollback here ?
    }

    public void endCommandScopedEntityManager() {
    	GrailsIntegration.flushCurrentSession();
    }

    public void dispose() {
       
    }

}
