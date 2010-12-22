package org.drools.gorm.session;

import org.drools.gorm.GrailsIntegration;
import org.drools.runtime.Environment;
import org.hibernate.Session;

public class DefaultGormManager
    implements HibernateManager {

    public DefaultGormManager(Environment env) {

    }    
    
    public Session getApplicationScopedEntityManager() {
    	return GrailsIntegration.getCurrentSession();
    }

    public Session getCommandScopedEntityManager() {
        return getApplicationScopedEntityManager();
    }    

    public void beginCommandScopedEntityManager() {

    }

    public void endCommandScopedEntityManager() {
    	GrailsIntegration.flushCurrentSession();
    }

    public void dispose() {
       
    }

}
