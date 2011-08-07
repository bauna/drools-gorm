package org.drools.gorm


import org.drools.KnowledgeBase
import org.drools.KnowledgeBaseFactory
import org.drools.builder.KnowledgeBuilder
import org.drools.builder.KnowledgeBuilderFactory
import org.drools.builder.ResourceType
import org.drools.event.process.ProcessStartedEvent 
import org.drools.gorm.impl.ProcessEventListenerAdapter 
import org.drools.gorm.test.DroolsTest;
import org.drools.io.ResourceFactory
import org.drools.runtime.Environment
import org.drools.runtime.StatefulKnowledgeSession
import org.drools.runtime.process.ProcessInstance
import org.drools.runtime.process.WorkItem
import org.jbpm.process.core.context.variable.VariableScope
import org.jbpm.process.instance.ContextInstance;

public class PersistentDomainTests extends DroolsTestCase {

	static transactional = false
    boolean isOK = true
	
    public void testSimpleDomainPersistence() {
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

        StatefulKnowledgeSession ksession = 
        	kstore.newStatefulKnowledgeSession(kbase, null, env)
        def sessionId = ksession.id

        def fact1 = new DroolsTest(name:"fact1", value:1)
        fact1.save(flush:true)
        def fact1Id = fact1.id
        def fact1Handle = ksession.insert(fact1)
        def fact2 = new DroolsTest(name:"fact2", value:1)
        fact2.save()  // without flush
        def fact2Id = fact2.id
        def fact2Handle = ksession.insert(fact2)

        ksession.fireAllRules()

        ksession.dispose()

        this.restartDbSession()
        
        ksession = kstore.loadStatefulKnowledgeSession(sessionId, 
                kbase, null, env)
        ksession.fireAllRules()
        
        assertEquals(2, ksession.objects.size())
        
        def fact1A = DroolsTest.get(fact1Id)
        def factA2 = DroolsTest.get(fact2Id)
        
        assertEquals(2, fact1A.value)
        assertEquals(2, factA2.value)

        ksession.dispose()
    }

