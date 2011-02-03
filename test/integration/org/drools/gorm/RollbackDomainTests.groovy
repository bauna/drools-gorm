package org.drools.gorm


import org.drools.KnowledgeBase
import org.drools.KnowledgeBaseFactory
import org.drools.SessionConfiguration;
import org.drools.builder.KnowledgeBuilder
import org.drools.builder.KnowledgeBuilderFactory
import org.drools.builder.ResourceType
import org.drools.event.process.ProcessStartedEvent 
import org.drools.gorm.impl.ProcessEventListenerAdapter 
import org.drools.gorm.processinstance.GormProcessInstanceManagerFactory;
import org.drools.gorm.processinstance.GormSignalManagerFactory;
import org.drools.gorm.processinstance.GormWorkItemManagerFactory;
import org.drools.gorm.session.SingleSessionCommandService;
import org.drools.gorm.test.DroolsTest;
import org.drools.io.ResourceFactory
import org.drools.runtime.Environment
import org.drools.runtime.StatefulKnowledgeSession
import org.drools.runtime.process.ProcessInstance
import org.drools.runtime.process.WorkItem
import org.jbpm.process.core.context.variable.VariableScope
import org.jbpm.process.instance.ContextInstance;

public class RollbackDomainTests extends GroovyTestCase {
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
    
    def testRollback() {
        def str = """
			package org.drools.test
			import org.drools.gorm.test.DroolsTest
			rule rule1
			when
				\$droolsTest : DroolsTest(value == 1)
			then
				modify(\$droolsTest) {
					setValue(2L)
			    }
			end
		"""
		
		KnowledgeBuilder kbuilder = KnowledgeBuilderFactory.newKnowledgeBuilder()
        kbuilder.add(ResourceFactory.newByteArrayResource(str.getBytes()),
                     ResourceType.DRL)
        KnowledgeBase kbase = KnowledgeBaseFactory.newKnowledgeBase()

        if (kbuilder.hasErrors()) {
            fail(kbuilder.getErrors().toString())
        }

        kbase.addKnowledgePackages(kbuilder.getKnowledgePackages())
        Environment env = KnowledgeBaseFactory.newEnvironment()
        
        StatefulKnowledgeSession ksession = null
        def fact1Id = null
        def fact1Handle = null
        
        DroolsTest.withTransaction { status ->
            env.set( "pepe", sessionFactory.currentSession )
        	ksession = 
        		kstore.newStatefulKnowledgeSession(kbase, 
                    getGORMSessionConfig(), 
                    env)

	        def fact1 = new DroolsTest(name:"fact4", value:1)
	        fact1.save(flush:true)
	        fact1Id = fact1.id
	        fact1Handle = ksession.insert(fact1)
        }	        
    	def sessionId = ksession.id
        
    	assertEquals(1, ksession.objects.size())
    	
    	def fact2Id = null
        def fact2Handle = null
        DroolsTest.withTransaction {status ->
            env.set( "pepe", sessionFactory.currentSession )
            def fact2 = new DroolsTest(name:"fact5", value:1)
	        fact2.save()  // without flush
	        fact2Id = fact2.id
	        fact2Handle = ksession.insert(fact2)
	        status.setRollbackOnly()
        }
	        
    	assertEquals(1, ksession.objects.size())
    	
        ksession.dispose()
        
        ksession = kstore.loadStatefulKnowledgeSession(sessionId, 
            kbase, getGORMSessionConfig(), env)
        assertEquals(1, ksession.objects.size())
        def fact1A = DroolsTest.get(fact1Id)
        assertEquals(1, fact1A.value)
        
        def num_domains = DroolsTest.list().size()
       	
        DroolsTest.withTransaction {status ->
            env.set( "pepe", sessionFactory.currentSession )
	        def fact2A = new DroolsTest(name:"fact5", value:1)
	        fact2A.save()
	        fact2Id = fact2A.id
	        fact2Handle = ksession.insert(fact2A)

	        assertEquals(2, ksession.objects.size())
	        
	        ksession.fireAllRules()
        	
	        assertEquals(2, fact1A.value)
	        assertEquals(2, fact2A.value)
	        
        	status.setRollbackOnly()
    	}
        
    	assertEquals(num_domains, DroolsTest.list().size())
        assertEquals(1, ksession.objects.size())

        fact1A = DroolsTest.get(fact1Id)
        assertEquals(1, fact1A.value)
        
        ksession.fireAllRules()
        fact1A = new ArrayList(ksession.getObjects()).get(0)
        assertEquals(2, fact1A.value)
                
        ksession.dispose()
    }
}
