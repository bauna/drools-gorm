package org.drools.gorm.session;

import org.drools.gorm.GrailsIntegration;
import org.drools.persistence.session.JpaManager;
import org.drools.runtime.Environment;
import org.hibernate.Session;

public class DefaultJpaManager
    implements JpaManager {
    Environment                  env;

    public DefaultJpaManager(Environment env) {
        //TODO {bauna} check if we need to store the environment.
    	this.env = env;
    }    
    
    public Session getApplicationScopedEntityManager() {
    	return GrailsIntegration.getCurrentSession();
//        if ( this.appScopedEntityManager == null ) {
//            // Use the App scoped EntityManager if the user has provided it, and it is open.
//            this.appScopedEntityManager = (EntityManager) this.env.get( EnvironmentName.APP_SCOPED_ENTITY_MANAGER );
//            if ( this.appScopedEntityManager != null && !this.appScopedEntityManager.isOpen() ) {
//                throw new RuntimeException("Provided APP_SCOPED_ENTITY_MANAGER is not open");
//            }
//            
//            if ( this.appScopedEntityManager == null ) {
//                internalAppScopedEntityManager = true;
//                this.appScopedEntityManager = this.emf.createEntityManager();
//
//                this.env.set( EnvironmentName.APP_SCOPED_ENTITY_MANAGER,
//                              this.appScopedEntityManager );
//            } else {
//                internalAppScopedEntityManager = false;
//            }            
//        }
//        return appScopedEntityManager;
    }

    public Session getCommandScopedEntityManager() {
        return getApplicationScopedEntityManager();
    }    

    public void beginCommandScopedEntityManager() {
    	//TODO {bauna} do nothing for now.
    	
    	
//        EntityManager cmdScopedEntityManager = (EntityManager) env.get( EnvironmentName.CMD_SCOPED_ENTITY_MANAGER );
//        if ( cmdScopedEntityManager == null || !cmdScopedEntityManager.isOpen() ) {
//            internalCmdScopedEntityManager = true;
//            cmdScopedEntityManager = this.emf.createEntityManager(); // no need to call joinTransaction as it will do so if one already exists
//            this.env.set( EnvironmentName.CMD_SCOPED_ENTITY_MANAGER,
//                          cmdScopedEntityManager );
//        } else {
//            internalCmdScopedEntityManager = false;
//        }
//        cmdScopedEntityManager.joinTransaction();
//        appScopedEntityManager.joinTransaction();
    }

    public void endCommandScopedEntityManager() {
//        if ( this.internalCmdScopedEntityManager ) {
//            this.env.set( EnvironmentName.CMD_SCOPED_ENTITY_MANAGER, 
//                          null );
//        }
    }

    public void dispose() {
//        if ( this.internalAppScopedEntityManager ) {
//            if (  this.appScopedEntityManager != null && this.appScopedEntityManager.isOpen() ) {
//                this.appScopedEntityManager.close();
//            }
//            this.internalAppScopedEntityManager = false;
//            this.env.set( EnvironmentName.APP_SCOPED_ENTITY_MANAGER, null );
//        }
//        
//        if ( this.internalCmdScopedEntityManager ) {
//            if (  this.cmdScopedEntityManager != null && this.cmdScopedEntityManager.isOpen() ) {
//                this.cmdScopedEntityManager.close();
//            }
//            this.internalCmdScopedEntityManager = false;
//            this.env.set( EnvironmentName.CMD_SCOPED_ENTITY_MANAGER, null );            
//        }        
    }

}
