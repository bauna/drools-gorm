package org.drools.gorm

import org.drools.gorm.processinstance.GormSignalManagerFactory;
import org.drools.gorm.processinstance.GormWorkItemManagerFactory;
import org.drools.gorm.processinstance.GormProcessInstanceManagerFactory;
import org.drools.gorm.session.SingleSessionCommandService;
import org.drools.gorm.test.DroolsTest;
import org.drools.io.ResourceFactory
import org.drools.builder.KnowledgeBuilder
import org.drools.builder.KnowledgeBuilderFactory
import org.drools.builder.ResourceType 
import org.drools.SessionConfiguration
import org.drools.KnowledgeBase
import org.drools.KnowledgeBaseFactory
import org.drools.base.MapGlobalResolver
import org.drools.runtime.Environment
import org.drools.runtime.EnvironmentName
import org.drools.runtime.StatefulKnowledgeSession
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.orm.hibernate3.SessionHolder
import org.springframework.orm.hibernate3.SessionFactoryUtils 

import org.hibernate.FlushMode

public class DroolsTestCase extends GroovyTestCase {
    
    def sessionFactory
    def kstore
    
    public void setUp() {
        def all = DroolsTest.findAll();
        for (a in all) {
            a.delete(flush: true)
        }
            
    }
    
    def getDroolsResource(filename) {
        def url = getClass().getResource(filename)
        return ResourceFactory.newUrlResource(url)
    }
    
    def setupKSession(resources) {
        KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder()
        resources.each {
            kbuilder.add(this.getDroolsResource(it), 
                ResourceType.determineResourceType(it))
        }
        
        if (kbuilder.hasErrors()) {
            fail(kbuilder.getErrors().toString())
        }
        
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase()
        kbase.addKnowledgePackages( kbuilder.getKnowledgePackages() )
        
        Environment env = KnowledgeBaseFactory.newEnvironment()
        
        StatefulKnowledgeSession ksession = kstore.newStatefulKnowledgeSession( kbase, null, env )
        
        return [kbase, ksession, ksession.id, env]
    }
    
    def registerWorkItemHandler(StatefulKnowledgeSession ksession) {
        
        TestWorkItemHandler handler = TestWorkItemHandler.getInstance()
        ksession.getWorkItemManager().registerWorkItemHandler("MyWork", handler)
        ksession.getWorkItemManager().registerWorkItemHandler("Human Task", handler)
        ksession.getWorkItemManager().registerWorkItemHandler("Log", handler)
        return handler
    }
    
    def registerWorkItemHandler(SingleSessionCommandService service) {
        
        def ksession = service.ksession
        TestWorkItemHandler handler = TestWorkItemHandler.getInstance();
        ksession.getWorkItemManager().registerWorkItemHandler("MyWork", handler)
        ksession.getWorkItemManager().registerWorkItemHandler("Human Task", handler)
        ksession.getWorkItemManager().registerWorkItemHandler("Log", handler)
        return handler
    }
    
    def restartDbSession() {
        System.out.println("closing DB Session... ")
        this.unbindSession()
        this.bindSession()
        System.out.println("... DB Session restarted")
    }
    
    def getGORMSessionConfig() {
        Properties properties = new Properties();
        
        properties.setProperty( "drools.commandService",
        SingleSessionCommandService.class.getName() );
        properties.setProperty( "drools.processInstanceManagerFactory", 
        GormProcessInstanceManagerFactory.class.getName() );
        properties.setProperty( "drools.workItemManagerFactory", 
        GormWorkItemManagerFactory.class.getName() );
        properties.setProperty( "drools.processSignalManagerFactory",
			GormSignalManagerFactory.class.getName() );
        
        return new SessionConfiguration(properties);
    }    
    
    /**
     * Bind hibernate session to current thread
     */
    private boolean bindSession() {
        if(sessionFactory == null) {
            throw new IllegalStateException("No sessionFactory property provided")
        }
        final Object inStorage = TransactionSynchronizationManager.getResource(sessionFactory)
        if(inStorage != null) {
            ((SessionHolder)inStorage).getSession().flush()
            return false
        } else {
            def session = SessionFactoryUtils.getSession(sessionFactory, true)
            session.setFlushMode(FlushMode.AUTO)
            TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(session))
            return true
        }
    }
    
    /**
     * Bind hibernate session to current thread
     */
    private void unbindSession() {
        if(sessionFactory == null) {
            throw new IllegalStateException("No sessionFactory property provided")
        }
        try {
            final SessionHolder sessionHolder = (SessionHolder) TransactionSynchronizationManager.unbindResource(sessionFactory)
            if(!FlushMode.MANUAL.equals(sessionHolder.getSession().getFlushMode())) {
                sessionHolder.getSession().flush()
            }
            SessionFactoryUtils.closeSession(sessionHolder.getSession())
        } catch(Exception e) {
            //todo the catch clause here might not be necessary as the only call to unbindSession() is wrapped in a try block already
            fireThreadException(e)
        }
    }         
}
