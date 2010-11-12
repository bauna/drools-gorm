package org.drools.gorm.session;

import java.util.Collections;
import java.util.Date;
import java.util.IdentityHashMap;
import java.util.Map;

import org.drools.KnowledgeBase;
import org.drools.RuleBase;
import org.drools.SessionConfiguration;
import org.drools.command.Command;
import org.drools.command.Context;
import org.drools.command.impl.ContextImpl;
import org.drools.command.impl.GenericCommand;
import org.drools.command.impl.KnowledgeCommandContext;
import org.drools.command.runtime.DisposeCommand;
import org.drools.common.EndOperationListener;
import org.drools.common.InternalKnowledgeRuntime;
import org.drools.gorm.GrailsIntegration;
import org.drools.gorm.impl.GormDroolsTransactionManager;
import org.drools.gorm.session.marshalling.GormSessionMarshallingHelper;
import org.drools.impl.KnowledgeBaseImpl;
import org.drools.persistence.session.JpaJDKTimerService;
import org.drools.persistence.session.JpaManager;
import org.drools.persistence.session.TransactionManager;
import org.drools.persistence.session.TransactionSynchronization;
import org.drools.process.instance.WorkItemManager;
import org.drools.runtime.Environment;
import org.drools.runtime.EnvironmentName;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SingleSessionCommandService
    implements
    org.drools.command.SingleSessionCommandService {
    
    Logger                               logger                                            = LoggerFactory.getLogger( getClass() );    

    private SessionInfo                 sessionInfo;
    private GormSessionMarshallingHelper marshallingHelper;

    private StatefulKnowledgeSession    ksession;
    private Environment                 env;
    private KnowledgeCommandContext     kContext;

    private TransactionManager          txm;
    private JpaManager                  jpm;
    
    private volatile boolean  doRollback;
    
    private static Map<Object, Object> synchronizations = Collections.synchronizedMap( new IdentityHashMap<Object, Object>() );
    
//    public static Map<Object, Object> txManagerClasses = Collections.synchronizedMap( new IdentityHashMap<Object, Object>() );

    public void checkEnvironment(Environment env) {
        if ( env.get( EnvironmentName.ENTITY_MANAGER_FACTORY ) == null ) {
            throw new IllegalArgumentException( "Environment must have an EntityManagerFactory" );
        }   
    }

    public SingleSessionCommandService(RuleBase ruleBase,
                                       SessionConfiguration conf,
                                       Environment env) {
        this( new KnowledgeBaseImpl( ruleBase ),
              conf,
              env );
    }

    public SingleSessionCommandService(int sessionId,
                                       RuleBase ruleBase,
                                       SessionConfiguration conf,
                                       Environment env) {
        this( sessionId,
              new KnowledgeBaseImpl( ruleBase ),
              conf,
              env );
    }

    public SingleSessionCommandService(KnowledgeBase kbase,
                                       KnowledgeSessionConfiguration conf,
                                       Environment env) {
        if ( conf == null ) {
            conf = new SessionConfiguration();
        }
        this.env = env;        
        
        checkEnvironment( this.env );        
        
        this.sessionInfo = GrailsIntegration.getGormDomainService().getNewSessionInfo();

        initTransactionManager( this.env );
        
        // create session but bypass command service
        this.ksession = kbase.newStatefulKnowledgeSession(conf, this.env);
        
        this.kContext = new KnowledgeCommandContext( new ContextImpl( "ksession",
                                                                      null ),
                                                     null,
                                                     null,
                                                     this.ksession,
                                                     null );

        ((JpaJDKTimerService) ((InternalKnowledgeRuntime) ksession).getTimerService()).setCommandService( this );
        
        this.marshallingHelper = new GormSessionMarshallingHelper( this.ksession,
                                                                  conf );
        this.sessionInfo.setMarshallingHelper( this.marshallingHelper );
        ((InternalKnowledgeRuntime) this.ksession).setEndOperationListener( new EndOperationListenerImpl( this.sessionInfo ) );        
        
        // Use the App scoped EntityManager if the user has provided it, and it is open.

        try {
            this.txm.begin();
 
            //this.appScopedEntityManager.joinTransaction();
            registerRollbackSync();

            jpm.getApplicationScopedEntityManager().persist( this.sessionInfo );

            this.txm.commit();

        } catch ( Exception t1 ) {
            try {
                this.txm.rollback();
            } catch ( Throwable t2 ) {
                throw new RuntimeException( "Could not commit session or rollback",
                                            t2 );
            }
            throw new RuntimeException( "Could not commit session",
                                        t1 );
        }

        // update the session id to be the same as the session info id
        ((InternalKnowledgeRuntime) ksession).setId( this.sessionInfo.getId() );

    }

    public SingleSessionCommandService(int sessionId,
                                       KnowledgeBase kbase,
                                       KnowledgeSessionConfiguration conf,
                                       Environment env) {
        if ( conf == null ) {
            conf = new SessionConfiguration();
        }
                

        this.env = env;
        
        checkEnvironment( this.env );
        
        initTransactionManager( this.env );

        initKsession( sessionId,
                      kbase,
                      conf );
    }

    public void initKsession(int sessionId,
                             KnowledgeBase kbase,
                             KnowledgeSessionConfiguration conf) {
        if ( !doRollback && this.ksession != null ) {
            return;
            // nothing to initialise
        }
        
        this.doRollback = false;       

        try {
            this.sessionInfo = new GORMDomainService().getSessionInfo(sessionId);
        } catch ( Exception e ) {
            throw new RuntimeException( "Could not find session data for id " + sessionId,
                                        e );
        }

        if ( sessionInfo == null ) {
            throw new RuntimeException( "Could not find session data for id " + sessionId );
        }

        if ( this.marshallingHelper == null ) {
            // this should only happen when this class is first constructed
            this.marshallingHelper = new GormSessionMarshallingHelper( kbase,
                                                                      conf,
                                                                      env );
        }

        this.sessionInfo.setMarshallingHelper( this.marshallingHelper );

        // if this.ksession is null, it'll create a new one, else it'll use the existing one
        this.ksession = this.marshallingHelper.loadSnapshot( this.sessionInfo.getData(),
                                                             this.ksession );

        // update the session id to be the same as the session info id
        ((InternalKnowledgeRuntime) ksession).setId( this.sessionInfo.getId() );

        ((InternalKnowledgeRuntime) this.ksession).setEndOperationListener( new EndOperationListenerImpl( this.sessionInfo ) );

        ((JpaJDKTimerService) ((InternalKnowledgeRuntime) ksession).getTimerService()).setCommandService( this );
        
        if ( this.kContext == null ) {
            // this should only happen when this class is first constructed
            this.kContext = new KnowledgeCommandContext( new ContextImpl( "ksession",
                                                                          null ),
                                                         null,
                                                         null,
                                                         this.ksession,
                                                         null );
        }

    }
    
    public void initTransactionManager(Environment env) {
    	jpm = new DefaultGormManager(env);
    	txm = new GormDroolsTransactionManager(GrailsIntegration.getTransactionManager());
    	
//        Object tm = env.get( EnvironmentName.TRANSACTION_MANAGER );
//        if ( tm != null && tm.getClass().getName().startsWith( "org.springframework" ) ) {
//            try {
//                Class<?> cls = Class.forName( "org.drools.container.spring.beans.persistence.DroolsSpringTransactionManager" );
//                Constructor<?> con = cls.getConstructors()[0];
//                this.txm = (TransactionManager) con.newInstance( tm );
//                logger.debug( "Instantiating  DroolsSpringTransactionManager" );
//                                
//                if ( tm.getClass().getName().toLowerCase().contains( "jpa" ) ) {
//                    // configure spring for JPA and local transactions
//                    cls = Class.forName( "org.drools.container.spring.beans.persistence.DroolsSpringJpaManager" );
//                    con = cls.getConstructors()[0];
//                    this.jpm =  ( JpaManager) con.newInstance( new Object[] { this.env } );
//                } else {
//                    // configure spring for JPA and distributed transactions 
//                }
//            } catch ( Exception e ) {
//                logger.warn( "Could not instatiate DroolsSpringTransactionManager" );
//                throw new RuntimeException( "Could not instatiate org.drools.container.spring.beans.persistence.DroolsSpringTransactionManager", e );
//            }
//        } else {
//            logger.debug( "Instantiating  JtaTransactionManager" );
//            this.txm = new JtaTransactionManager( env.get( EnvironmentName.TRANSACTION ),
//                                                  env.get( EnvironmentName.TRANSACTION_SYNCHRONIZATION_REGISTRY ),
//                                                  tm ); 
//            this.jpm = new DefaultGormManager(this.env);
//        }
    }

    public static class EndOperationListenerImpl
        implements
        EndOperationListener {
        private SessionInfo info;

        public EndOperationListenerImpl(SessionInfo info) {
            this.info = info;
        }

        public void endOperation(InternalKnowledgeRuntime kruntime) {
            this.info.setLastModificationDate( new Date( kruntime.getLastIdleTimestamp() ) );
        }
    }

    public Context getContext() {
        return this.kContext;
    }

    public synchronized <T> T execute(Command<T> command) {
        try {
            txm.begin();
            
            initKsession( this.sessionInfo.getId(),
                          this.marshallingHelper.getKbase(),
                          this.marshallingHelper.getConf() );
            
            this.jpm.beginCommandScopedEntityManager();

            registerRollbackSync();

            T result = ((GenericCommand<T>) command).execute( this.kContext );

            txm.commit();

            return result;

        }catch (RuntimeException re){
            rollbackTransaction(re);
            throw re;
        } catch ( Exception t1 ) {
            rollbackTransaction(t1);
            throw new RuntimeException("Wrapped exception see cause", t1);
        } finally {
            if ( command instanceof DisposeCommand ) {
                this.jpm.dispose();
            }
        }
    }

    private void rollbackTransaction(Exception t1) {
        try {
            logger.error( "Could not commit session", t1 );
            txm.rollback();
        } catch ( Exception t2 ) {
            logger.error( "Could not rollback", t2 );
            throw new RuntimeException( "Could not commit session or rollback", t2 );
        }
    }

    public void dispose() {
        if ( ksession != null ) {
            ksession.dispose();
        }
    }

    public int getSessionId() {
        return sessionInfo.getId();
    }

    private void registerRollbackSync() {
        if ( synchronizations.get( this ) == null ) {
            txm.registerTransactionSynchronization( new SynchronizationImpl() );
            synchronizations.put( this,
                                  this );
        }

    }

    private class SynchronizationImpl
        implements
        TransactionSynchronization {


        public void afterCompletion(int status) {
            if ( status != TransactionManager.STATUS_COMMITTED ) {
                SingleSessionCommandService.this.rollback();                
            }

            // always cleanup thread local whatever the result
            SingleSessionCommandService.synchronizations.remove( SingleSessionCommandService.this );
            
            try {
				SingleSessionCommandService.this.jpm.endCommandScopedEntityManager();
			} catch (Exception e) {
				logger.error("afterCompletion endCommandScopedEntityManager()" , e);
			}

            StatefulKnowledgeSession ksession = SingleSessionCommandService.this.ksession;
            // clean up cached process and work item instances
            if ( ksession != null ) {
                ((InternalKnowledgeRuntime) ksession).getProcessRuntime().clearProcessInstances();
                ((WorkItemManager) ksession.getWorkItemManager()).clear();
            }

        }

        public void beforeCompletion() {

        }

    }

    private void rollback() {
        this.doRollback = true;
    }
}