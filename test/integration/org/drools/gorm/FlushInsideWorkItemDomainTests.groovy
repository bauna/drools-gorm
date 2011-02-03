package org.drools.gorm


import org.drools.KnowledgeBase
import org.drools.KnowledgeBaseFactory
import org.drools.SessionConfiguration;
import org.drools.builder.KnowledgeBuilder
import org.drools.builder.KnowledgeBuilderFactory
import org.drools.builder.ResourceType
import org.drools.common.AbstractRuleBase;
import org.drools.event.process.ProcessStartedEvent 
import org.drools.gorm.helper.ProcessCreatorForHelp;
import org.drools.gorm.impl.ProcessEventListenerAdapter 
import org.drools.gorm.processinstance.GormProcessInstanceManagerFactory;
import org.drools.gorm.processinstance.GormSignalManagerFactory;
import org.drools.gorm.processinstance.GormWorkItemManagerFactory;
import org.drools.gorm.session.SingleSessionCommandService;
import org.drools.gorm.test.DroolsTest;
import org.drools.impl.InternalKnowledgeBase;
import org.drools.io.ResourceFactory
import org.drools.runtime.Environment
import org.drools.runtime.StatefulKnowledgeSession
import org.drools.runtime.process.ProcessInstance
import org.drools.runtime.process.WorkItem
import org.drools.runtime.process.WorkItemManager;
import org.jbpm.process.core.context.variable.VariableScope
import org.jbpm.process.instance.ContextInstance;
import org.drools.process.instance.WorkItemHandler;

public class FlushInsideWorkItemDomainTests extends GroovyTestCase {
	static transactional = false
    def sessionFactory

	def kstore
   
    def void setUp() {
        def all = DroolsTest.findAll();
        for (a in all) {
            a.delete(flush: true)
        }
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
        
    def testFlush() {
        def processId = "org.drools.grails.TestWI"
        def workItemName = "wiSaveAndFlush"
        def workItemId = null
        
        
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase();
        ((AbstractRuleBase) ((InternalKnowledgeBase) kbase).getRuleBase())
                .addProcess( ProcessCreatorForHelp.newProcessWithOneWork(processId, workItemName) );
        
        Environment env = KnowledgeBaseFactory.newEnvironment()
        
        WorkItemHandler wih = new WorkItemHandler() {
            
            @Override
            public void executeWorkItem(WorkItem wi, WorkItemManager wim) {
                workItemId = wi.id
                def droolTest = new DroolsTest(name:"factWI", value:1)
                droolTest.save(flush: true)
            }
            
            @Override
            public void abortWorkItem(WorkItem wi, WorkItemManager wim) {
            }
        }
        def id
        StatefulKnowledgeSession ksession
        def processInstanceId
        ProcessInstance processInstance
        
        DroolsTest.withTransaction{ status ->
            ksession = kstore.newStatefulKnowledgeSession( kbase, null, env )
                
            id = ksession.id 
                    
            ksession.getWorkItemManager().registerWorkItemHandler(workItemName, 
                wih);
    
            processInstance = ksession.startProcess( processId )
            processInstanceId = processInstance.id
            assertNotNull workItemId
            ksession.dispose()
        
        }

//        this.restartDbSession() //----------------------------------------------

        ksession = kstore.loadStatefulKnowledgeSession( id, kbase, null, env )
        processInstance = ksession.getProcessInstance( processInstanceId )
        assertNotNull( processInstance )

        ksession.dispose()

//        this.restartDbSession() //----------------------------------------------
        
        ksession = kstore.loadStatefulKnowledgeSession( id, kbase, null, env )
        processInstance = ksession.getProcessInstance( processInstanceId )

        ksession.getWorkItemManager().registerWorkItemHandler(workItemName, 
            wih);

        ksession.getWorkItemManager().completeWorkItem(workItemId, null);
        
        ksession.dispose()

//        this.restartDbSession() //----------------------------------------------

        ksession = kstore.loadStatefulKnowledgeSession( id, kbase, null, env )
        processInstance = ksession.getProcessInstance( processInstanceId )
        assertNull( processInstance )
        ksession.dispose()
    }
}