    public void testDomainPersistence() {
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

			rule rule2
			when
				\$droolsTest : DroolsTest(value == 3)
			then
				modify(\$droolsTest) {
					setValue(4L)
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

        StatefulKnowledgeSession ksession = 
        	kstore.newStatefulKnowledgeSession(kbase, null, env)
        def sessionId = ksession.id

        def fact1 = new DroolsTest(name:"fact3", value:1)
        fact1.save(flush:true)
        def fact1Id = fact1.id
        def fact1Handle = ksession.insert(fact1)

        ksession.dispose()

        this.restartDbSession()
        
        ksession = kstore.loadStatefulKnowledgeSession(sessionId, 
            kbase, null, env)

        def fact1A = DroolsTest.get(fact1Id)
        assertNotNull(fact1A)
        assertNotSame(fact1A, fact1) 			
        
        assertEquals(1, ksession.objects.size())
        assertEquals(1, fact1A.value)														
        ksession.fireAllRules()
        assertEquals(1, ksession.objects.size())
//        fact1A = new ArrayList(ksession.getObjects()).get(0)
        assertEquals(2, fact1A.value)
        
        // rule2 will not fire because we didn't notify the session about the update
        fact1A.value = 3
        ksession.fireAllRules()
        assertEquals(3, fact1A.value)
        
        // notify the session about the update and rule2 will fire
        //FIXME when Drools 5.3.0 is released. 
//        ksession.update(fact1Handle, fact1A)
//        ksession.fireAllRules()
//        assertEquals(4, fact1A.value)
        
        ksession.dispose()
    }
    
    public void testRollback() {
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
        	ksession = 
        		kstore.newStatefulKnowledgeSession(kbase, null, env)

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
        	def fact2 = new DroolsTest(name:"fact5", value:1)
	        fact2.save()  // without flush
	        fact2Id = fact2.id
	        fact2Handle = ksession.insert(fact2)
	        status.setRollbackOnly()
        }
	        
    	assertEquals(1, ksession.objects.size())
    	
        ksession.dispose()

        this.restartDbSession()
        
        ksession = kstore.loadStatefulKnowledgeSession(sessionId, 
            kbase, null, env)
        assertEquals(1, ksession.objects.size())
        def fact1A = DroolsTest.get(fact1Id)
        assertEquals(1, fact1A.value)
        
        def num_domains = DroolsTest.list().size()
       	
        DroolsTest.withTransaction {status ->
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

    public void testPersistenceVariables() {
		def (kbase, ksession, id, env) = this.setupKSession(["DomainVariablesProcess.rf"])
		def handler = this.registerWorkItemHandler(ksession)
        
        Map<String, Object> parameters = new HashMap<String, Object>()
        
        def var1 = new DroolsTest(name:"var1", value:1)
		var1.save()
        def var1Id = var1.id
		
        parameters.put("name", var1)
        
        ksession.addEventListener( new ProcessEventListenerAdapter() {
            @Override
            public void afterProcessStarted(ProcessStartedEvent event) {
                PersistentDomainTests.this.isOK = event.getProcessInstance()
                    .getContextInstance(VariableScope.VARIABLE_SCOPE)
                        .getVariable("name").equals(DroolsTest.get(var1Id))
            }
        })
        
        assertTrue(isOK)
        
        ProcessInstance processInstance = ksession.startProcess( "org.drools.test.TestProcess", parameters )
        def processInstanceId = processInstance.id

        WorkItem workItem = handler.getWorkItem()
        def workItemId = workItem.id
        assertNotNull( workItem )
        assertEquals( var1, workItem.getParameter("name"))
        
        ksession.dispose()

        this.restartDbSession()
        isOK = false
        ksession = kstore.loadStatefulKnowledgeSession( id, kbase, null, env )
        processInstance = ksession.getProcessInstance( processInstanceId )
        
        assertNotNull( processInstance )

        assertTrue(var1 != DroolsTest.get(var1Id))
        
        ksession.dispose()

        this.restartDbSession() //----------------------------------------------
        
        ksession = kstore.loadStatefulKnowledgeSession( id, kbase, null, env )
        ksession.addEventListener( new ProcessEventListenerAdapter() {
            @Override
            public void afterProcessCompleted(org.drools.event.process.ProcessCompletedEvent event) {
                PersistentDomainTests.this.isOK = event.getProcessInstance()
                    .getContextInstance(VariableScope.VARIABLE_SCOPE)
                        .getVariable("name").equals(DroolsTest.get(var1Id))
            }
        })
        
        processInstance = ksession.getProcessInstance( processInstanceId )

        handler = this.registerWorkItemHandler(ksession)

//        assertEquals(processInstance.getContextInstance(VariableScope.VARIABLE_SCOPE).getVariable("name"), DroolsTest.get(var1Id))
        
        ksession.getWorkItemManager().completeWorkItem( workItemId, null )
        workItem = handler.getWorkItem()
        workItemId = workItem.id
        assertNotNull( workItem )
        assertEquals( DroolsTest.get(var1Id), workItem.getParameter("text"))

        ksession.dispose()

        this.restartDbSession() //----------------------------------------------
        
        ksession = kstore.loadStatefulKnowledgeSession( id, kbase, null, env )
        processInstance = ksession.getProcessInstance( processInstanceId )
        assertNotNull( processInstance )

        ksession.dispose()

        this.restartDbSession() //----------------------------------------------

        ksession = kstore.loadStatefulKnowledgeSession( id, kbase, null, env )
        handler = this.registerWorkItemHandler(ksession)
        
        ksession.getWorkItemManager().completeWorkItem( workItemId, null )

        workItem = handler.getWorkItem()
        assertNull( workItem )

        ksession.dispose()

        this.restartDbSession() //----------------------------------------------

        ksession = kstore.loadStatefulKnowledgeSession( id, kbase, null, env )
        processInstance = ksession.getProcessInstance( processInstanceId )
        assertNull( processInstance )
        ksession.dispose()
    }

    public void testAbortWorkItem() {
		def (kbase, ksession, id, env) = this.setupKSession(["DomainVariablesProcess.rf"])
		def handler = this.registerWorkItemHandler(ksession)

        ProcessInstance processInstance = ksession.startProcess( "org.drools.test.TestProcess")
        def processInstanceId = processInstance.id

        WorkItem workItem = handler.getWorkItem()
        def workItemId = workItem.id
        assertNotNull( workItem )
        
        ksession.dispose()

        this.restartDbSession() //----------------------------------------------

        ksession = kstore.loadStatefulKnowledgeSession( id, kbase, null, env )
        processInstance = ksession.getProcessInstance( processInstanceId )
        assertNotNull( processInstance )

        ksession.dispose()

        this.restartDbSession() //----------------------------------------------
        
        ksession = kstore.loadStatefulKnowledgeSession( id, kbase, null, env )
        processInstance = ksession.getProcessInstance( processInstanceId )

        handler = this.registerWorkItemHandler(ksession)

        ksession.getWorkItemManager().abortWorkItem( workItemId )
        
        workItem = handler.getWorkItem()
        workItemId = workItem.id
        assertNotNull( workItem )

        ksession.dispose()

        this.restartDbSession() //----------------------------------------------

        ksession = kstore.loadStatefulKnowledgeSession( id, kbase, null, env )
        handler = this.registerWorkItemHandler(ksession)
        
        ksession.getWorkItemManager().completeWorkItem( workItemId, null )

        workItem = handler.getWorkItem()
        assertNull( workItem )

        ksession.dispose()

        this.restartDbSession() //----------------------------------------------

        ksession = kstore.loadStatefulKnowledgeSession( id, kbase, null, env )
        processInstance = ksession.getProcessInstance( processInstanceId )
        assertNull( processInstance )
        ksession.dispose()
    }
}
