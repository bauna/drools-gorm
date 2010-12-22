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
import org.drools.common.EndOperationListener;
import org.drools.common.InternalKnowledgeRuntime;
import org.drools.gorm.GrailsIntegration;
import org.drools.gorm.session.marshalling.GormSessionMarshallingHelper;
import org.drools.impl.KnowledgeBaseImpl;
import org.drools.persistence.session.JpaJDKTimerService;
import org.drools.process.instance.WorkItemManager;
import org.drools.runtime.Environment;
import org.drools.runtime.KnowledgeSessionConfiguration;
import org.drools.runtime.StatefulKnowledgeSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class SingleSessionCommandService
    implements
    org.drools.command.SingleSessionCommandService {
    
    private SessionInfo sessionInfo;
    private GormSessionMarshallingHelper marshallingHelper;

    private StatefulKnowledgeSession ksession;
    private Environment env;
    private KnowledgeCommandContext kContext;
    private volatile boolean doRollback;

    public void checkEnvironment(Environment env) {
       
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
        ((InternalKnowledgeRuntime) this.ksession).setEndOperationListener( new EndOperationListenerImpl() );        
        
        // Use the App scoped EntityManager if the user has provided it, and it is open.

        PlatformTransactionManager txManager = GrailsIntegration.getTransactionManager();
        DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
        txDef.setPropagationBehavior(DefaultTransactionDefinition.PROPAGATION_REQUIRED); 
        TransactionStatus status = txManager.getTransaction(txDef);
        try {
            this.env.set(SessionInfo.SAFE_GORM_COMMIT_STATE, false); 
            registerRollbackSync();
            
            GrailsIntegration.getGormDomainService().saveDomain(this.sessionInfo);
            this.env.set(SessionInfo.SAFE_GORM_COMMIT_STATE, true);
            txManager.commit(status);
        } catch ( Exception t1 ) {
            try {
                txManager.rollback(status);
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
            this.sessionInfo = GrailsIntegration.getGormDomainService().getSessionInfo(sessionId);
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

        ((InternalKnowledgeRuntime) this.ksession).setEndOperationListener( new EndOperationListenerImpl() );

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


    public class EndOperationListenerImpl implements EndOperationListener {

        public void endOperation(InternalKnowledgeRuntime kruntime) {
            SingleSessionCommandService.this.sessionInfo.setLastModificationDate( new Date( kruntime.getLastIdleTimestamp() ) );
        }
    }

    public Context getContext() {
        return this.kContext;
    }

    public synchronized <T> T execute(Command<T> command) {
        PlatformTransactionManager txManager = GrailsIntegration.getTransactionManager();
        DefaultTransactionDefinition txDef = new DefaultTransactionDefinition();
        txDef.setPropagationBehavior(DefaultTransactionDefinition.PROPAGATION_REQUIRED); 
        TransactionStatus status = txManager.getTransaction(txDef);

        try {
            this.env.set(SessionInfo.SAFE_GORM_COMMIT_STATE, false);
            initKsession( this.sessionInfo.getId(),
                          this.marshallingHelper.getKbase(),
                          this.marshallingHelper.getConf() );
            
            registerRollbackSync();

            T result = ((GenericCommand<T>) command).execute( this.kContext );
            this.env.set(SessionInfo.SAFE_GORM_COMMIT_STATE, true);
            txManager.commit(status);

            return result;
        } catch (RuntimeException e){
            status.setRollbackOnly();
            throw e;
        } catch ( Exception e ) {
            status.setRollbackOnly();
            throw new RuntimeException("Wrapped exception see cause", e);
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

    @SuppressWarnings("unchecked")
    private Map<Object, Object> getSyncronizationMap() {
        Map<Object, Object> map = (Map<Object, Object>) env.get("synchronizations");
        if ( map == null ) {
            map = Collections.synchronizedMap( new IdentityHashMap<Object, Object>() );
            env.set("synchronizations", map);
        }
        return map;
    }
    
    private void registerRollbackSync() throws IllegalStateException {
        Map<Object, Object> map = getSyncronizationMap();

        if (!map.containsKey( this )) {
            TransactionSynchronizationManager.registerSynchronization(new SynchronizationImpl());
            map.put(this, this);
        }
    }

    private class SynchronizationImpl
        implements TransactionSynchronization {

        @Override
        public void suspend() {}

        @Override
        public void resume() {}

        @Override
        public void flush() {}

        @Override
        public void beforeCommit(boolean readOnly) {}

        @Override
        public void beforeCompletion() {}

        @Override
        public void afterCommit() {}

        @Override
        public void afterCompletion(int status) {
            try {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    SingleSessionCommandService.this.rollback();
                }
                // clean up cached process and work item instances
                StatefulKnowledgeSession ksession = SingleSessionCommandService.this.ksession;
                if (ksession != null) {
                    ((InternalKnowledgeRuntime) ksession).getProcessRuntime().clearProcessInstances();
                    ((WorkItemManager) ksession.getWorkItemManager()).clear();
                }
            } finally {
                SingleSessionCommandService.this.getSyncronizationMap().remove(SingleSessionCommandService.this);
            }
        }
    }

    private void rollback() {
        this.doRollback = true;
    }
}